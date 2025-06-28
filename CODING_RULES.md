# コーディング規約

## 基本方針
- 分かりやすく、スマート、セキュアなコードを書く
- 可読性と保守性を最優先に考える
- チーム開発を意識した一貫性のあるコード

## 命名規則

### 変数・メソッド名
- **キャメルケース**を使用
- 意味のある名前を付ける
```java
// 良い例
String userName = "田中太郎";
int loginAttemptCount = 0;
boolean isAuthenticated = false;

// 悪い例
String str = "田中太郎";
int cnt = 0;
boolean flag = false;
```

### 定数
- **アッパースネークケース**を使用
- `static final`で宣言
```java
public static final int MAX_LOGIN_ATTEMPTS = 5;
public static final String SESSION_TIMEOUT = "30";
public static final String DEFAULT_ROLE = "ROLE_USER";
```

## コーディングスタイル

### 1. 環境変数の一元管理
- `application.properties`または専用の設定クラスで管理
- `@Value`アノテーションまたは`@ConfigurationProperties`を使用
```java
@Value("${app.security.pepper}")
private String pepper;

@Value("${app.jwt.secret}")
private String jwtSecret;
```

### 2. ハードコーディングを避ける
- マジックナンバーや文字列は定数化
```java
// 悪い例
if (failedAttempts > 5) {
    lockAccount();
}

// 良い例
if (failedAttempts > MAX_LOGIN_ATTEMPTS) {
    lockAccount();
}
```

### 3. CDNの利用
- 外部ライブラリ（Bootstrap、jQuery等）はCDN経由で読み込む
```html
<!-- Bootstrap CSS -->
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">

<!-- jQuery -->
<script src="https://cdn.jsdelivr.net/npm/jquery@3.7.0/dist/jquery.min.js"></script>
```

### 4. 相対パスの利用
- リソース参照は相対パスを使用
- Thymeleafの`@{}`記法を活用
```html
<!-- 良い例 -->
<link rel="stylesheet" th:href="@{/css/style.css}">
<script th:src="@{/js/app.js}"></script>

<!-- 悪い例 -->
<link rel="stylesheet" href="/LoginDemo/css/style.css">
```

### 5. ネストを浅くする
- 早期リターン、ガード節を活用
```java
// 悪い例
public void processLogin(String username, String password) {
    if (username != null) {
        if (password != null) {
            if (isValidUser(username, password)) {
                // 処理
            }
        }
    }
}

// 良い例
public void processLogin(String username, String password) {
    if (username == null || password == null) {
        return;
    }
    
    if (!isValidUser(username, password)) {
        return;
    }
    
    // 処理
}
```

### 6. 可読性の向上
- 適切な変数名とメソッド分割
- 1メソッド1責任の原則
```java
// 悪い例
public void process(User u) {
    // 100行のコード
}

// 良い例
public void processUserRegistration(User user) {
    validateUserInput(user);
    encryptPassword(user);
    saveToDatabase(user);
    sendWelcomeEmail(user);
}
```

### 7. 拡張for文の使用
- 可能な限り拡張for文を使用
```java
// 通常のfor文（避ける）
for (int i = 0; i < users.size(); i++) {
    User user = users.get(i);
    process(user);
}

// 拡張for文（推奨）
for (User user : users) {
    process(user);
}
```

### 8. 情報隠蔽
- privateメソッド、カプセル化を徹底
- 必要最小限のアクセス修飾子を使用
```java
public class UserService {
    // フィールドはprivate
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    // 外部に公開する必要があるメソッドのみpublic
    public User registerUser(UserDto userDto) {
        validateUserDto(userDto);
        return saveUser(userDto);
    }
    
    // 内部処理はprivate
    private void validateUserDto(UserDto userDto) {
        // バリデーション処理
    }
    
    private User saveUser(UserDto userDto) {
        // 保存処理
    }
}
```

## セキュリティ

### 1. 機密情報の管理
- パスワード、APIキー、秘密鍵は環境変数で管理
- 絶対にソースコードにハードコーディングしない
```properties
# application.properties
app.security.pepper=${SECURITY_PEPPER}
app.jwt.secret=${JWT_SECRET}
app.oauth.client-secret=${OAUTH_CLIENT_SECRET}
```

### 2. SQLインジェクション対策
- プリペアドステートメントまたはJPAクエリメソッドを使用
```java
// 悪い例
String query = "SELECT * FROM users WHERE username = '" + username + "'";

// 良い例（JPA）
User findByUsername(String username);

// 良い例（JPQL）
@Query("SELECT u FROM User u WHERE u.username = :username")
User findByUsername(@Param("username") String username);
```

### 3. バリデーション
- 入力値は必ず検証する
- Bean Validationを活用
```java
@NotBlank(message = "ユーザー名は必須です")
@Size(min = 3, max = 20, message = "ユーザー名は3文字以上20文字以下で入力してください")
private String username;

@NotBlank(message = "パスワードは必須です")
@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
         message = "パスワードは8文字以上で、英字、数字、特殊文字を含む必要があります")
private String password;
```

### 4. セキュリティヘッダー
- 適切なセキュリティヘッダーを設定
```java
@Configuration
public class SecurityHeaderConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
            .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
            .frameOptions(frame -> frame.sameOrigin())
            .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
            .contentTypeOptions(Customizer.withDefaults())
        );
        return http.build();
    }
}
```

## エラーハンドリング

### 1. 例外処理
- 適切な例外処理とログ出力
```java
try {
    // 処理
} catch (SpecificException e) {
    // 具体的な例外を先にキャッチ
    log.error("特定のエラーが発生しました: {}", e.getMessage());
    throw new BusinessException("処理に失敗しました", e);
} catch (Exception e) {
    // 汎用的な例外は最後に
    log.error("予期しないエラーが発生しました", e);
    throw new SystemException("システムエラーが発生しました", e);
}
```

### 2. エラーメッセージ
- ユーザーには詳細なエラー情報を表示しない
- ログには詳細情報を記録
```java
// ユーザー向けメッセージ
model.addAttribute("error", "ログインに失敗しました");

// ログ
log.warn("ログイン失敗: ユーザー名={}, IPアドレス={}", username, ipAddress);
```

## テスト

### 1. テストコードも同じ規約に従う
- テストメソッド名は日本語も可
```java
@Test
void ユーザー登録が正常に完了すること() {
    // テストコード
}

@Test
void パスワードが要件を満たさない場合は例外が発生すること() {
    // テストコード
}
```

### 2. テストデータ
- テスト用の定数を定義
```java
private static final String TEST_USERNAME = "testuser";
private static final String TEST_PASSWORD = "Test@1234";
private static final String TEST_EMAIL = "test@example.com";
```