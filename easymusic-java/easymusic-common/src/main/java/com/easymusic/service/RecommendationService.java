package com.easymusic.service;

import com.easymusic.entity.po.MusicInfo;

import java.util.List;

/**
 * 音乐推荐服务接口
 */
public interface RecommendationService {

    /**
     * 根据用户偏好推荐音乐
     * @param userId 当前用户ID，为null时使用热度排序
     * @param limit  返回数量
     * @return 推荐音乐列表（含用户信息和点赞状态）
     */
    List<MusicInfo> recommend(String userId, int limit);
}
