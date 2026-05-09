package com.cookiemusic.controller;

import com.cookiemusic.entity.enums.UserIntegralRecordTypeEnum;
import com.cookiemusic.entity.po.UserInfo;
import com.cookiemusic.entity.query.UserInfoQuery;
import com.cookiemusic.entity.vo.PaginationResultVO;
import com.cookiemusic.entity.vo.ResponseVO;
import com.cookiemusic.service.UserInfoService;
import com.cookiemusic.service.UserIntegralRecordService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Slf4j
@Validated
public class UserInfoController extends ABaseController {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private UserIntegralRecordService userIntegralRecordService;

    @RequestMapping("/loadUser")
    public ResponseVO loadUser(UserInfoQuery userInfoQuery) {
        userInfoQuery.setOrderBy("u.create_time desc");
        PaginationResultVO resultVO = userInfoService.findListByPage(userInfoQuery);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/changeUserStatus")
    public ResponseVO changeUserStatus(@NotEmpty String userId, @NotNull Integer status) {
        UserInfo updateInfo = new UserInfo();
        updateInfo.setStatus(status);
        userInfoService.updateUserInfoByUserId(updateInfo, userId);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/changeIntegral")
    public ResponseVO changeIntegral(@NotEmpty String userId, @NotNull Integer integral) {
        userIntegralRecordService.changeUserIntegral(integral < 0 ? UserIntegralRecordTypeEnum.ADMIN_DEDUCT : UserIntegralRecordTypeEnum.ADMIN_ADD, null, userId,
                integral, null);
        return getSuccessResponseVO(null);
    }
}
