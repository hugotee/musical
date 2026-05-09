package com.cookiemusic.aspect;

import com.cookiemusic.annotation.GlobalInterceptor;
import com.cookiemusic.entity.dto.TokenUserInfoDTO;
import com.cookiemusic.entity.enums.ResponseCodeEnum;
import com.cookiemusic.exception.BusinessException;
import com.cookiemusic.redis.RedisComponent;
import com.cookiemusic.utils.StringTools;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Component("operationAspect")
@Aspect
@Slf4j
public class GlobalOperationAspect {

    @Resource
    private RedisComponent redisComponent;

    @Before("@annotation(com.cookiemusic.annotation.GlobalInterceptor)")
    public void interceptorDo(JoinPoint point) {
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
        if (null == interceptor) {
            return;
        }
        /**
         * 校验登录
         */
        if (interceptor.checkLogin()) {
            checkLogin();
        }
    }

    //校验登录
    private void checkLogin() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String token = request.getHeader("token");
        if (StringTools.isEmpty(token)) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        TokenUserInfoDTO tokenUserInfoDto = redisComponent.getTokenUserInfoDto(token);
        if (System.getProperty("dev") != null) {
            tokenUserInfoDto = new TokenUserInfoDTO();
            tokenUserInfoDto.setUserId("1000000");
            tokenUserInfoDto.setNickName("程序员老罗");
            tokenUserInfoDto.setToken(token);
            redisComponent.saveTokenUserInfoDto(tokenUserInfoDto);
        }
        if (tokenUserInfoDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
    }
}