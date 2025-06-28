# Spring Boot チートシート（初学者向け）

## 🎯 最重要：理解すべき5つの概念

### 1. DIコンテナ（依存性注入）
```java
// Springが自動的にインスタンスを作って注入してくれる
@Service
public class UserService {
    // ❌ 自分でnewしない
    // private UserRepository repo = new UserRepository();
    
    // ✅ Springが注入
    private final UserRepository repo;
    
    // Lombokの@RequiredArgsConstructorが
    // このコンストラクタを自動生成
    public UserService(UserRepository repo) {
        this.repo = repo;
    }
}
```

### 2. レイヤー構造
```
リクエスト
    ↓
@Controller     // 1. Webリクエストを受け取る
    ↓
@Service        // 2. ビジネスロジック実行
    ↓
@Repository     // 3. データベース操作
    ↓
Database
```

### 3. アノテーションの役割
```java
// クラスレベル：このクラスの役割を宣言
@Controller     → Webリクエスト処理係
@Service        → ビジネスロジック係
@Repository     → データベース操作係
@Component      → その他の部品
@Configuration  → 設定係

// メソッドレベル：このメソッドの役割を宣言
@GetMapping     → GETリクエスト受付
@PostMapping    → POSTリクエスト受付
@Transactional  → DB操作をまとめて実行
```

### 4. Lombokは「省略記法」
```java
// Lombokなし（30行）
public class User {
    private String name;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return "User{name='" + name + "'}";
    }
    // ... equals, hashCode なども必要
}

// Lombokあり（3行）
@Data
public class User {
    private String name;
}
```

### 5. 設定より規約
```properties
# application.properties
spring.datasource.url=jdbc:h2:mem:testdb

# ↑これだけで：
# - H2データベース自動設定
# - テーブル自動作成
# - コネクションプール設定
# すべて自動！
```

## 📁 最小限のファイル構成

```
src/main/java/com/example/demo/
├── LoginDemoApplication.java      // 起動クラス
├── controller/
│   └── AuthController.java       // Web画面
├── service/
│   └── UserService.java          // ロジック
├── repository/
│   └── UserRepository.java       // DB操作
├── entity/
│   └── User.java                 // テーブル定義
└── config/
    └── SecurityConfig.java       // セキュリティ設定
```

## 🔍 コードの読み方

### 1. アノテーションから役割を推測
```java
@Controller                    // → Web画面担当
@RequiredArgsConstructor      // → コンストラクタ自動生成
@Slf4j                        // → log変数が使える
public class AuthController {
    private final UserService userService;  // 自動注入される
    
    @GetMapping("/login")     // → GET /login を処理
    public String login() {
        log.info("ログイン画面表示");  // @Slf4jのおかげ
        return "login";  // → templates/login.html
    }
}
```

### 2. 処理の流れを追う
```
1. ブラウザ: GET /login
2. SecurityConfig: 認証不要と判定
3. AuthController: login()メソッド実行
4. Thymeleaf: login.html をレンダリング
5. ブラウザ: HTMLを表示
```

## 🛠️ よく使うパターン

### 画面表示（Controller → HTML）
```java
@GetMapping("/users")
public String list(Model model) {
    model.addAttribute("users", userService.findAll());
    return "user-list";  // user-list.html
}
```

### API作成（RestController → JSON）
```java
@RestController
@RequestMapping("/api")
public class ApiController {
    @GetMapping("/users")
    public List<User> getUsers() {
        return userService.findAll();  // 自動的にJSON化
    }
}
```

### フォーム処理
```java
@PostMapping("/register")
public String register(@ModelAttribute User user,
                      BindingResult result) {
    if (result.hasErrors()) {
        return "register";  // エラー時は同じ画面
    }
    userService.save(user);
    return "redirect:/login";  // 成功時はリダイレクト
}
```

## 🚨 よくある勘違い

### 1. すべて理解する必要はない
```java
// 最初はこれだけ理解すればOK
@Service  // Springが管理するクラス
@Transactional  // DB操作をまとめる
```

### 2. アノテーションは「宣言」
```java
@Service  // 「これはサービスクラスです」と宣言
// Springが勝手に必要な処理をしてくれる
```

### 3. Lombokは必須ではない
```java
// Lombokなしでも動く（ただし記述量が増える）
// 慣れたら使えばOK
```

## 📚 段階的学習プラン

### Level 1: 基本を理解
- [ ] @Controller と @GetMapping で画面表示
- [ ] Model で値を渡す
- [ ] Thymeleaf で表示

### Level 2: CRUD操作
- [ ] @Service でビジネスロジック
- [ ] JpaRepository で DB操作
- [ ] @Transactional の理解

### Level 3: セキュリティ
- [ ] Spring Security の基本
- [ ] ログイン機能
- [ ] アクセス制御

### Level 4: 応用
- [ ] RESTful API
- [ ] 例外処理
- [ ] テスト作成

## 🔗 依存関係の理解

```
Application.java
    ↓ 起動
Spring Boot
    ↓ 自動設定
DIコンテナ
    ↓ インスタンス作成
Controller → Service → Repository
    ↓         ↓         ↓
  @Autowired で自動的に接続
```

これで Spring Boot の全体像が掴めるはずです！