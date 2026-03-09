package com.aop.springbootaideepseek.controller;

import com.aop.springbootaideepseek.service.BaiduOcrSdkService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ocr")
public class BaiduOcrController {

    private final BaiduOcrSdkService ocrService;
    private final ObjectMapper objectMapper;

    public BaiduOcrController(BaiduOcrSdkService ocrService, ObjectMapper objectMapper) {
        this.ocrService = ocrService;
        this.objectMapper = objectMapper;
    }

    /**
     * 通用文字识别（基础版）
     * @param imageBase64 图片的base64编码
     * @return 识别结果
     */
    @PostMapping("/general-basic")
    public ResponseEntity<Map<String, Object>> generalBasic(@RequestBody Map<String, String> request) {
        String imageBase64 = request.get("image");
        if (imageBase64 == null || imageBase64.isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("image参数不能为空"));
        }

        try {
            String result = ocrService.generalBasic(imageBase64);
            return ResponseEntity.ok(createSuccessResponse(result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("识别失败: " + e.getMessage()));
        }
    }

    /**
     * 通用文字识别（高精度版）
     * @param imageBase64 图片的base64编码
     * @return 识别结果
     */
    @PostMapping("/accurate-basic")
    public ResponseEntity<Map<String, Object>> accurateBasic(@RequestBody Map<String, String> request) {
        String imageBase64 = request.get("image");
        if (imageBase64 == null || imageBase64.isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("image参数不能为空"));
        }

        try {
            String result = ocrService.accurateBasic(imageBase64);
            return ResponseEntity.ok(createSuccessResponse(result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("识别失败: " + e.getMessage()));
        }
    }

    /**
     * 通用文字识别（含位置信息）
     * @param imageBase64 图片的base64编码
     * @return 识别结果
     */
    @PostMapping("/general")
    public ResponseEntity<Map<String, Object>> general(@RequestBody Map<String, String> request) {
        String imageBase64 = request.get("image");
        if (imageBase64 == null || imageBase64.isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("image参数不能为空"));
        }

        try {
            String result = ocrService.general(imageBase64);
            return ResponseEntity.ok(createSuccessResponse(result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("识别失败: " + e.getMessage()));
        }
    }

    /**
     * 上传图片文件进行OCR识别（基础版）
     * @param file 图片文件
     * @return 识别结果
     */
    @PostMapping("/upload/general-basic")
    public ResponseEntity<Map<String, Object>> uploadGeneralBasic(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("文件不能为空"));
        }

        try {
            String imageBase64 = Base64.getEncoder().encodeToString(file.getBytes());
            String result = ocrService.generalBasic(imageBase64);
            return ResponseEntity.ok(createSuccessResponse(result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("识别失败: " + e.getMessage()));
        }
    }

    /**
     * 上传图片文件进行OCR识别（高精度版）
     * @param file 图片文件
     * @return 识别结果
     */
    @PostMapping("/upload/accurate-basic")
    public ResponseEntity<Map<String, Object>> uploadAccurateBasic(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("文件不能为空"));
        }

        try {
            String imageBase64 = Base64.getEncoder().encodeToString(file.getBytes());
            String result = ocrService.accurateBasic(imageBase64);
            return ResponseEntity.ok(createSuccessResponse(result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("识别失败: " + e.getMessage()));
        }
    }

    /**
     * 测试端点 - 检查OCR服务是否可用
     * @return 服务状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        try {
            // 尝试获取访问令牌来测试服务
            String token = ocrService.getAccessToken();
            response.put("status", "UP");
            response.put("message", "百度OCR服务可用");
            response.put("token_available", token != null && !token.isEmpty());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("message", "百度OCR服务不可用: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    private String extractWordsFromOcrResult(String ocrResult) {
        try {
            JsonNode root = objectMapper.readTree(ocrResult);
            JsonNode wordsResult = root.path("words_result");
            if (wordsResult.isArray() && wordsResult.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < wordsResult.size(); i++) {
                    JsonNode item = wordsResult.get(i);
                    JsonNode wordsNode = item.path("words");
                    if (!wordsNode.isMissingNode()) {
                        sb.append(wordsNode.asText());
                        if (i < wordsResult.size() - 1) {
                            sb.append(" ");
                        }
                    }
                }
                return sb.toString();
            }
        } catch (Exception e) {
            // 解析失败，返回原始结果
        }
        return ocrResult;
    }

    private Map<String, Object> createSuccessResponse(String result) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", extractWordsFromOcrResult(result));
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}