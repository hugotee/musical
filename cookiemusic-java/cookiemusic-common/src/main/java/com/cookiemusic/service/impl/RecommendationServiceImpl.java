package com.cookiemusic.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cookiemusic.entity.enums.MusicActionTypeEnum;
import com.cookiemusic.entity.enums.MusicStatusEnum;
import com.cookiemusic.entity.po.MusicCreation;
import com.cookiemusic.entity.po.MusicInfo;
import com.cookiemusic.entity.po.MusicInfoAction;
import com.cookiemusic.entity.query.MusicCreationQuery;
import com.cookiemusic.entity.query.MusicInfoActionQuery;
import com.cookiemusic.entity.query.MusicInfoQuery;
import com.cookiemusic.mappers.MusicCreationMapper;
import com.cookiemusic.mappers.MusicInfoActionMapper;
import com.cookiemusic.mappers.MusicInfoMapper;
import com.cookiemusic.service.RecommendationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service("recommendationService")
public class RecommendationServiceImpl implements RecommendationService {

    private static final int COLD_START_THRESHOLD = 3;
    private static final int CF_TOP_K = 10;

    @Resource
    private MusicInfoMapper<MusicInfo, MusicInfoQuery> musicInfoMapper;

    @Resource
    private MusicInfoActionMapper<MusicInfoAction, MusicInfoActionQuery> musicInfoActionMapper;

    @Resource
    private MusicCreationMapper<MusicCreation, MusicCreationQuery> musicCreationMapper;

    // ==================== 公开接口 ====================

    @Override
    public List<MusicInfo> recommend(String userId, int limit) {
        return recommend(userId, limit, "hybrid");
    }

    @Override
    public List<MusicInfo> recommend(String userId, int limit, String type) {
        List<MusicInfo> candidates = loadCandidates(userId);
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, MusicCreation> creationMap = loadCreationMap();
        long now = System.currentTimeMillis();

        // 归一化基准
        int maxPlay = 1, maxGood = 1;
        for (MusicInfo mi : candidates) {
            if (mi.getPlayCount() > maxPlay) maxPlay = mi.getPlayCount();
            if (mi.getGoodCount() > maxGood) maxGood = mi.getGoodCount();
        }

        // 按类型计算分数
        Map<String, Double> scores;
        if (type == null) {
            type = "hybrid";
        }
        switch (type) {
            case "content":
                scores = scoreByContent(candidates, userId, creationMap, maxPlay, maxGood, now);
                break;
            case "userCf":
                scores = scoreByUserCF(candidates, userId, maxPlay, maxGood, now);
                break;
            case "itemCf":
                scores = scoreByItemCF(candidates, userId, maxPlay, maxGood, now);
                break;
            case "popularity":
                scores = scoreByPopularity(candidates, maxPlay, maxGood, now);
                break;
            case "hybrid":
            default:
                scores = scoreHybrid(candidates, userId, creationMap, maxPlay, maxGood, now);
                break;
        }

        return buildResult(scores, userId, limit);
    }

    // ==================== 数据加载 ====================

    private List<MusicInfo> loadCandidates(String userId) {
        MusicInfoQuery query = new MusicInfoQuery();
        query.setMusicStatus(MusicStatusEnum.CREATED.getStatus());
        List<MusicInfo> all = musicInfoMapper.selectList(query);
        List<MusicInfo> candidates = new ArrayList<>();
        for (MusicInfo mi : all) {
            if (userId == null || !userId.equals(mi.getUserId())) {
                candidates.add(mi);
            }
        }
        return candidates;
    }

    private Map<String, MusicCreation> loadCreationMap() {
        List<MusicCreation> all = musicCreationMapper.selectList(new MusicCreationQuery());
        Map<String, MusicCreation> map = new HashMap<>();
        for (MusicCreation mc : all) {
            map.put(mc.getCreationId(), mc);
        }
        return map;
    }

