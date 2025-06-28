# 実例で学ぶ：複数言語システムの実装

## 🏢 実在企業の技術スタック

### Uber
```
モバイルアプリ: Swift (iOS), Kotlin (Android)
マップサービス: C++
マイクロサービス: Go, Java
機械学習: Python
データ処理: Apache Spark (Scala)
Web: Node.js
```

### Instagram
```
Web/API: Python (Django)
データ処理: Python
キャッシング: C++ (Memcached)
検索: Java (Elasticsearch)
iOS: Objective-C → Swift
Android: Java → Kotlin
```

## 🔧 具体的な実装例：画像投稿サービス

### システム構成
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   React     │────▶│ Spring Boot │────▶│   Python    │
│  Frontend   │     │     API     │     │  ML Service │
└─────────────┘     └──────┬──────┘     └─────────────┘
                           │
                    ┌──────▼──────┐     ┌─────────────┐
                    │     Go      │────▶│     C++     │
                    │Image Upload │     │Image Process│
                    └─────────────┘     └─────────────┘
```

### 1. フロントエンド（React/TypeScript）
```typescript
// ImageUpload.tsx
const uploadImage = async (file: File) => {
  const formData = new FormData();
  formData.append('image', file);
  
  const response = await fetch('/api/images/upload', {
    method: 'POST',
    body: formData
  });
  
  return response.json();
};
```

### 2. APIゲートウェイ（Spring Boot/Java）
```java
@RestController
@RequestMapping("/api/images")
public class ImageController {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @PostMapping("/upload")
    public ImageResponse uploadImage(@RequestParam("image") MultipartFile file) {
        // Goサービスに転送
        String uploadUrl = "http://image-service:8080/upload";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", file.getResource());
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = 
            new HttpEntity<>(body, headers);
            
        return restTemplate.postForObject(uploadUrl, requestEntity, ImageResponse.class);
    }
}
```

### 3. 画像アップロードサービス（Go）
```go
// main.go
package main

import (
    "bytes"
    "encoding/json"
    "io"
    "net/http"
)

type ImageService struct {
    processorURL string
}

func (s *ImageService) HandleUpload(w http.ResponseWriter, r *http.Request) {
    // ファイル受信
    file, header, err := r.FormFile("image")
    if err != nil {
        http.Error(w, err.Error(), http.StatusBadRequest)
        return
    }
    defer file.Close()
    
    // バイナリデータ読み込み（高速）
    buf := bytes.NewBuffer(nil)
    if _, err := io.Copy(buf, file); err != nil {
        http.Error(w, err.Error(), http.StatusInternalServerError)
        return
    }
    
    // C++処理サービスに転送
    resp, err := http.Post(s.processorURL, "image/jpeg", buf)
    if err != nil {
        http.Error(w, err.Error(), http.StatusInternalServerError)
        return
    }
    
    // レスポンス
    json.NewEncoder(w).Encode(map[string]string{
        "status": "uploaded",
        "filename": header.Filename,
    })
}
```

### 4. 画像処理サービス（C++）
```cpp
// image_processor.cpp
#include <opencv2/opencv.hpp>
#include <httplib.h>
#include <json/json.h>

class ImageProcessor {
public:
    Json::Value processImage(const std::string& imageData) {
        // バイナリデータからOpenCVマトリックスに変換
        std::vector<uchar> data(imageData.begin(), imageData.end());
        cv::Mat image = cv::imdecode(data, cv::IMREAD_COLOR);
        
        // 画像処理（リサイズ、フィルター等）
        cv::Mat processed;
        cv::resize(image, processed, cv::Size(800, 600));
        
        // サムネイル生成
        cv::Mat thumbnail;
        cv::resize(image, thumbnail, cv::Size(200, 200));
        
        Json::Value result;
        result["width"] = processed.cols;
        result["height"] = processed.rows;
        result["processing_time_ms"] = 15;
        
        return result;
    }
};

