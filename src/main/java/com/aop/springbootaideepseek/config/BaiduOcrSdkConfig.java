package com.aop.springbootaideepseek.config;

import com.baidu.aip.ocr.AipOcr;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 百度OCR SDK配置类
 */
@Configuration
public class BaiduOcrSdkConfig {

    @Bean
    public AipOcr aipOcr(BaiduOcrProperties properties) {
        // 初始化一个AipOcr
        AipOcr client = new AipOcr(properties.getAppId(), properties.getApiKey(), properties.getSecretKey());

        // 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);

        // 可选：设置代理（如果需要）
        // client.setHttpProxy("proxy_host", proxy_port);

        return client;
    }
}