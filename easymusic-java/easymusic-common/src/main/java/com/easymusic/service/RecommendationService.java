package com.easymusic.service;

import com.easymusic.entity.po.MusicInfo;

import java.util.List;

/**
 * 音乐推荐服务接口
 */
public interface RecommendationService {

    /**
     * 混合推荐（默认，融合所有算法）
     */
    List<MusicInfo> recommend(String userId, int limit);

    /**
     * 指定算法类型推荐
     * @param userId 当前用户ID，为null时使用热度排序
     * @param limit  返回数量
     * @param type   算法类型：content / userCf / itemCf / popularity / hybrid
     */
    List<MusicInfo> recommend(String userId, int limit, String type);
}