int main() {
    httplib::Server svr;
    ImageProcessor processor;
    
    svr.Post("/process", [&](const httplib::Request& req, httplib::Response& res) {
        Json::Value result = processor.processImage(req.body);
        res.set_content(result.toStyledString(), "application/json");
    });
    
    svr.listen("0.0.0.0", 8082);
}
```

### 5. 画像分析サービス（Python）
```python
# image_analyzer.py
from flask import Flask, request, jsonify
import tensorflow as tf
import numpy as np

app = Flask(__name__)
model = tf.keras.applications.MobileNetV2(weights='imagenet')

@app.route('/analyze', methods=['POST'])
def analyze_image():
    # 画像データ受信
    image_data = request.files['image'].read()
    
    # TensorFlowで分析
    image = tf.image.decode_image(image_data)
    image = tf.image.resize(image, (224, 224))
    image = tf.keras.applications.mobilenet_v2.preprocess_input(image)
    
    # 予測
    predictions = model.predict(np.expand_dims(image, 0))
    decoded = tf.keras.applications.mobilenet_v2.decode_predictions(predictions)
    
    # 結果を返す
    results = []
    for _, label, score in decoded[0]:
        results.append({
            'label': label,
            'confidence': float(score)
        })
    
    return jsonify({
        'predictions': results,
        'model': 'MobileNetV2'
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
```

## 🚀 なぜこの組み合わせ？

### 各言語の役割

| サービス | 言語 | 選定理由 |
|----------|------|----------|
| Frontend | React/TS | 型安全、エコシステム |
| API Gateway | Java | 安定性、認証処理 |
| Upload | Go | 高速I/O、並行処理 |
| Image Processing | C++ | OpenCV、処理速度 |
| ML Analysis | Python | TensorFlow、ライブラリ |

## 📡 通信プロトコルの選択

### REST API（JSON）
```
利点: シンプル、デバッグ容易
欠点: オーバーヘッド大
用途: Web API、外部連携
```

### gRPC（Protocol Buffers）
```
利点: 高速、型安全、双方向通信
欠点: 設定複雑、デバッグ困難
用途: マイクロサービス間通信
```

### GraphQL
```
利点: 柔軟なクエリ、過不足ないデータ取得
欠点: 学習コスト、キャッシュ複雑
用途: 複雑なフロントエンド
```

### WebSocket
```
利点: リアルタイム、双方向
欠点: 接続管理が複雑
用途: チャット、通知
```

## 💡 実装のコツ

### 1. 共通インターフェース定義

```yaml
# api-spec.yaml (OpenAPI)
paths:
  /api/images/upload:
    post:
      summary: Upload image
      requestBody:
        content:
          multipart/form-data:
            schema:
              type: object
              properties:
                image:
                  type: string
                  format: binary
      responses:
        200:
          description: Success
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ImageResponse'
```

### 2. エラーハンドリング統一

```json
{
  "error": {
    "code": "IMG_TOO_LARGE",
    "message": "Image size exceeds 10MB",
    "service": "image-upload",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

### 3. ログ相関

```
Request-ID: 550e8400-e29b-41d4-a716-446655440000

[Frontend] Request-ID: 550e8400... - Uploading image
[Java API] Request-ID: 550e8400... - Received upload request
[Go Service] Request-ID: 550e8400... - Processing image
[C++ Service] Request-ID: 550e8400... - Resizing to 800x600
```

## 🎯 言語選択チェックリスト

```
□ パフォーマンス要件を満たすか？
□ 必要なライブラリは存在するか？
□ チームに経験者はいるか？
□ 採用市場に人材は豊富か？
□ 長期的な保守は可能か？
□ 他サービスとの連携は容易か？
□ テスト・デバッグツールは充実しているか？
□ セキュリティアップデートは頻繁か？
```

これらを総合的に判断して、最適な言語を選択します。