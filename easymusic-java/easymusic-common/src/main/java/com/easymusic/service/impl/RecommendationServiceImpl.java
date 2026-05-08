package com.easymusic.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.easymusic.entity.enums.MusicActionTypeEnum;
import com.easymusic.entity.enums.MusicStatusEnum;
import com.easymusic.entity.po.MusicCreation;
import com.easymusic.entity.po.MusicInfo;
import com.easymusic.entity.po.MusicInfoAction;
import com.easymusic.entity.query.MusicCreationQuery;
import com.easymusic.entity.query.MusicInfoActionQuery;
import com.easymusic.entity.query.MusicInfoQuery;
import com.easymusic.mappers.MusicCreationMapper;
import com.easymusic.mappers.MusicInfoActionMapper;
import com.easymusic.mappers.MusicInfoMapper;
import com.easymusic.service.RecommendationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service("recommendationService")
public class RecommendationServiceImpl implements RecommendationService {

    private static final double CONTENT_WEIGHT = 0.6;
    private static final double POPULARITY_WEIGHT = 0.3;
    private static final double RECENCY_WEIGHT = 0.1;
    private static final int COLD_START_THRESHOLD = 3;

    @Resource
    private MusicInfoMapper<MusicInfo, MusicInfoQuery> musicInfoMapper;

    @Resource
    private MusicInfoActionMapper<MusicInfoAction, MusicInfoActionQuery> musicInfoActionMapper;

    @Resource
    private MusicCreationMapper<MusicCreation, MusicCreationQuery> musicCreationMapper;