    /** 加载所有点赞记录，构建 user→likedMusicIds 和 music→likingUserIds 两个映射 */
    private void loadActionMaps(Map<String, Set<String>> userLikeMap,
                                Map<String, Set<String>> musicLikeMap) {
        List<MusicInfoAction> allActions = musicInfoActionMapper.selectList(new MusicInfoActionQuery());
        for (MusicInfoAction a : allActions) {
            userLikeMap.computeIfAbsent(a.getUserId(), k -> new HashSet<>()).add(a.getMusicId());
            musicLikeMap.computeIfAbsent(a.getMusicId(), k -> new HashSet<>()).add(a.getUserId());
        }
    }

    // ==================== 基于内容的推荐 ====================

    private Map<String, Double> scoreByContent(List<MusicInfo> candidates, String userId,
                                                Map<String, MusicCreation> creationMap,
                                                int maxPlay, int maxGood, long now) {
        Map<String, Double> genrePref = new HashMap<>();
        Map<String, Double> emotionPref = new HashMap<>();
        Map<String, Double> voicePref = new HashMap<>();
        boolean coldStart = !buildContentProfile(userId, creationMap, genrePref, emotionPref, voicePref);

        Map<String, Double> scores = new LinkedHashMap<>();
        for (MusicInfo c : candidates) {
            double contentScore = 0.0;
            if (!coldStart && c.getCreationId() != null) {
                MusicCreation mc = creationMap.get(c.getCreationId());
                if (mc != null && mc.getSettings() != null) {
                    contentScore = computeContentSimilarity(mc.getSettings(), genrePref, emotionPref, voicePref);
                }
            }
            double popScore = popularityScore(c, maxPlay, maxGood);
            double recencyScore = recencyScore(c, now);

            if (coldStart) {
                scores.put(c.getMusicId(), popScore * 0.7 + recencyScore * 0.3);
            } else {
                scores.put(c.getMusicId(), contentScore * 0.6 + popScore * 0.3 + recencyScore * 0.1);
            }
        }
        return scores;
    }

    /** @return true 表示有足够的偏好数据 */
    private boolean buildContentProfile(String userId, Map<String, MusicCreation> creationMap,
                                        Map<String, Double> genre, Map<String, Double> emotion,
                                        Map<String, Double> voice) {
        if (userId == null) return false;
        MusicInfoActionQuery q = new MusicInfoActionQuery();
        q.setUserId(userId);
        q.setActionType(MusicActionTypeEnum.GOOD.getType());
        List<MusicInfoAction> actions = musicInfoActionMapper.selectList(q);
        if (actions.size() < COLD_START_THRESHOLD) return false;

        for (MusicInfoAction a : actions) {
            MusicInfo mi = musicInfoMapper.selectByMusicId(a.getMusicId());
            if (mi != null && mi.getCreationId() != null) {
                MusicCreation mc = creationMap.get(mi.getCreationId());
                if (mc != null && mc.getSettings() != null) {
                    parseSettings(mc.getSettings(), genre, emotion, voice);
                }
            }
        }
        return true;
    }

