package com.easymusic.service.impl;

import com.easymusic.entity.config.AppConfig;
import com.easymusic.entity.constants.Constants;
import com.easymusic.entity.dto.MusicSettingDTO;
import com.easymusic.entity.enums.*;
import com.easymusic.entity.po.MusicCreation;
import com.easymusic.entity.po.MusicInfo;
import com.easymusic.entity.po.SysDict;
import com.easymusic.entity.query.MusicCreationQuery;
import com.easymusic.entity.query.MusicInfoQuery;
import com.easymusic.entity.query.SimplePage;
import com.easymusic.entity.vo.PaginationResultVO;
import com.easymusic.exception.BusinessException;
import com.easymusic.mappers.MusicCreationMapper;
import com.easymusic.mappers.MusicInfoMapper;
import com.easymusic.redis.RedisComponent;
import com.easymusic.service.MusicCreationService;
import com.easymusic.utils.JsonUtils;
import com.easymusic.utils.StringTools;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 业务接口实现
 */
@Service("musicCreationService")
@Slf4j
public class MusicCreationServiceImpl implements MusicCreationService {

    @Resource
    private MusicCreationMapper<MusicCreation, MusicCreationQuery> musicCreationMapper;

    @Resource
    private MusicInfoMapper<MusicInfo, MusicInfoQuery> musicInfoMapper;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private AppConfig appConfig;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<MusicCreation> findListByParam(MusicCreationQuery param) {
        return this.musicCreationMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(MusicCreationQuery param) {
        return this.musicCreationMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<MusicCreation> findListByPage(MusicCreationQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<MusicCreation> list = this.findListByParam(param);
        PaginationResultVO<MusicCreation> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(MusicCreation bean) {
        return this.musicCreationMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<MusicCreation> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.musicCreationMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<MusicCreation> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.musicCreationMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(MusicCreation bean, MusicCreationQuery param) {
        StringTools.checkParam(param);
        return this.musicCreationMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(MusicCreationQuery param) {
        StringTools.checkParam(param);
        return this.musicCreationMapper.deleteByParam(param);
    }

    /**
     * 根据CreationId获取对象
     */
    @Override
    public MusicCreation getMusicCreationByCreationId(String creationId) {
        return this.musicCreationMapper.selectByCreationId(creationId);
    }

    /**
     * 根据CreationId修改
     */
    @Override
    public Integer updateMusicCreationByCreationId(MusicCreation bean, String creationId) {
        return this.musicCreationMapper.updateByCreationId(bean, creationId);
    }

    /**
     * 根据CreationId删除
     */
    @Override
    public Integer deleteMusicCreationByCreationId(String creationId) {
        return this.musicCreationMapper.deleteByCreationId(creationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> createMusic(MusicCreation musicCreation, MusicSettingDTO musicSettingDTO) {
        MusicTypeEnum musicTypeEnum = MusicTypeEnum.getByType(musicCreation.getMusicType());
        if (null == musicTypeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        getModelInfo(musicTypeEnum, musicCreation.getModel());

        List<SysDict> sysDictSubList = redisComponent.getDictSubList(musicTypeEnum.getDictCode());
        if (sysDictSubList == null || sysDictSubList.isEmpty()) {
            throw new BusinessException("系统配置错误，请联系管理员");
        }
        Optional<SysDict> dictInfo = sysDictSubList.stream().filter(value -> value.getDictCode().equals(musicCreation.getModel())).findFirst();
        if (dictInfo.isEmpty()) {
            throw new BusinessException("系统配置错误，请联系管理员");
        }
        String creationId = StringTools.getRandomString(Constants.LENGTH_15);

        Date curDate = new Date();

        musicCreation.setCreationId(creationId);
        musicCreation.setSettings(JsonUtils.convertObj2Json(musicSettingDTO));
        musicCreation.setCreateTime(curDate);
        musicCreationMapper.insert(musicCreation);

        String prompt = musicCreation.getPrompt();
        if (MusicModeTypeEnum.ADVANCED.getModeType().equals(musicCreation.getModeType())) {
            try {
                for (MusicSettingEnum settingEnum : MusicSettingEnum.values()) {
                    PropertyDescriptor pd = new PropertyDescriptor(settingEnum.getKeyCode(), MusicSettingDTO.class);
                    Method method = pd.getReadMethod();
                    Object obj = method.invoke(musicSettingDTO);
                    if (obj == null) {
                        continue;
                    }
                    prompt = prompt + " " + settingEnum.getTypeDesc() + ":" + obj;
                }
            } catch (Exception e) {
                log.error("获取音乐设置信息失败", e);
            }
        }
        // 这里先用本地固定 mp3 作为创作结果，保证创作闭环可直接演示。
        String musicId = StringTools.getRandomNumber(Constants.LENGTH_12);
        String audioPath = copyLocalDemoMusic(musicId);

        MusicInfo musicInfo = new MusicInfo();
        musicInfo.setMusicId(musicId);
        musicInfo.setUserId(musicCreation.getUserId());
        musicInfo.setCreationId(musicCreation.getCreationId());
        musicInfo.setGoodCount(0);
        musicInfo.setPlayCount(0);
        musicInfo.setCreateTime(curDate);
        musicInfo.setCommendType(CommendTypeEnum.NOT_COMMEND.getType());
        musicInfo.setMusicStatus(MusicStatusEnum.CREATED.getStatus());
        musicInfo.setTaskId(creationId);
        musicInfo.setMusicType(musicCreation.getMusicType());
        musicInfo.setMusicTitle(resolveDemoTitle(prompt));
        musicInfo.setAudioPath(audioPath);
        musicInfo.setLyrics("[]");
        musicInfoMapper.insert(musicInfo);

        List<String> musicIdList = new ArrayList<>();
        musicIdList.add(musicId);
        return musicIdList;
    }

    record ModelInfo(String model, String apiCode) {

    }

    private ModelInfo getModelInfo(MusicTypeEnum musicTypeEnum, String modelId) {
        if (MusicTypeEnum.MUSIC == musicTypeEnum) {
            ModelType4MusicEnum musicEnum = ModelType4MusicEnum.getById(modelId);
            if (null == musicEnum) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
            return new ModelInfo(musicEnum.getModelCode(), musicEnum.getApiCode());

        } else if (MusicTypeEnum.PURE == musicTypeEnum) {
            ModelType4PureMusicEnum musicEnum = ModelType4PureMusicEnum.getById(modelId);
            if (null == musicEnum) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
            return new ModelInfo(musicEnum.getModelCode(), musicEnum.getApiCode());
        } else {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

    private String copyLocalDemoMusic(String musicId) {
        try {
            String sourcePath = "/Users/hugo/Downloads/逃跑计划 - 夜空中最亮的星.mp3";
            File sourceFile = new File(sourcePath);
            if (!sourceFile.exists()) {
                throw new BusinessException("本地示例音乐文件不存在");
            }
            String folderName = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            String targetRelativePath = folderName + "/" + musicId + Constants.AUDIO_SUFFIX;
            Path targetPath = Path.of(appConfig.getProjectFolder(), Constants.FILE_FOLDER_FILE, targetRelativePath);
            Files.createDirectories(targetPath.getParent());
            Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetRelativePath;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("复制本地示例音乐失败", e);
            throw new BusinessException("复制本地示例音乐失败");
        }
    }

    private String resolveDemoTitle(String prompt) {
        if (!StringTools.isEmpty(prompt)) {
            return prompt.length() > 30 ? prompt.substring(0, 30) : prompt;
        }
        return "夜空中最亮的星";
    }
}
