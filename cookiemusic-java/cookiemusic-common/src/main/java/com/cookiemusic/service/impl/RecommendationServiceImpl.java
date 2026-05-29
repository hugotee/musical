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

    // FunkSVD 参数
    private static final int SVD_FACTORS = 5;
    private static final int SVD_EPOCHS = 100;
    private static final double SVD_LR = 0.005;
    private static final double SVD_LAMBDA = 0.15;

    // Apriori 参数
    private static final double MIN_CONFIDENCE = 0.3;

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
            case "svd":
                scores = scoreBySVD(candidates, userId, maxPlay, maxGood, now);
                break;
            case "apriori":
                scores = scoreByApriori(candidates, userId, maxPlay, maxGood, now);
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

    /** @return true 表示有足够的偏好数据，使用 TF-IDF 加权替代简单计数 */
    private boolean buildContentProfile(String userId, Map<String, MusicCreation> creationMap,
                                        Map<String, Double> genre, Map<String, Double> emotion,
                                        Map<String, Double> voice) {
        if (userId == null) return false;
        MusicInfoActionQuery q = new MusicInfoActionQuery();
        q.setUserId(userId);
        q.setActionType(MusicActionTypeEnum.GOOD.getType());
        List<MusicInfoAction> actions = musicInfoActionMapper.selectList(q);
        if (actions.size() < COLD_START_THRESHOLD) return false;

        // 1. 计算 IDF：统计每个标签出现在多少首作品中
        Map<String, Double> genreDF = new HashMap<>();
        Map<String, Double> emotionDF = new HashMap<>();
        Map<String, Double> voiceDF = new HashMap<>();
        int totalDocs = creationMap.size();

        for (MusicCreation mc : creationMap.values()) {
            if (mc.getSettings() == null) continue;
            try {
                JSONObject json = JSON.parseObject(mc.getSettings());
                String g = json.getString("musicGener");
                if (g != null && !g.isEmpty()) genreDF.merge(g, 1.0, Double::sum);
                String e = json.getString("musicEmotion");
                if (e != null && !e.isEmpty()) {
                    for (String t : e.split(",")) {
                        String s = t.trim();
                        if (!s.isEmpty()) emotionDF.merge(s, 1.0, Double::sum);
                    }
                }
                String s = json.getString("musicSex");
                if (s != null && !s.isEmpty()) voiceDF.merge(s, 1.0, Double::sum);
            } catch (Exception ignored) {}
        }
        // IDF = log(N / (1 + df))
        for (String k : genreDF.keySet())
            genreDF.put(k, Math.log((double) totalDocs / (1 + genreDF.get(k))));
        for (String k : emotionDF.keySet())
            emotionDF.put(k, Math.log((double) totalDocs / (1 + emotionDF.get(k))));
        for (String k : voiceDF.keySet())
            voiceDF.put(k, Math.log((double) totalDocs / (1 + voiceDF.get(k))));

        // 2. 构建用户 TF-IDF 画像：Σ(TF-IDF) 对每首用户点赞的歌
        for (MusicInfoAction a : actions) {
            MusicInfo mi = musicInfoMapper.selectByMusicId(a.getMusicId());
            if (mi != null && mi.getCreationId() != null) {
                MusicCreation mc = creationMap.get(mi.getCreationId());
                if (mc != null && mc.getSettings() != null) {
                    parseSettingsTFIDF(mc.getSettings(), genre, emotion, voice, genreDF, emotionDF, voiceDF);
                }
            }
        }
        return true;
    }

    /** TF-IDF 余弦相似度：用户画像(TF-IDF向量) vs 候选歌曲(one-hot向量) */
    private double computeContentSimilarity(String settings, Map<String, Double> genrePref,
                                            Map<String, Double> emotionPref, Map<String, Double> voicePref) {
        try {
            JSONObject json = JSON.parseObject(settings);
            double dotProduct = 0.0;
            int candidateTagCount = 0;

            String gener = json.getString("musicGener");
            if (gener != null && !gener.isEmpty()) {
                dotProduct += genrePref.getOrDefault(gener, 0.0);
                candidateTagCount++;
            }

            String musicEmotion = json.getString("musicEmotion");
            if (musicEmotion != null && !musicEmotion.isEmpty()) {
                for (String e : musicEmotion.split(",")) {
                    String t = e.trim();
                    if (t.isEmpty()) continue;
                    dotProduct += emotionPref.getOrDefault(t, 0.0);
                    candidateTagCount++;
                }
            }

            String sex = json.getString("musicSex");
            if (sex != null && !sex.isEmpty()) {
                dotProduct += voicePref.getOrDefault(sex, 0.0);
                candidateTagCount++;
            }

            if (dotProduct == 0.0 || candidateTagCount == 0) return 0.0;

            // ||user_profile||
            double userNorm = 0.0;
            for (double v : genrePref.values()) userNorm += v * v;
            for (double v : emotionPref.values()) userNorm += v * v;
            for (double v : voicePref.values()) userNorm += v * v;
            userNorm = Math.sqrt(userNorm);

            // ||candidate|| = sqrt(tag_count)
            double candidateNorm = Math.sqrt(candidateTagCount);

            return dotProduct / (userNorm * candidateNorm);
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

        // 第一轮：收集各算法的原始分数（分算法存储）
        Map<String, Double> rawContent = new LinkedHashMap<>();
        Map<String, Double> rawUserCf = new LinkedHashMap<>();
        Map<String, Double> rawItemCf = new LinkedHashMap<>();
        Map<String, Double> rawPop = new LinkedHashMap<>();
        Map<String, Double> rawRecency = new LinkedHashMap<>();

        for (MusicInfo c : candidates) {
            String mid = c.getMusicId();

            double contentScore = 0.0;
            if (contentReady && c.getCreationId() != null) {
                MusicCreation mc = creationMap.get(c.getCreationId());
                if (mc != null && mc.getSettings() != null) {
                    contentScore = computeContentSimilarity(mc.getSettings(), genrePref, emotionPref, voicePref);
                }
            }
            rawContent.put(mid, contentScore);

            double userCfScore = 0.0;
            if (cfReady) {
                for (Map.Entry<String, Double> u : topUsers) {
                    Set<String> likes = userLikeMap.get(u.getKey());
                    if (likes != null && likes.contains(mid)) {
                        userCfScore += u.getValue();
                    }
                }
            }
            rawUserCf.put(mid, userCfScore);

            double itemCfScore = itemCfCache.getOrDefault(mid, 0.0);
            rawItemCf.put(mid, itemCfScore);

            rawPop.put(mid, popularityScore(c, maxPlay, maxGood));
            rawRecency.put(mid, recencyScore(c, now));
        }

        // Min-Max 归一化：各算法分数映射到 [0, 1]，消除量纲差异
        Map<String, Double> normContent = normalizeScores(rawContent);
        Map<String, Double> normUserCf = normalizeScores(rawUserCf);
        Map<String, Double> normItemCf = normalizeScores(rawItemCf);
        Map<String, Double> normPop = normalizeScores(rawPop);
        Map<String, Double> normRecency = normalizeScores(rawRecency);

        // 第二轮：归一化后加权融合
        Map<String, Double> scores = new LinkedHashMap<>();
        for (MusicInfo c : candidates) {
            String mid = c.getMusicId();
            double total;
            if (!contentReady && !cfReady) {
                total = normPop.get(mid) * 0.7 + normRecency.get(mid) * 0.3;
            } else {
                total = normContent.get(mid) * 0.25
                      + normUserCf.get(mid) * 0.25
                      + normItemCf.get(mid) * 0.25
                      + normPop.get(mid) * 0.15
                      + normRecency.get(mid) * 0.10;
            }
            scores.put(mid, total);
        }
        return scores;
    }

    // ==================== FunkSVD 矩阵分解 ====================

    /** 构造用户-物品交互矩阵，用 SGD 分解为隐因子向量，预测评分 */
    private Map<String, Double> scoreBySVD(List<MusicInfo> candidates, String userId,
                                            int maxPlay, int maxGood, long now) {
        Map<String, Double> scores = new LinkedHashMap<>();
        if (userId == null) {
            return scoreByPopularity(candidates, maxPlay, maxGood, now);
        }

        Map<String, Set<String>> userLikeMap = new HashMap<>();
        Map<String, Set<String>> musicLikeMap = new HashMap<>();
        loadActionMaps(userLikeMap, musicLikeMap);

        // 构建索引
        List<String> users = new ArrayList<>(userLikeMap.keySet());
        Map<String, Integer> userIdx = new HashMap<>();
        for (int i = 0; i < users.size(); i++) userIdx.put(users.get(i), i);

        Map<String, Integer> itemIdx = new HashMap<>();
        for (int i = 0; i < candidates.size(); i++) itemIdx.put(candidates.get(i).getMusicId(), i);

        int nUsers = users.size();
        int nItems = candidates.size();
        if (nUsers == 0 || nItems == 0) return scoreByPopularity(candidates, maxPlay, maxGood, now);

        int k = SVD_FACTORS;

        // 随机初始化隐因子矩阵
        Random rand = new Random(42);
        double[][] P = new double[nUsers][k]; // user latent factors
        double[][] Q = new double[nItems][k]; // item latent factors
        for (int u = 0; u < nUsers; u++)
            for (int f = 0; f < k; f++) P[u][f] = rand.nextDouble() * 0.1;
        for (int i = 0; i < nItems; i++)
            for (int f = 0; f < k; f++) Q[i][f] = rand.nextDouble() * 0.1;

        // SGD 训练：目标最小化 (1 - p_u^T q_i)^2 + λ(||p_u||^2 + ||q_i||^2)
        for (int epoch = 0; epoch < SVD_EPOCHS; epoch++) {
            for (Map.Entry<String, Set<String>> entry : userLikeMap.entrySet()) {
                Integer ui = userIdx.get(entry.getKey());
                if (ui == null) continue;
                for (String mid : entry.getValue()) {
                    Integer mi = itemIdx.get(mid);
                    if (mi == null) continue;

                    double pred = 0.0;
                    for (int f = 0; f < k; f++) pred += P[ui][f] * Q[mi][f];

                    double err = 1.0 - pred; // 喜欢=1

                    for (int f = 0; f < k; f++) {
                        double pf = P[ui][f], qf = Q[mi][f];
                        P[ui][f] += SVD_LR * (err * qf - SVD_LAMBDA * pf);
                        Q[mi][f] += SVD_LR * (err * pf - SVD_LAMBDA * qf);
                    }
                }
            }
        }

        // 用学到的隐因子预测目标用户的评分
        Integer targetIdx = userIdx.get(userId);
        Set<String> targetLikes = userLikeMap.getOrDefault(userId, Collections.emptySet());
        boolean coldStart = targetIdx == null || targetLikes.size() < COLD_START_THRESHOLD;

        for (MusicInfo c : candidates) {
            double svdScore = 0.0;
            if (!coldStart) {
                Integer mi = itemIdx.get(c.getMusicId());
                if (mi != null) {
                    for (int f = 0; f < k; f++) svdScore += P[targetIdx][f] * Q[mi][f];
                    svdScore = Math.max(0, Math.min(1, svdScore));
                }
            }
            double popScore = popularityScore(c, maxPlay, maxGood);
            double recency = recencyScore(c, now);
            if (coldStart) {
                scores.put(c.getMusicId(), popScore * 0.7 + recency * 0.3);
            } else {
                scores.put(c.getMusicId(), svdScore * 0.7 + popScore * 0.2 + recency * 0.1);
            }
        }
        return scores;
    }

    // ==================== Apriori 关联规则 ====================

    /** 从所有用户的点赞记录中挖掘关联规则 "喜欢A → 喜欢B" (置信度 ≥ MIN_CONFIDENCE) */
    private Map<String, Double> scoreByApriori(List<MusicInfo> candidates, String userId,
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

        // 计算所有二元关联规则的置信度: confidence(A→B) = P(B|A) = |users(A∩B)| / |users(A)|
        Map<String, Map<String, Double>> rules = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : userLikeMap.entrySet()) {
            Set<String> likes = entry.getValue();
            for (String a : likes) {
                Map<String, Double> confMap = rules.computeIfAbsent(a, k -> new HashMap<>());
                for (String b : likes) {
                    if (a.equals(b)) continue;
                    confMap.merge(b, 1.0, Double::sum);
                }
            }
        }
        // 共现次数 → 置信度
        for (Map.Entry<String, Map<String, Double>> entry : rules.entrySet()) {
            Set<String> aUsers = musicLikeMap.get(entry.getKey());
            if (aUsers == null || aUsers.isEmpty()) continue;
            double supportA = aUsers.size();
            for (Map.Entry<String, Double> rule : entry.getValue().entrySet()) {
                rule.setValue(rule.getValue() / supportA);
            }
        }

        // 基于用户已点赞的歌曲触发规则，聚合得分
        for (MusicInfo c : candidates) {
            String mid = c.getMusicId();
            if (targetLikes.contains(mid)) continue;

            double ruleScore = 0.0;
            int triggeredRules = 0;
            for (String likedId : targetLikes) {
                Map<String, Double> confMap = rules.get(likedId);
                if (confMap == null) continue;
                Double conf = confMap.get(mid);
                if (conf != null && conf >= MIN_CONFIDENCE) {
                    ruleScore += conf;
                    triggeredRules++;
                }
            }
            double finalScore = triggeredRules > 0 ? ruleScore / triggeredRules : 0.0;
            double popScore = popularityScore(c, maxPlay, maxGood);
            double recency = recencyScore(c, now);
            scores.put(mid, finalScore * 0.6 + popScore * 0.3 + recency * 0.1);
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

    /** Min-Max 归一化到 [0, 1]：消除各算法分数量纲差异 */
    private Map<String, Double> normalizeScores(Map<String, Double> scores) {
        if (scores.isEmpty()) return scores;
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        for (double v : scores.values()) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        double range = max - min;
        Map<String, Double> result = new LinkedHashMap<>();
        if (range < 1e-9) {
            // 所有分数相同，均分
            for (String k : scores.keySet()) result.put(k, 0.5);
        } else {
            for (Map.Entry<String, Double> e : scores.entrySet()) {
                result.put(e.getKey(), (e.getValue() - min) / range);
            }
        }
        return result;
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

    /** TF-IDF 版的 parseSettings：累加 IDF 权重替代简单计数 */
    private void parseSettingsTFIDF(String settings, Map<String, Double> genre,
                                     Map<String, Double> emotion, Map<String, Double> voice,
                                     Map<String, Double> genreIDF, Map<String, Double> emotionIDF,
                                     Map<String, Double> voiceIDF) {
        try {
            JSONObject json = JSON.parseObject(settings);
            String gener = json.getString("musicGener");
            if (gener != null && !gener.isEmpty())
                genre.merge(gener, genreIDF.getOrDefault(gener, 0.0), Double::sum);
            String e = json.getString("musicEmotion");
            if (e != null && !e.isEmpty()) {
                for (String t : e.split(",")) {
                    String s = t.trim();
                    if (!s.isEmpty())
                        emotion.merge(s, emotionIDF.getOrDefault(s, 0.0), Double::sum);
                }
            }
            String sex = json.getString("musicSex");
            if (sex != null && !sex.isEmpty())
                voice.merge(sex, voiceIDF.getOrDefault(sex, 0.0), Double::sum);
        } catch (Exception ignored) {}
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
