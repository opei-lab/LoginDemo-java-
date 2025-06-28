# Spring Boot 処理フロー完全マップ

## 🗺️ ログイン処理の完全な流れ

### 1. ログイン画面表示（GET /login）

```
[ブラウザ] GET http://localhost:8080/login
    ↓
[SecurityFilterChain] 
    @Bean SecurityConfig.java
    .requestMatchers("/login").permitAll() ← 認証不要と判定
    ↓
[DispatcherServlet]
    @GetMapping("/login") を探す
    ↓
[AuthController]
    @Controller ← Springが管理
    @GetMapping("/login") ← このメソッドにマッチ！
    public String loginPage() {
        return "login"; ← テンプレート名
    }
    ↓
[ViewResolver]
    "login" → templates/login.html
    ↓
[Thymeleaf]
    HTMLをレンダリング
    ↓
[ブラウザ]
    ログイン画面表示
```

### 2. ログイン実行（POST /login）

```
[ブラウザ] POST /login (username=user1, password=pass123)
    ↓
[SecurityFilterChain]
    ↓
[UsernamePasswordAuthenticationFilter] ← Spring Security内部
    ↓
[AuthenticationManager]
    ↓
[UserServiceImpl] ← ここから私たちのコード！
    @Service
    public class UserServiceImpl implements UserDetailsService {
        
        @RequiredArgsConstructor ← Lombokがコンストラクタ生成
        ├─ private final UserRepository userRepository;
        └─ private final PasswordEncoder passwordEncoder;
        
        @Override
        public UserDetails loadUserByUsername(String username) {
            ↓
            [UserRepository]
            @Repository ← Spring Data JPAが実装を自動生成
            Optional<User> findByUsername(String username);
                ↓
                [Hibernate] ← JPAの実装
                SELECT * FROM users WHERE username = ?
                    ↓
                    [H2 Database]
                    データ取得
                    ↓
            User entity を返す
            ↓
        UserDetails に変換して返す
        }
    }
    ↓
[PasswordEncoder]
    入力パスワードとDBパスワードを比較
    ↓
[SecurityContext]
    認証情報を保存
    ↓
[AuthController] ← 成功時のリダイレクト先
    defaultSuccessUrl("/home", true)
```

## 🔗 アノテーションによる接続

### DIコンテナの動作

```java
// 1. 起動時スキャン
@SpringBootApplication
public class LoginDemoApplication {
    // @ComponentScan により、以下をすべてスキャン：
    // - @Controller
    // - @Service  
    // - @Repository
    // - @Component
    // - @Configuration
}

// 2. インスタンス作成と登録
@Service("userService")  // Bean名: userService
public class UserServiceImpl implements IUserService {
    // Springが new UserServiceImpl() を実行
    // DIコンテナに登録
}

// 3. 依存性の注入
@Controller
@RequiredArgsConstructor  // Lombokがコンストラクタ生成
public class AuthController {
    private final IUserService userService;
    // ↑ DIコンテナから自動注入
    
    /* Lombokが生成するコード：
    public AuthController(IUserService userService) {
        this.userService = userService;
    }
    */
}
```

## 📊 主要な処理パターン

### パターン1: 画面表示（Controller → View）

```
[リクエスト] GET /register
    ↓
@Controller
public class AuthController {
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "register";  // → templates/register.html
    }
}
```

### パターン2: フォーム送信（Controller → Service → Repository）

```
[リクエスト] POST /register
    ↓
@Controller AuthController
    @PostMapping("/register")
    ↓ userService.register(user)
@Service UserServiceImpl  
    @Transactional  // トランザクション開始
    ↓ userRepository.save(user)
@Repository UserRepository
    ↓ Hibernateが SQL生成
[Database] INSERT INTO users ...
```

### パターン3: 認証が必要なページ

```
[リクエスト] GET /home
    ↓
[SecurityFilterChain]
    .anyRequest().authenticated() ← 認証チェック
    ↓ 未認証なら
    redirect:/login
    ↓ 認証済みなら
@Controller AuthController
    @GetMapping("/home")
    public String home(@AuthenticationPrincipal UserDetails user) {
        // user には認証済みユーザー情報が自動注入
    }
```

## 🎯 重要な接続ポイント

### 1. URL → Controller メソッド

| URL | HTTPメソッド | Controllerメソッド |
|-----|------------|------------------|
| /login | GET | AuthController.loginPage() |
| /login | POST | Spring Securityが処理 |
| /register | GET | AuthController.registerForm() |
| /register | POST | AuthController.register() |
| /home | GET | AuthController.home() |

### 2. Service の依存関係

```
UserServiceImpl
├─ UserRepository (DB操作)
├─ PasswordEncoder (パスワード暗号化)
├─ LoginAttemptRepository (ログイン試行記録)
├─ PasswordHistoryService (パスワード履歴)
└─ AuditLogService (監査ログ)
```

### 3. Repository → テーブル

| Repository | Entity | テーブル名 |
|-----------|--------|----------|
| UserRepository | User | users |
| LoginAttemptRepository | LoginAttempt | login_attempts |
| AuditLogRepository | AuditLog | audit_logs |

## 🔍 デバッグ時の追跡方法

### 1. ログを仕込む

```java
@Slf4j  // Lombokでlogger生成
@Service
public class UserServiceImpl {
    
    public User register(User user) {
        log.info("=== register開始: {}", user.getUsername());
        
        // 処理
        
        log.info("=== register完了: ID={}", savedUser.getId());
        return savedUser;
    }
}
```

### 2. ブレークポイントを置く場所

```
1. Controller のメソッド開始
2. Service のビジネスロジック
3. Repository の呼び出し前後
```

### 3. 実行されるSQLを確認

```properties
# application.properties
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

## 📝 クイックリファレンス

### 新機能追加の手順

```
1. Entity作成 → テーブル設計
   @Entity
   public class NewFeature { }

2. Repository作成 → DB操作定義
   @Repository
   public interface NewFeatureRepository extends JpaRepository<NewFeature, Long> { }

3. Service作成 → ビジネスロジック
   @Service
   @RequiredArgsConstructor
   public class NewFeatureService { }

4. Controller作成 → エンドポイント
   @Controller
   @RequiredArgsConstructor
   public class NewFeatureController { }

5. View作成 → 画面
   templates/new-feature.html
```

### トラブルシューティング

| エラー | 原因 | 解決方法 |
|-------|------|---------|
| No qualifying bean | DIコンテナに登録されていない | @Service等のアノテーション確認 |
| Template not found | テンプレート名の誤り | return文とファイル名を確認 |
| Table not found | Entity認識されていない | @Entityと@TableScan確認 |