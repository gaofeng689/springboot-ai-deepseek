package com.aop.springbootaideepseek.service;

import com.aop.springbootaideepseek.config.BaiduOcrProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Service
public class BaiduOcrService {
    private static final Logger log = LoggerFactory.getLogger(BaiduOcrService.class);

    private final BaiduOcrProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String cachedAccessToken;
    private long tokenExpiryTime;

    public BaiduOcrService(BaiduOcrProperties properties, RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * 获取访问令牌，如果缓存有效则返回缓存的令牌
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
     * @return 识别结果文本
     */
    public String generalBasic(String imageBase64) {
        return ocrRequest(properties.getGeneralUrl(), imageBase64, new HashMap<>());
    }

    /**
     * 通用文字识别（高精度版）
     * @param imageBase64 图片的base64编码
     * @return 识别结果文本
     */
    public String accurateBasic(String imageBase64) {
        return ocrRequest(properties.getAccurateUrl(), imageBase64, new HashMap<>());
    }

    /**
     * 通用文字识别（含位置信息）
     * @param imageBase64 图片的base64编码
     * @return 完整的JSON响应
     */
    public String general(String imageBase64) {
        Map<String, Object> options = new HashMap<>();
        options.put("recognize_granularity", "big");
        options.put("language_type", "CHN_ENG");
        options.put("detect_direction", "true");
        options.put("vertexes_location", "true");
        options.put("probability", "true");
        return ocrRequest(properties.getGeneralUrl(), imageBase64, options);
    }

    /**
     * 执行OCR请求
     */
    private String ocrRequest(String url, String imageBase64, Map<String, Object> options) {
        String accessToken = getAccessToken();
        String requestUrl = url + "?access_token=" + accessToken;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("image", imageBase64);
        options.forEach((key, value) -> body.add(key, value.toString()));

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(requestUrl, entity, String.class);
            String responseBody = response.getBody();

            if (response.getStatusCode() == HttpStatus.OK) {
                // 检查响应是否包含错误码
                try {
                    JsonNode root = objectMapper.readTree(responseBody);
                    if (root.has("error_code")) {
                        int errorCode = root.path("error_code").asInt();
                        String errorMsg = root.path("error_msg").asText();
                        log.error("百度OCR API返回错误: code={}, message={}", errorCode, errorMsg);
                        throw new RuntimeException("百度OCR错误 [" + errorCode + "]: " + errorMsg);
                    }
                } catch (Exception e) {
                    // 如果解析JSON失败，但HTTP状态码是200，可能响应不是JSON或者是成功的非标准响应
                    // 继续返回原始响应
                    log.debug("响应不是JSON或解析失败，返回原始响应");
                }
                return responseBody;
            } else {
                log.error("OCR请求失败，HTTP状态码: {}, 响应: {}", response.getStatusCode(), responseBody);
                throw new RuntimeException("OCR请求失败: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("OCR请求异常", e);
            throw new RuntimeException("OCR请求异常: " + e.getMessage(), e);
        }
    }
}