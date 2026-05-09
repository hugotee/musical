package com.cookiemusic.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cookiemusic.api.MusicCreateApi;
import com.cookiemusic.entity.config.AppConfig;
import com.cookiemusic.entity.constants.Constants;
import com.cookiemusic.entity.dto.MusicCreationResultDTO;
import com.cookiemusic.entity.dto.MusicSettingDTO;
import com.cookiemusic.entity.dto.MusicTaskDTO;
import com.cookiemusic.entity.enums.*;
import com.cookiemusic.entity.po.MusicCreation;
import com.cookiemusic.entity.po.MusicInfo;
import com.cookiemusic.entity.po.SysDict;
import com.cookiemusic.entity.query.MusicCreationQuery;
import com.cookiemusic.entity.query.MusicInfoQuery;
import com.cookiemusic.entity.query.SimplePage;
import com.cookiemusic.entity.vo.PaginationResultVO;
import com.cookiemusic.exception.BusinessException;
import com.cookiemusic.mappers.MusicCreationMapper;
import com.cookiemusic.mappers.MusicInfoMapper;
import com.cookiemusic.redis.RedisComponent;
import com.cookiemusic.service.MusicCreationService;
import com.cookiemusic.service.UserIntegralRecordService;
import com.cookiemusic.spring.SpringContext;
import com.cookiemusic.utils.JsonUtils;
import com.cookiemusic.utils.OKHttpUtils;
import com.cookiemusic.utils.StringTools;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Resource
    private UserIntegralRecordService userIntegralRecordService;

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
        ModelInfo modelInfo = getModelInfo(musicTypeEnum, musicCreation.getModel());

        List<SysDict> sysDictSubList = redisComponent.getDictSubList(musicTypeEnum.getDictCode());
        if (sysDictSubList == null || sysDictSubList.isEmpty()) {
            throw new BusinessException("系统配置错误，请联系管理员");
        }
        Optional<SysDict> dictInfo = sysDictSubList.stream().filter(value -> value.getDictCode().equals(musicCreation.getModel())).findFirst();
        if (dictInfo.isEmpty()) {
            throw new BusinessException("系统配置错误，请联系管理员");
        }
        SysDict sysDict = dictInfo.get();
        Integer integralCost = Integer.parseInt(sysDict.getDictValue());

        String creationId = StringTools.getRandomString(Constants.LENGTH_15);

        Date curDate = new Date();

        musicCreation.setCreationId(creationId);
        musicCreation.setSettings(JsonUtils.convertObj2Json(musicSettingDTO));
        musicCreation.setCreateTime(curDate);
        musicCreationMapper.insert(musicCreation);

        // 扣减积分
        userIntegralRecordService.changeUserIntegral(
                UserIntegralRecordTypeEnum.CREATE_MUSIC,
                creationId,
                musicCreation.getUserId(),
                -integralCost,
                null);

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
        if (MusicTypeEnum.PURE.getType().equals(musicCreation.getMusicType())) {
            // 纯音乐: 本地 MusicGen 同步生成
            return createWithMusicGen(musicCreation, prompt, musicSettingDTO, curDate);
        } else {
            // 歌曲: 天谱乐 API 异步生成（带人声）
            return createWithTianpuyue(musicCreation, prompt, modelInfo, curDate);
        }
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

    /** 纯音乐: 本地 MusicGen Small 同步生成 */
    private List<String> createWithMusicGen(MusicCreation musicCreation, String prompt,
                                            MusicSettingDTO musicSettingDTO, Date curDate) {
        MusicCreationResultDTO genResult = callMusicGen(prompt, musicCreation.getMusicType(),
                musicSettingDTO.getMusicGener(), musicSettingDTO.getMusicEmotion(), musicSettingDTO.getMusicSex());
        if (!genResult.getCreateSuccess()) {
            throw new BusinessException("AI音乐生成失败，请稍后重试");
        }

        String musicId = StringTools.getRandomNumber(Constants.LENGTH_12);
        String localPath = downloadMusicGenAudio(genResult, musicId);

        MusicInfo musicInfo = new MusicInfo();
        musicInfo.setMusicId(musicId);
        musicInfo.setUserId(musicCreation.getUserId());
        musicInfo.setCreationId(musicCreation.getCreationId());
        musicInfo.setGoodCount(0);
        musicInfo.setPlayCount(0);
        musicInfo.setCreateTime(curDate);
        musicInfo.setCommendType(CommendTypeEnum.NOT_COMMEND.getType());
        musicInfo.setMusicStatus(MusicStatusEnum.CREATED.getStatus());
        musicInfo.setTaskId(genResult.getTaskId());
        musicInfo.setMusicType(musicCreation.getMusicType());
        musicInfo.setMusicTitle(genResult.getTitle() != null ? genResult.getTitle()
                : prompt.substring(0, Math.min(30, prompt.length())));
        musicInfo.setDuration(genResult.getDuration());
        musicInfo.setAudioPath(localPath);
        musicInfo.setLyrics("[]");
        musicInfoMapper.insert(musicInfo);

        List<String> musicIdList = new ArrayList<>();
        musicIdList.add(musicId);
        return musicIdList;
    }

    /** 歌曲: 天谱乐 API 异步生成（支持人声） */
    private List<String> createWithTianpuyue(MusicCreation musicCreation, String prompt,
                                             ModelInfo modelInfo, Date curDate) {
        MusicCreateApi musicCreateApi = (MusicCreateApi) SpringContext.getBean(modelInfo.apiCode());
        List<String> itemIds;
        if (MusicTypeEnum.MUSIC.getType().equals(musicCreation.getMusicType())) {
            itemIds = musicCreateApi.createMusic(modelInfo.model(), prompt, musicCreation.getLyrics());
        } else {
            itemIds = musicCreateApi.createPureMusic(modelInfo.model(), prompt);
        }
        if (itemIds == null || itemIds.isEmpty()) {
            throw new BusinessException("音乐创作失败");
        }

        List<MusicInfo> musicInfoList = new ArrayList<>();
        List<String> musicIdList = new ArrayList<>();
        for (String item : itemIds) {
            MusicInfo musicInfo = new MusicInfo();
            String musicId = StringTools.getRandomNumber(Constants.LENGTH_12);
            musicInfo.setMusicId(musicId);
            musicInfo.setUserId(musicCreation.getUserId());
            musicInfo.setCreationId(musicCreation.getCreationId());
            musicInfo.setGoodCount(0);
            musicInfo.setPlayCount(0);
            musicInfo.setCreateTime(curDate);
            musicInfo.setCommendType(CommendTypeEnum.NOT_COMMEND.getType());
            musicInfo.setMusicStatus(MusicStatusEnum.CREATING.getStatus());
            musicInfo.setTaskId(item);
            musicInfo.setMusicType(musicCreation.getMusicType());
            musicInfoList.add(musicInfo);

            if (appConfig.getAutoCheckMusic()) {
                MusicTaskDTO musicTaskDto = new MusicTaskDTO();
                musicTaskDto.setApiCode(modelInfo.apiCode());
                musicTaskDto.setMusicId(musicId);
                musicTaskDto.setTaskId(item);
                musicTaskDto.setMusicType(musicCreation.getMusicType());
                redisComponent.addMusicCreateTask(musicTaskDto);
            }
            musicIdList.add(musicId);
        }
        musicInfoMapper.insertBatch(musicInfoList);
        return musicIdList;
    }

    private MusicCreationResultDTO callMusicGen(String prompt, Integer musicType,
                                                String musicGener, String musicEmotion, String musicSex) {
        String serverUrl = appConfig.getMusicgenServerUrl();
        String url = serverUrl + "/api/generate";

        Map<String, Object> params = new HashMap<>();
        params.put("prompt", prompt);
        params.put("duration", 30);
        params.put("guidance_scale", 3.0);
        params.put("musicType", musicType);
        if (musicGener != null && !musicGener.isBlank()) {
            params.put("musicGener", musicGener);
        }
        if (musicEmotion != null && !musicEmotion.isBlank()) {
            params.put("musicEmotion", musicEmotion);
        }
        if (musicSex != null && !musicSex.isBlank()) {
            params.put("musicSex", musicSex);
        }

        String jsonBody = JSON.toJSONString(params);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        // MusicGen 生成需要约 90 秒，超时设为 180 秒
        String responseStr = OKHttpUtils.postRequest4Json(url, headers, jsonBody, 180);
        JSONObject json = JSON.parseObject(responseStr);

        MusicCreationResultDTO result = new MusicCreationResultDTO();
        result.setCreateSuccess(json.getBoolean("createSuccess"));
        result.setTaskId(json.getString("taskId"));
        result.setTitle(json.getString("title"));
        result.setDuration(json.getInteger("duration"));
        result.setAudioUrl(serverUrl + json.getString("audioUrl"));
        return result;
    }

    private String downloadMusicGenAudio(MusicCreationResultDTO result, String musicId) {
        String folderName = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String relativePath = folderName + "/" + musicId + Constants.AUDIO_SUFFIX;
        Path targetPath = Path.of(appConfig.getProjectFolder(), Constants.FILE_FOLDER_FILE, relativePath);
        try {
            Files.createDirectories(targetPath.getParent());
        } catch (Exception e) {
            log.error("创建目录失败", e);
            throw new BusinessException("文件存储异常");
        }
        // 下载 WAV 并转为 mp3 后缀存储（浏览器兼容）
        OKHttpUtils.download(result.getAudioUrl(), targetPath.toString());
        return relativePath;
    }
}