    @Override
    public List<MusicInfo> recommend(String userId, int limit) {
        // 1. 加载所有已完成的音乐和创作记录
        List<MusicInfo> allMusic = loadAllCompletedMusic();

        List<MusicCreation> allCreations = musicCreationMapper.selectList(new MusicCreationQuery());
        Map<String, MusicCreation> creationMap = new HashMap<>();
        for (MusicCreation mc : allCreations) {
            creationMap.put(mc.getCreationId(), mc);
        }

        // 2. 筛选候选（排除用户自己的音乐）
        List<MusicInfo> candidates = new ArrayList<>();
        for (MusicInfo mi : allMusic) {
            if (userId == null || !userId.equals(mi.getUserId())) {
                candidates.add(mi);
            }
        }

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 构建用户偏好画像
        Map<String, Double> genrePref = new HashMap<>();
        Map<String, Double> emotionPref = new HashMap<>();
        Map<String, Double> voicePref = new HashMap<>();
        boolean coldStart = true;

        if (userId != null) {
            MusicInfoActionQuery actionQuery = new MusicInfoActionQuery();
            actionQuery.setUserId(userId);
            actionQuery.setActionType(MusicActionTypeEnum.GOOD.getType());
            List<MusicInfoAction> actions = musicInfoActionMapper.selectList(actionQuery);

            if (actions.size() >= COLD_START_THRESHOLD) {
                coldStart = false;
                for (MusicInfoAction action : actions) {
                    MusicInfo likedMusic = musicInfoMapper.selectByMusicId(action.getMusicId());
                    if (likedMusic != null && likedMusic.getCreationId() != null) {
                        MusicCreation creation = creationMap.get(likedMusic.getCreationId());
                        if (creation != null && creation.getSettings() != null) {
                            parseSettings(creation.getSettings(), genrePref, emotionPref, voicePref);
                        }
                    }
                }
            }
        }

        // 4. 计算全局最大值用于归一化
        int maxPlayCount = 1;
        int maxGoodCount = 1;
        for (MusicInfo mi : candidates) {
            if (mi.getPlayCount() > maxPlayCount) maxPlayCount = mi.getPlayCount();
            if (mi.getGoodCount() > maxGoodCount) maxGoodCount = mi.getGoodCount();
        }

        long now = System.currentTimeMillis();

        // 5. 为每个候选打分
        List<Map.Entry<String, Double>> scoredIds = new ArrayList<>();
        for (MusicInfo candidate : candidates) {
            double contentScore = 0.0;

            if (!coldStart && candidate.getCreationId() != null) {
                MusicCreation creation = creationMap.get(candidate.getCreationId());
                if (creation != null && creation.getSettings() != null) {
                    contentScore = computeContentSimilarity(creation.getSettings(), genrePref, emotionPref, voicePref);
                }
            }

            // 热度分：播放量 + 点赞数 归一化加权
            double popScore = (double) candidate.getPlayCount() / maxPlayCount * 0.5
                            + (double) candidate.getGoodCount() / maxGoodCount * 2.0;
            popScore = Math.min(popScore, 1.0);

            // 时间衰减：30天半衰期
            double recencyScore = 0.0;
            if (candidate.getCreateTime() != null) {
                long days = (now - candidate.getCreateTime().getTime()) / (24 * 3600 * 1000L);
                recencyScore = Math.exp(-days / 30.0);
            }

            double totalScore;
            if (coldStart) {
                totalScore = popScore * 0.7 + recencyScore * 0.3;
            } else {
                totalScore = contentScore * CONTENT_WEIGHT + popScore * POPULARITY_WEIGHT + recencyScore * RECENCY_WEIGHT;
            }

            scoredIds.add(new AbstractMap.SimpleEntry<>(candidate.getMusicId(), totalScore));
        }

        // 6. 按分数排序取 Top N
        scoredIds.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<String> topIds = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, scoredIds.size()); i++) {
            topIds.add(scoredIds.get(i).getKey());
        }

        // 7. 用 musicIdList 查询完整数据（含用户信息和点赞状态）
        MusicInfoQuery resultQuery = new MusicInfoQuery();
        resultQuery.setQueryUser(true);
        resultQuery.setCurrentUserId(userId);
        resultQuery.setMusicIdList(topIds);
        List<MusicInfo> result = musicInfoMapper.selectList(resultQuery);

        // 按分数顺序重排
        Map<String, MusicInfo> resultMap = new HashMap<>();
        for (MusicInfo mi : result) {
            resultMap.put(mi.getMusicId(), mi);
        }
        List<MusicInfo> ordered = new ArrayList<>();
        for (String id : topIds) {
            MusicInfo mi = resultMap.get(id);
            if (mi != null) {
                ordered.add(mi);
            }
        }

        return ordered;
    }

    private List<MusicInfo> loadAllCompletedMusic() {
        MusicInfoQuery query = new MusicInfoQuery();
        query.setMusicStatus(MusicStatusEnum.CREATED.getStatus());
        return musicInfoMapper.selectList(query);
    }

    private void parseSettings(String settings, Map<String, Double> genre, Map<String, Double> emotion, Map<String, Double> voice) {
        try {
            JSONObject json = JSON.parseObject(settings);
            String gener = json.getString("musicGener");
            if (gener != null && !gener.isEmpty()) {
                genre.merge(gener, 1.0, Double::sum);
            }
            String musicEmotion = json.getString("musicEmotion");
            if (musicEmotion != null && !musicEmotion.isEmpty()) {
                for (String e : musicEmotion.split(",")) {
                    String trimmed = e.trim();
                    if (!trimmed.isEmpty()) {
                        emotion.merge(trimmed, 1.0, Double::sum);
                    }
                }
            }
            String sex = json.getString("musicSex");
            if (sex != null && !sex.isEmpty()) {
                voice.merge(sex, 1.0, Double::sum);
            }
        } catch (Exception ignored) {
        }
    }

    private double computeContentSimilarity(String settings, Map<String, Double> genrePref,
                                            Map<String, Double> emotionPref, Map<String, Double> voicePref) {
        try {
            JSONObject json = JSON.parseObject(settings);
            double similarity = 0.0;
            int count = 0;

            String gener = json.getString("musicGener");
            if (gener != null && genrePref.containsKey(gener)) {
                double maxG = maxValue(genrePref);
                similarity += maxG > 0 ? genrePref.get(gener) / maxG : 0;
                count++;
            }

            String musicEmotion = json.getString("musicEmotion");
            if (musicEmotion != null) {
                double emotionScore = 0;
                double maxE = maxValue(emotionPref);
                int emotionCount = 0;
                for (String e : musicEmotion.split(",")) {
                    String trimmed = e.trim();
                    if (emotionPref.containsKey(trimmed)) {
                        emotionScore += maxE > 0 ? emotionPref.get(trimmed) / maxE : 0;
                        emotionCount++;
                    }
                }
                if (emotionCount > 0) {
                    similarity += emotionScore / emotionCount;
                    count++;
                }
            }

            String sex = json.getString("musicSex");
            if (sex != null && voicePref.containsKey(sex)) {
                double maxV = maxValue(voicePref);
                similarity += maxV > 0 ? voicePref.get(sex) / maxV : 0;
                count++;
            }

            return count > 0 ? similarity / count : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double maxValue(Map<String, Double> map) {
        double max = 0;
        for (double v : map.values()) {
            if (v > max) max = v;
        }
        return max;
    }
}
