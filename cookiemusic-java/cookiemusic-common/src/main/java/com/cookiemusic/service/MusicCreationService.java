package com.cookiemusic.service;

import com.cookiemusic.entity.dto.MusicSettingDTO;
import com.cookiemusic.entity.po.MusicCreation;
import com.cookiemusic.entity.query.MusicCreationQuery;
import com.cookiemusic.entity.vo.PaginationResultVO;

import java.util.List;


/**
 * 业务接口
 */
public interface MusicCreationService {

    /**
     * 根据条件查询列表
     */
    List<MusicCreation> findListByParam(MusicCreationQuery param);

    /**
     * 根据条件查询列表
     */
    Integer findCountByParam(MusicCreationQuery param);

    /**
     * 分页查询
     */
    PaginationResultVO<MusicCreation> findListByPage(MusicCreationQuery param);

    /**
     * 新增
     */
    Integer add(MusicCreation bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<MusicCreation> listBean);

    /**
     * 批量新增/修改
     */
    Integer addOrUpdateBatch(List<MusicCreation> listBean);

    /**
     * 多条件更新
     */
    Integer updateByParam(MusicCreation bean, MusicCreationQuery param);

    /**
     * 多条件删除
     */
    Integer deleteByParam(MusicCreationQuery param);

    /**
     * 根据CreationId查询对象
     */
    MusicCreation getMusicCreationByCreationId(String creationId);


    /**
     * 根据CreationId修改
     */
    Integer updateMusicCreationByCreationId(MusicCreation bean, String creationId);


    /**
     * 根据CreationId删除
     */
    Integer deleteMusicCreationByCreationId(String creationId);

    /**
     * 音乐创作
     *
     * @param musicCreation
     */
    List<String> createMusic(MusicCreation musicCreation, MusicSettingDTO settingDTO);
}