package com.cookiemusic.service.impl;

import com.cookiemusic.entity.config.AppConfig;
import com.cookiemusic.entity.constants.Constants;
import com.cookiemusic.entity.dto.TokenUserInfoDTO;
import com.cookiemusic.entity.enums.PageSize;
import com.cookiemusic.entity.enums.ResponseCodeEnum;
import com.cookiemusic.entity.enums.UserStatusEnum;
import com.cookiemusic.entity.po.UserInfo;
import com.cookiemusic.entity.query.SimplePage;
import com.cookiemusic.entity.query.UserInfoQuery;
import com.cookiemusic.entity.vo.PaginationResultVO;
import com.cookiemusic.exception.BusinessException;
import com.cookiemusic.mappers.UserInfoMapper;
import com.cookiemusic.redis.RedisComponent;
import com.cookiemusic.service.UserInfoService;
import com.cookiemusic.utils.CopyTools;
import com.cookiemusic.utils.FileUtils;
import com.cookiemusic.utils.StringTools;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;


/**
 * 用户信息 业务接口实现
 */
@Slf4j
@Service("userInfoService")
public class UserInfoServiceImpl implements UserInfoService {

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private AppConfig appConfig;

    @Resource
    private FileUtils fileUtils;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<UserInfo> findListByParam(UserInfoQuery param) {
        return this.userInfoMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(UserInfoQuery param) {
        return this.userInfoMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<UserInfo> list = this.findListByParam(param);
        PaginationResultVO<UserInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(UserInfo bean) {
        return this.userInfoMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<UserInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userInfoMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<UserInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userInfoMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(UserInfo bean, UserInfoQuery param) {
        StringTools.checkParam(param);
        return this.userInfoMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(UserInfoQuery param) {
        StringTools.checkParam(param);
        return this.userInfoMapper.deleteByParam(param);
    }

    /**
     * 根据UserId获取对象
     */
    @Override
    public UserInfo getUserInfoByUserId(String userId) {
        return this.userInfoMapper.selectByUserId(userId);
    }

    /**
     * 根据UserId修改
     */
    @Override
    public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
        return this.userInfoMapper.updateByUserId(bean, userId);
    }

    /**
     * 根据UserId删除
     */
    @Override
    public Integer deleteUserInfoByUserId(String userId) {
        return this.userInfoMapper.deleteByUserId(userId);
    }

    /**
     * 根据Email获取对象
     */
    @Override
    public UserInfo getUserInfoByEmail(String email) {
        return this.userInfoMapper.selectByEmail(email);
    }

    /**
     * 根据Email修改
     */
    @Override
    public Integer updateUserInfoByEmail(UserInfo bean, String email) {
        return this.userInfoMapper.updateByEmail(bean, email);
    }

    /**
     * 根据Email删除
     */
    @Override
    public Integer deleteUserInfoByEmail(String email) {
        return this.userInfoMapper.deleteByEmail(email);
    }


    @Override
    public void register(String email, String nickName, String password) {
        UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
        if (null != userInfo) {
            throw new BusinessException("邮箱账号已经存在");
        }
        Date curDate = new Date();
        String userId = StringTools.getRandomNumber(Constants.LENGTH_12);
        userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setNickName(nickName);
        userInfo.setEmail(email);
        userInfo.setPassword(StringTools.encodeByMD5(password));
        userInfo.setCreateTime(curDate);
        userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
        /**
         * 随机获取头像
         */
        userInfo.setAvatar(fileUtils.copyAvatar(userId));
        this.userInfoMapper.insert(userInfo);
    }

    @Override
    public TokenUserInfoDTO login(String email, String password) {
        UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
        if (null == userInfo || !userInfo.getPassword().equals(password)) {
            throw new BusinessException("账号或者密码错误");
        }
        if (UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())) {
            throw new BusinessException("账号已禁用");
        }

        UserInfo updateInfo = new UserInfo();
        updateInfo.setLastLoginTime(new Date());
        this.userInfoMapper.updateByUserId(updateInfo, userInfo.getUserId());

        TokenUserInfoDTO tokenUserInfoDto = CopyTools.copy(userInfo, TokenUserInfoDTO.class);
        String token = StringTools.encodeByMD5(tokenUserInfoDto.getUserId() + StringTools.getRandomString(Constants.LENGTH_20));
        tokenUserInfoDto.setToken(token);
        redisComponent.saveTokenUserInfoDto(tokenUserInfoDto);
        return tokenUserInfoDto;
    }

    @Override
    public void updateUserInfo(TokenUserInfoDTO tokenUserInfoDto, MultipartFile avatar, String nickName) {
        UserInfo updateInfo = new UserInfo();
        if (avatar != null) {
            String suffix = StringTools.getFileSuffix(avatar.getOriginalFilename());
            String fileName = tokenUserInfoDto.getUserId() + suffix;
            String avatarPath = fileUtils.uploadFile(avatar, Constants.FILE_FOLDER_AVATAR_NAME, fileName);
            updateInfo.setAvatar(avatarPath + "&" + System.currentTimeMillis());
        }
        updateInfo.setNickName(nickName);
        userInfoMapper.updateByUserId(updateInfo, tokenUserInfoDto.getUserId());
        Boolean updateTokenInfo = false;
        if (!tokenUserInfoDto.getNickName().equals(nickName)) {
            tokenUserInfoDto.setNickName(nickName);
            updateTokenInfo = true;
        }
       if (updateInfo.getAvatar() != null && !tokenUserInfoDto.getAvatar().equals(updateInfo.getAvatar())) {
            tokenUserInfoDto.setAvatar(updateInfo.getAvatar());
            updateTokenInfo = true;
        }
        if (!updateTokenInfo) {
            return;
        }
        redisComponent.saveTokenUserInfoDto(tokenUserInfoDto);
    }

    @Override
    public void updatePassword(String userId, String oldPassword, String newPassword) {
        UserInfo userInfo = this.userInfoMapper.selectByUserId(userId);
        if (null == userInfo) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (!userInfo.getPassword().equals(StringTools.encodeByMD5(oldPassword))) {
            throw new BusinessException("原始密码错误");
        }
        UserInfo updateInfo = new UserInfo();
        updateInfo.setPassword(StringTools.encodeByMD5(newPassword));
        this.userInfoMapper.updateByUserId(updateInfo, userId);
    }

    @Override
    public void updateUserStatus(Integer status, String userId) {
        UserStatusEnum userStatusEnum = UserStatusEnum.getByStatus(status);
        if (userStatusEnum == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        UserInfo updateInfo = new UserInfo();
        updateInfo.setStatus(userStatusEnum.getStatus());
        userInfoMapper.updateByUserId(updateInfo, userId);
    }
}