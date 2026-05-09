package com.cookiemusic.controller;

import com.cookiemusic.annotation.GlobalInterceptor;
import com.cookiemusic.entity.enums.MusicStatusEnum;
import com.cookiemusic.entity.enums.ResponseCodeEnum;
import com.cookiemusic.entity.po.UserInfo;
import com.cookiemusic.entity.query.MusicInfoActionQuery;
import com.cookiemusic.entity.query.MusicInfoQuery;
import com.cookiemusic.entity.vo.PaginationResultVO;
import com.cookiemusic.entity.vo.ResponseVO;
import com.cookiemusic.entity.vo.UserInfoVO;
import com.cookiemusic.exception.BusinessException;
import com.cookiemusic.service.MusicInfoActionService;
import com.cookiemusic.service.MusicInfoService;
import com.cookiemusic.service.UserInfoService;
import com.cookiemusic.utils.CopyTools;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Validated
public class UserController extends ABaseController {

    @Resource
    private MusicInfoService musicInfoService;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private MusicInfoActionService musicInfoActionService;

    @RequestMapping("/getUserInfo")
    @GlobalInterceptor
    public ResponseVO getUserInfo(@NotEmpty String userId) {
        UserInfo userInfo = userInfoService.getUserInfoByUserId(userId);
        if (null == userInfo) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);

        MusicInfoQuery musicInfoQuery = new MusicInfoQuery();
        musicInfoQuery.setUserId(userId);
        musicInfoQuery.setMusicStatus(MusicStatusEnum.CREATED.getStatus());
        Integer musicCount = this.musicInfoService.findCountByParam(musicInfoQuery);
        userInfoVO.setMusicCount(musicCount);

        MusicInfoActionQuery actionQuery = new MusicInfoActionQuery();
        actionQuery.setMusicUserId(userId);
        Integer goodCount = musicInfoActionService.findCountByParam(actionQuery);
        userInfoVO.setGoodCount(goodCount);
        return getSuccessResponseVO(userInfoVO);
    }

    @RequestMapping("/loadUserMusic")
    @GlobalInterceptor
    public ResponseVO loadUserMusic(@NotEmpty String userId, Integer pageNo) {
        MusicInfoQuery musicInfoQuery = new MusicInfoQuery();
        musicInfoQuery.setPageNo(pageNo);
        musicInfoQuery.setUserId(userId);
        musicInfoQuery.setMusicStatus(MusicStatusEnum.CREATED.getStatus());
        musicInfoQuery.setOrderBy("create_time desc");
        PaginationResultVO resultVO = this.musicInfoService.findListByPage(musicInfoQuery);
        return getSuccessResponseVO(resultVO);
    }


}
