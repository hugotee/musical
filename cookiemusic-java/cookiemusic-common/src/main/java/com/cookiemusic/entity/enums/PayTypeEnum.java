package com.cookiemusic.entity.enums;


public enum PayTypeEnum {
    PROXY("proxy", "使用第三方代理接口"),
    WECHAT("wechat", "使用微信官方");

    private String payType;
    private String desc;

    PayTypeEnum(String payType, String desc) {
        this.payType = payType;
        this.desc = desc;
    }

    public String getPayType() {
        return payType;
    }

    public String getDesc() {
        return desc;
    }
}