    private double computeContentSimilarity(String settings, Map<String, Double> genrePref,
                                            Map<String, Double> emotionPref, Map<String, Double> voicePref) {
        try {
            JSONObject json = JSON.parseObject(settings);
            double sim = 0.0;
            int count = 0;

            String gener = json.getString("musicGener");
            if (gener != null && genrePref.containsKey(gener)) {
                double maxG = maxValue(genrePref);
                sim += maxG > 0 ? genrePref.get(gener) / maxG : 0;
                count++;
            }

            String musicEmotion = json.getString("musicEmotion");
            if (musicEmotion != null) {
                double emotionScore = 0;
                double maxE = maxValue(emotionPref);
                int emotionCount = 0;
                for (String e : musicEmotion.split(",")) {
                    String t = e.trim();
                    if (emotionPref.containsKey(t)) {
                        emotionScore += maxE > 0 ? emotionPref.get(t) / maxE : 0;
                        emotionCount++;
                    }
                }
                if (emotionCount > 0) { sim += emotionScore / emotionCount; count++; }
            }

            String sex = json.getString("musicSex");
            if (sex != null && voicePref.containsKey(sex)) {
                double maxV = maxValue(voicePref);
                sim += maxV > 0 ? voicePref.get(sex) / maxV : 0;
                count++;
            }
            return count > 0 ? sim / count : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    // ==================== User-Based 协同过滤 ====================

    private Map<String, Double> scoreByUserCF(List<MusicInfo> candidates, String userId,
                                               int maxPlay, int maxGood, long now) {
        Map<String, Double> scores = new LinkedHashMap<>();
        if (userId == null) {
            return scoreByPopularity(candidates, maxPlay, maxGood, now);
        }

        Map<String, Set<String>> userLikeMap = new HashMap<>();
        Map<String, Set<String>> musicLikeMap = new HashMap<>();
        loadActionMaps(userLikeMap, musicLikeMap);

        Set<String> targetLikes = userLikeMap.getOrDefault(userId, Collections.emptySet());
        if (targetLikes.size() < COLD_START_THRESHOLD) {
            return scoreByPopularity(candidates, maxPlay, maxGood, now);
        }

        // 计算目标用户与其他用户的余弦相似度
        Map<String, Double> userSim = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : userLikeMap.entrySet()) {
            String other = entry.getKey();
            if (other.equals(userId)) continue;
            double sim = cosineSimilarity(targetLikes, entry.getValue());
            if (sim > 0) userSim.put(other, sim);
        }

        // Top K 相似用户
        List<Map.Entry<String, Double>> topUsers = userSim.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(CF_TOP_K).collect(Collectors.toList());

        // 聚合候选歌曲分数 = Σ 相似用户对该歌的相似度
        for (MusicInfo c : candidates) {
            double cfScore = 0.0;
            for (Map.Entry<String, Double> u : topUsers) {
                Set<String> likes = userLikeMap.get(u.getKey());
                if (likes != null && likes.contains(c.getMusicId())) {
                    cfScore += u.getValue();
                }
            }
            double popScore = popularityScore(c, maxPlay, maxGood);
            double recency = recencyScore(c, now);
            scores.put(c.getMusicId(), cfScore * 0.7 + popScore * 0.2 + recency * 0.1);
        }
        return scores;
    }

    // ==================== Item-Based 协同过滤 ====================

    private Map<String, Double> scoreByItemCF(List<MusicInfo> candidates, String userId,
                                               int maxPlay, int maxGood, long now) {
        Map<String, Double> scores = new LinkedHashMap<>();
        if (userId == null) {
            return scoreByPopularity(candidates, maxPlay, maxGood, now);
        }

        Map<String, Set<String>> userLikeMap = new HashMap<>();
        Map<String, Set<String>> musicLikeMap = new HashMap<>();
        loadActionMaps(userLikeMap, musicLikeMap);

        Set<String> targetLikes = userLikeMap.getOrDefault(userId, Collections.emptySet());
        if (targetLikes.size() < COLD_START_THRESHOLD) {
            return scoreByPopularity(candidates, maxPlay, maxGood, now);
        }

        // 预计算候选歌曲与用户已点赞歌曲的相似度
        Map<String, Double> itemCfCache = new HashMap<>();
        for (MusicInfo c : candidates) {
            if (targetLikes.contains(c.getMusicId())) continue;
            Set<String> cUsers = musicLikeMap.get(c.getMusicId());
            if (cUsers == null || cUsers.isEmpty()) continue;

            double maxSim = 0.0;
            for (String likedId : targetLikes) {
                Set<String> likedUsers = musicLikeMap.get(likedId);
                if (likedUsers == null) continue;
                double sim = cosineSimilarity(cUsers, likedUsers);
                if (sim > maxSim) maxSim = sim;
            }
            if (maxSim > 0) itemCfCache.put(c.getMusicId(), maxSim);
        }

        for (MusicInfo c : candidates) {
            double cfScore = itemCfCache.getOrDefault(c.getMusicId(), 0.0);
            double popScore = popularityScore(c, maxPlay, maxGood);
            double recency = recencyScore(c, now);
            scores.put(c.getMusicId(), cfScore * 0.7 + popScore * 0.2 + recency * 0.1);
        }
        return scores;
    }

    // ==================== 热度排序 ====================

    private Map<String, Double> scoreByPopularity(List<MusicInfo> candidates,
                                                   int maxPlay, int maxGood, long now) {
        Map<String, Double> scores = new LinkedHashMap<>();
        for (MusicInfo c : candidates) {
            double popScore = popularityScore(c, maxPlay, maxGood);
            double recency = recencyScore(c, now);
            scores.put(c.getMusicId(), popScore * 0.7 + recency * 0.3);
        }
        return scores;
    }

    // ==================== 混合推荐 ====================

    private Map<String, Double> scoreHybrid(List<MusicInfo> candidates, String userId,
                                             Map<String, MusicCreation> creationMap,
                                             int maxPlay, int maxGood, long now) {
        if (userId == null) {
            return scoreByPopularity(candidates, maxPlay, maxGood, now);
        }

        // 内容画像
        Map<String, Double> genrePref = new HashMap<>();
        Map<String, Double> emotionPref = new HashMap<>();
        Map<String, Double> voicePref = new HashMap<>();
        boolean contentReady = buildContentProfile(userId, creationMap, genrePref, emotionPref, voicePref);

        // 协同过滤数据
        Map<String, Set<String>> userLikeMap = new HashMap<>();
        Map<String, Set<String>> musicLikeMap = new HashMap<>();
        loadActionMaps(userLikeMap, musicLikeMap);
        Set<String> targetLikes = userLikeMap.getOrDefault(userId, Collections.emptySet());
        boolean cfReady = targetLikes.size() >= COLD_START_THRESHOLD;

        // User-CF 相似用户
        Map<String, Double> userSim = new HashMap<>();
        if (cfReady) {
            for (Map.Entry<String, Set<String>> entry : userLikeMap.entrySet()) {
                if (entry.getKey().equals(userId)) continue;
                double sim = cosineSimilarity(targetLikes, entry.getValue());
                if (sim > 0) userSim.put(entry.getKey(), sim);
            }
        }
        List<Map.Entry<String, Double>> topUsers = userSim.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(CF_TOP_K).collect(Collectors.toList());

        // Item-CF 预计算
        Map<String, Double> itemCfCache = new HashMap<>();
        if (cfReady) {
            for (MusicInfo c : candidates) {
                if (targetLikes.contains(c.getMusicId())) continue;
                Set<String> cUsers = musicLikeMap.get(c.getMusicId());
                if (cUsers == null) continue;
                double maxSim = 0.0;
                for (String likedId : targetLikes) {
                    Set<String> likedUsers = musicLikeMap.get(likedId);
                    if (likedUsers == null) continue;
                    double sim = cosineSimilarity(cUsers, likedUsers);
                    if (sim > maxSim) maxSim = sim;
                }
                if (maxSim > 0) itemCfCache.put(c.getMusicId(), maxSim);
            }
        }

        Map<String, Double> scores = new LinkedHashMap<>();
        for (MusicInfo c : candidates) {
            // 内容相似度
            double contentScore = 0.0;
            if (contentReady && c.getCreationId() != null) {
                MusicCreation mc = creationMap.get(c.getCreationId());
                if (mc != null && mc.getSettings() != null) {
                    contentScore = computeContentSimilarity(mc.getSettings(), genrePref, emotionPref, voicePref);
                }
            }

            // User-CF 分数
            double userCfScore = 0.0;
            if (cfReady) {
                for (Map.Entry<String, Double> u : topUsers) {
                    Set<String> likes = userLikeMap.get(u.getKey());
                    if (likes != null && likes.contains(c.getMusicId())) {
                        userCfScore += u.getValue();
                    }
                }
            }

            // Item-CF 分数
            double itemCfScore = itemCfCache.getOrDefault(c.getMusicId(), 0.0);

            double popScore = popularityScore(c, maxPlay, maxGood);
            double recency = recencyScore(c, now);

            double total;
            if (!contentReady && !cfReady) {
                // 冷启动：纯热度
                total = popScore * 0.7 + recency * 0.3;
            } else {
                // 混合加权
                total = contentScore * 0.25
                      + userCfScore * 0.25
                      + itemCfScore * 0.25
                      + popScore * 0.15
                      + recency * 0.10;
            }

            scores.put(c.getMusicId(), total);
        }
        return scores;
    }

    // ==================== 通用工具方法 ====================

    /** 余弦相似度：|A ∩ B| / sqrt(|A| × |B|) */
    private double cosineSimilarity(Set<String> set1, Set<String> set2) {
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;
        Set<String> smaller = set1.size() < set2.size() ? set1 : set2;
        Set<String> larger = set1.size() < set2.size() ? set2 : set1;
        int intersection = 0;
        for (String s : smaller) {
            if (larger.contains(s)) intersection++;
        }
        return intersection / Math.sqrt((double) set1.size() * set2.size());
    }

    private double popularityScore(MusicInfo mi, int maxPlay, int maxGood) {
        double score = (double) mi.getPlayCount() / maxPlay * 0.5
                     + (double) mi.getGoodCount() / maxGood * 2.0;
        return Math.min(score, 1.0);
    }

    private double recencyScore(MusicInfo mi, long now) {
        if (mi.getCreateTime() == null) return 0.0;
        long days = (now - mi.getCreateTime().getTime()) / (24 * 3600 * 1000L);
        return Math.exp(-days / 30.0);
    }

    private void parseSettings(String settings, Map<String, Double> genre,
                               Map<String, Double> emotion, Map<String, Double> voice) {
        try {
            JSONObject json = JSON.parseObject(settings);
            String gener = json.getString("musicGener");
            if (gener != null && !gener.isEmpty()) genre.merge(gener, 1.0, Double::sum);
            String e = json.getString("musicEmotion");
            if (e != null && !e.isEmpty()) {
                for (String t : e.split(",")) {
                    String trimmed = t.trim();
                    if (!trimmed.isEmpty()) emotion.merge(trimmed, 1.0, Double::sum);
                }
            }
            String sex = json.getString("musicSex");
            if (sex != null && !sex.isEmpty()) voice.merge(sex, 1.0, Double::sum);
        } catch (Exception ignored) {}
    }

    private double maxValue(Map<String, Double> map) {
        double max = 0;
        for (double v : map.values()) { if (v > max) max = v; }
        return max;
    }

    /** 按分数排序取 Top N，查询完整数据并保持顺序 */
    private List<MusicInfo> buildResult(Map<String, Double> scores, String userId, int limit) {
        List<String> topIds = scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topIds.isEmpty()) return Collections.emptyList();

        MusicInfoQuery q = new MusicInfoQuery();
        q.setQueryUser(true);
        q.setCurrentUserId(userId);
        q.setMusicIdList(topIds);
        List<MusicInfo> list = musicInfoMapper.selectList(q);

        Map<String, MusicInfo> map = new HashMap<>();
        for (MusicInfo mi : list) map.put(mi.getMusicId(), mi);

        List<MusicInfo> ordered = new ArrayList<>();
        for (String id : topIds) {
            MusicInfo mi = map.get(id);
            if (mi != null) ordered.add(mi);
        }
        return ordered;
    }
}
