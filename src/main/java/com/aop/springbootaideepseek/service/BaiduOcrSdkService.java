package com.aop.springbootaideepseek.service;

import com.aop.springbootaideepseek.config.BaiduOcrProperties;
import com.baidu.aip.ocr.AipOcr;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 百度OCR SDK服务实现
 * 使用百度官方Java SDK进行OCR识别
 */
@Service
public class BaiduOcrSdkService {
    private static final Logger log = LoggerFactory.getLogger(BaiduOcrSdkService.class);

    private final BaiduOcrProperties properties;
    private final AipOcr aipOcr;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    private String cachedAccessToken;
    private long tokenExpiryTime;

    public BaiduOcrSdkService(BaiduOcrProperties properties, AipOcr aipOcr,
                              ObjectMapper objectMapper, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.aipOcr = aipOcr;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder.build();
    }

    /**
     * 获取访问令牌，如果缓存有效则返回缓存的令牌
     * 注意：SDK内部会自动管理token，此方法仅用于健康检查等场景
     */
    public String getAccessToken() {
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return cachedAccessToken;
        }
        String url = properties.getAccessTokenUrl();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");
        params.add("client_id", properties.getApiKey());
        params.add("client_secret", properties.getSecretKey());

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParams(params);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(builder.toUriString(), String.class);
            String responseBody = response.getBody();

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(responseBody);

                // 检查是否包含错误信息
                if (root.has("error")) {
                    String error = root.path("error").asText();
                    String errorDescription = root.path("error_description").asText();
                    log.error("获取访问令牌失败: error={}, description={}", error, errorDescription);
                    throw new RuntimeException("获取访问令牌失败: " + error + " - " + errorDescription);
                }

                String accessToken = root.path("access_token").asText();
                if (accessToken == null || accessToken.isEmpty()) {
                    log.error("获取访问令牌失败: 响应中缺少access_token, 响应体: {}", responseBody);
                    throw new RuntimeException("获取访问令牌失败: 响应中缺少access_token");
                }

                int expiresIn = root.path("expires_in").asInt(2592000); // 默认30天
                cachedAccessToken = accessToken;
                tokenExpiryTime = System.currentTimeMillis() + (expiresIn * 1000L) - 60000; // 提前1分钟过期
                log.info("获取百度OCR访问令牌成功，过期时间: {} 秒", expiresIn);
                return accessToken;
            } else {
                log.error("获取访问令牌失败，HTTP状态码: {}, 响应: {}", response.getStatusCode(), responseBody);
                throw new RuntimeException("获取访问令牌失败: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("获取访问令牌异常", e);
            throw new RuntimeException("获取访问令牌异常: " + e.getMessage(), e);
        }
    }

    /**
     * 通用文字识别（基础版）
     * @param imageBase64 图片的base64编码
     * @return 识别结果文本（JSON字符串）
     */
    public String generalBasic(String imageBase64) {
        return ocrSdkRequest(imageBase64, "generalBasic", new HashMap<String, String>());
    }

    /**
     * 通用文字识别（高精度版）
     * @param imageBase64 图片的base64编码
     * @return 识别结果文本（JSON字符串）
     */
    public String accurateBasic(String imageBase64) {
        return ocrSdkRequest(imageBase64, "accurateBasic", new HashMap<String, String>());
    }

    /**
     * 通用文字识别（含位置信息）
     * @param imageBase64 图片的base64编码
     * @return 识别结果文本（JSON字符串）
     */
    public String general(String imageBase64) {
        HashMap<String, String> options = new HashMap<>();
        options.put("recognize_granularity", "big");
        options.put("language_type", "CHN_ENG");
        options.put("detect_direction", "true");
        options.put("vertexes_location", "true");
        options.put("probability", "true");
        return ocrSdkRequest(imageBase64, "general", options);
    }

    /**
     * 执行OCR请求（使用百度SDK）
     */
    private String ocrSdkRequest(String imageBase64, String method, HashMap<String, String> options) {
        try {
            // 百度SDK接受base64字符串或字节数组，这里直接传递base64字符串
            JSONObject result;
            switch (method) {
                case "generalBasic":
                    // 通用文字识别（基础版）
                    result = aipOcr.basicGeneral(imageBase64, options);
                    break;
                case "accurateBasic":
                    // 通用文字识别（高精度版）
                    result = aipOcr.accurateGeneral(imageBase64, options);
                    break;
                case "general":
                    // 通用文字识别（含位置信息版）
                    result = aipOcr.general(imageBase64, options);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的OCR方法: " + method);
            }

            // 检查错误码
            if (result.has("error_code")) {
                int errorCode = result.getInt("error_code");
                String errorMsg = result.getString("error_msg");
                log.error("百度OCR SDK返回错误: code={}, message={}", errorCode, errorMsg);
                throw new RuntimeException("百度OCR错误 [" + errorCode + "]: " + errorMsg);
            }

            return result.toString();
        } catch (Exception e) {
            log.error("OCR SDK请求异常", e);
            throw new RuntimeException("OCR请求异常: " + e.getMessage(), e);
        }
    }
}