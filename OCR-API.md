# 百度OCR集成 API 文档

Spring Boot AI DeepSeek 项目已经集成了百度OCR（光学字符识别）功能，提供了多种OCR识别接口。

## 配置参数

在 `application.yaml` 中已配置以下参数：

```yaml
baidu:
  ocr:
    app-id: 7491260
    api-key: 6n4Q5WvAbBqahAnIvWxzd16w
    secret-key: fIA1JPXyK8FPEz6XUDwkh0f435H8wXqC
    general-url: https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic
    accurate-url: https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic
    access-token-url: https://aip.baidubce.com/oauth/2.0/token
```

## API 端点

所有OCR端点都位于 `/ocr` 路径下，服务器运行在端口 `8899`。

### 1. 服务健康检查

检查OCR服务是否可用。

**请求：**
```
GET http://localhost:8899/ocr/health
```

**响应示例（成功）：**
```json
{
  "status": "UP",
  "message": "百度OCR服务可用",
  "token_available": true
}
```

### 2. 通用文字识别（基础版）

对图片进行通用文字识别，返回识别的文本。

**请求：**
```
POST http://localhost:8899/ocr/general-basic
Content-Type: application/json
```

**请求体：**
```json
{
  "image": "base64编码的图片数据"
}
```

**响应示例：**
```json
{
  "success": true,
  "data": {
    "log_id": 2471272194,
    "words_result": [
      {
        "words": "识别出的文本1"
      },
      {
        "words": "识别出的文本2"
      }
    ],
    "words_result_num": 2
  },
  "timestamp": 1646647890123
}
```

### 3. 通用文字识别（高精度版）

对图片进行高精度文字识别，准确率更高。

**请求：**
```
POST http://localhost:8899/ocr/accurate-basic
Content-Type: application/json
```

**请求体：**
```json
{
  "image": "base64编码的图片数据"
}
```

### 4. 通用文字识别（含位置信息）

返回包含文字位置、方向、置信度等详细信息的识别结果。

**请求：**
```
POST http://localhost:8899/ocr/general
Content-Type: application/json
```

**请求体：**
```json
{
  "image": "base64编码的图片数据"
}
```

**响应示例：**
```json
{
  "success": true,
  "data": {
    "log_id": 2471272194,
    "words_result": [
      {
        "words": "识别出的文本",
        "location": {
          "top": 10,
          "left": 20,
          "width": 100,
          "height": 30
        },
        "probability": {
          "average": 0.95,
          "min": 0.9,
          "variance": 0.01
        }
      }
    ],
    "words_result_num": 1
  },
  "timestamp": 1646647890123
}
```

### 5. 文件上传接口（基础版）

直接上传图片文件进行识别。

**请求：**
```
POST http://localhost:8899/ocr/upload/general-basic
Content-Type: multipart/form-data
```

**表单参数：**
- `file`: 图片文件（支持 JPG、PNG、BMP 等格式）

### 6. 文件上传接口（高精度版）

直接上传图片文件进行高精度识别。

**请求：**
```
POST http://localhost:8899/ocr/upload/accurate-basic
Content-Type: multipart/form-data
```

**表单参数：**
- `file`: 图片文件

## 错误处理

所有接口都返回统一的错误格式：

**错误响应示例：**
```json
{
  "success": false,
  "message": "错误描述信息",
  "timestamp": 1646647890123
}
```

常见错误：
- `400 Bad Request`: 请求参数错误，如图片数据为空
- `500 Internal Server Error`: 服务器内部错误，如百度OCR服务不可用
- `503 Service Unavailable`: OCR服务不可用，如API密钥无效

## 使用示例

### cURL 示例

1. **健康检查：**
```bash
curl -X GET "http://localhost:8899/ocr/health"
```

2. **Base64图片识别：**
```bash
# 将图片转换为base64
IMAGE_BASE64=$(base64 -i image.jpg | tr -d '\n')

curl -X POST "http://localhost:8899/ocr/general-basic" \
  -H "Content-Type: application/json" \
  -d "{\"image\": \"$IMAGE_BASE64\"}"
```

3. **文件上传识别：**
```bash
curl -X POST "http://localhost:8899/ocr/upload/general-basic" \
  -F "file=@image.jpg"
```

### Python 示例

```python
import requests
import base64

# 1. 健康检查
response = requests.get("http://localhost:8899/ocr/health")
print(response.json())

# 2. Base64图片识别
with open("image.jpg", "rb") as image_file:
    image_base64 = base64.b64encode(image_file.read()).decode("utf-8")

payload = {"image": image_base64}
response = requests.post("http://localhost:8899/ocr/general-basic", json=payload)
print(response.json())

# 3. 文件上传识别
with open("image.jpg", "rb") as image_file:
    files = {"file": image_file}
    response = requests.post("http://localhost:8899/ocr/upload/general-basic", files=files)
    print(response.json())
```

## 注意事项

1. **图片大小限制**：百度OCR API 对图片大小有限制，建议图片不超过 4MB
2. **图片格式**：支持 JPG、PNG、BMP 等常见格式
3. **访问频率限制**：百度OCR API 有调用频率限制，具体请参考百度AI平台文档
4. **Token缓存**：访问令牌会自动缓存并在过期前刷新，无需手动管理
5. **并发处理**：服务支持并发请求，但受百度API限制

## 项目结构

```
src/main/java/com/aop/springbootaideepseek/
├── config/
│   └── BaiduOcrProperties.java     # OCR配置属性类
├── controller/
│   ├── BaiduOcrController.java     # OCR控制器
│   └── DeepSeekController.java     # DeepSeek AI控制器
├── service/
│   └── BaiduOcrService.java        # OCR服务实现
└── SpringbootAiDeepseekApplication.java # 主应用
```

## 启动应用

```bash
# 进入项目目录
cd springboot-ai-deepseek

# 启动应用
mvn spring-boot:run
```

应用启动后，可通过 http://localhost:8899 访问所有API。

## 相关链接

- [百度AI开放平台](https://ai.baidu.com/)
- [百度OCR技术文档](https://ai.baidu.com/tech/ocr)
- [Spring Boot文档](https://spring.io/projects/spring-boot)