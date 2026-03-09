package com.aop.springbootaideepseek.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "baidu.ocr")
public class BaiduOcrProperties {
    private String appId;
    private String apiKey;
    private String secretKey;
    private String generalUrl;
    private String accurateUrl;
    private String accessTokenUrl;

    // getters and setters
    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getGeneralUrl() {
        return generalUrl;
    }

    public void setGeneralUrl(String generalUrl) {
        this.generalUrl = generalUrl;
    }

    public String getAccurateUrl() {
        return accurateUrl;
    }

    public void setAccurateUrl(String accurateUrl) {
        this.accurateUrl = accurateUrl;
    }

    public String getAccessTokenUrl() {
        return accessTokenUrl;
    }

    public void setAccessTokenUrl(String accessTokenUrl) {
        this.accessTokenUrl = accessTokenUrl;
    }
}