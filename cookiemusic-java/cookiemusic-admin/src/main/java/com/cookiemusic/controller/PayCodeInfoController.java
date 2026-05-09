package com.cookiemusic.controller;

import com.cookiemusic.entity.constants.Constants;
import com.cookiemusic.entity.enums.PayCodeStatusEnum;
import com.cookiemusic.entity.po.PayCodeInfo;
import com.cookiemusic.entity.query.PayCodeInfoQuery;
import com.cookiemusic.entity.vo.ResponseVO;
import com.cookiemusic.service.PayCodeInfoService;
import com.cookiemusic.utils.StringTools;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付码 Controller
 */
@RestController
@RequestMapping("/payCode")
@Validated
public class PayCodeInfoController extends ABaseController {

    @Resource
    private PayCodeInfoService payCodeInfoService;

    /**
     * 根据条件分页查询
     */
    @RequestMapping("/loadPayCodeList")
    public ResponseVO loadPayCodeList(PayCodeInfoQuery query) {
        query.setOrderBy("create_time desc");
        query.setQueryUser(true);
        return getSuccessResponseVO(payCodeInfoService.findListByPage(query));
    }

    /**
     * 新增
     */
    @RequestMapping("/createCode")
    public ResponseVO createCode(@NotNull BigDecimal amount) {
        PayCodeInfo bean = new PayCodeInfo();
        bean.setAmount(amount);
        String payCode;
        do {
            payCode = StringTools.getRandomNumber(Constants.LENGTH_8);
        } while (payCodeInfoService.getPayCodeInfoByPayCode(payCode) != null);
        bean.setPayCode(payCode);
        bean.setCreateTime(new Date());
        bean.setStatus(PayCodeStatusEnum.NO_USE.getStatus());
        payCodeInfoService.add(bean);
        return getSuccessResponseVO(payCode);
    }

    @RequestMapping("/delCode")
    public ResponseVO delCode(@NotEmpty String payCode) {
        payCodeInfoService.deletePayCodeInfoByPayCode(payCode);
        return getSuccessResponseVO(null);
    }
}
