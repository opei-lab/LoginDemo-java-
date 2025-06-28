# アノテーションによる接続関係

## 🔌 どうやってクラスが繋がるのか

### 基本原理：DIコンテナ

```
起動時：
1. @SpringBootApplication がすべてをスキャン
2. @Controller, @Service, @Repository を見つける
3. インスタンスを作成してDIコンテナに登録
4. @RequiredArgsConstructor や @Autowired で自動接続
```

## 📐 実際の接続例

### ユーザー登録の流れで見る接続

```java
// ステップ1: Controller（入り口）
@Controller
@RequiredArgsConstructor  // ← これが接続の鍵！
public class AuthController {
    // ↓ 自動的に注入される
    private final IUserService userService;
    private final FormTokenService formTokenService;
    
    @PostMapping("/register")
    public String register(@ModelAttribute User user) {
        // ↓ Service層のメソッドを呼ぶ
        userService.register(user);
        return "redirect:/login";
    }
}

// ステップ2: Service（ビジネスロジック）
@Service  // ← DIコンテナに登録
@RequiredArgsConstructor  // ← 依存性を注入
public class UserServiceImpl implements IUserService {
    // ↓ 自動的に注入される
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordHistoryService passwordHistoryService;
    
    @Override
    @Transactional  // ← トランザクション境界
    public User register(User user) {
        // ↓ Repository層のメソッドを呼ぶ
        return userRepository.save(user);
    }
}

// ステップ3: Repository（データアクセス）
@Repository  // ← DIコンテナに登録
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data JPAが実装を自動生成
    Optional<User> findByUsername(String username);
}
```

## 🗂️ LoginDemoの全接続マップ

### AuthController の接続

```
AuthController
├─ @RequiredArgsConstructor で自動注入
├─ IUserService userService → UserServiceImpl
├─ UserRepository userRepository → Spring Data JPA
└─ FormTokenService formTokenService → FormTokenService

メソッドと画面の対応：
├─ GET  /login     → login.html
├─ GET  /register  → register.html  
├─ POST /register  → userService.register() → redirect:/login
└─ GET  /home      → home.html
```

### UserServiceImpl の接続

```
UserServiceImpl
├─ implements IUserService, UserDetailsService
├─ @RequiredArgsConstructor で自動注入
├─ UserRepository userRepository
├─ PasswordEncoder passwordEncoder → BCryptPasswordEncoder
├─ LoginAttemptRepository loginAttemptRepository
├─ PasswordHistoryService passwordHistoryService
├─ AuditLogService auditLogService
└─ RateLimitService rateLimitService

主要メソッド：
├─ register() → 新規登録
├─ loadUserByUsername() → Spring Security用
├─ changePassword() → パスワード変更
└─ recordLoginAttempt() → ログイン試行記録
```

### SecurityConfig の設定接続

```
SecurityConfig
├─ SecurityFilterChain → URL毎のアクセス制御
│   ├─ /login, /register → permitAll()
│   ├─ /admin/** → hasRole("ADMIN")  
│   └─ その他 → authenticated()
├─ PasswordEncoder → BCryptPasswordEncoder
└─ UserDetailsService → UserServiceImpl
```

## 🎨 アノテーションの種類と役割

### クラスレベル（何者かを宣言）

```java
// Spring Core
@Component      // 汎用コンポーネント
@Service        // ビジネスロジック層
@Repository     // データアクセス層  
@Controller     // Web層（HTML返却）
@RestController // Web層（JSON返却）
@Configuration  // 設定クラス

// Lombok
@Data           // getter/setter等を生成
@Slf4j          // logフィールドを生成
@RequiredArgsConstructor  // finalフィールドのコンストラクタ生成

// JPA
@Entity         // DBテーブルと対応
@Table          // テーブル名指定
```

### メソッドレベル（動作を指定）

```java
// Web
@GetMapping     // GET リクエスト
@PostMapping    // POST リクエスト
@RequestMapping // 汎用マッピング

// トランザクション
@Transactional  // DB操作をまとめる

// Bean定義
@Bean           // メソッドの戻り値をDIコンテナに登録
```

### フィールド/パラメータレベル

```java
// 依存性注入
@Autowired      // フィールド注入（非推奨）
@Value          // プロパティ値注入

// Web
@RequestParam   // URLパラメータ
@PathVariable   // URLパス変数
@ModelAttribute // フォームデータ
@RequestBody    // JSONボディ

// JPA
@Id             // 主キー
@Column         // カラム定義
@GeneratedValue // 自動生成
```

## 📍 接続の追跡方法

### 1. IntelliJでの追跡

```
Ctrl + クリック → 定義元へジャンプ
Ctrl + Alt + H → 呼び出し階層
Ctrl + Shift + F → プロジェクト全体検索
```

### 2. クラス図で可視化

```
AuthController
    ↓ uses
IUserService ← implements ← UserServiceImpl
                                ↓ uses
                            UserRepository
                                ↓ extends
                            JpaRepository<User, Long>
```

### 3. 実行時の確認

```java
@PostConstruct  // 起動時に実行
public void checkBeans() {
    log.info("UserService実装: {}", userService.getClass().getName());
    // 出力: UserService実装: com.example.demo.service.impl.UserServiceImpl
}
```

## 🚀 新機能追加時のチェックリスト

```
□ Entityを作成した → @Entity付けた？
□ Repositoryを作成した → @Repository付けた？ 
□ Serviceを作成した → @Service付けた？
□ Service実装でRepositoryを使う → @RequiredArgsConstructor付けた？
□ ControllerでServiceを使う → @RequiredArgsConstructor付けた？
□ 画面表示したい → @GetMapping付けた？
□ フォーム処理したい → @PostMapping付けた？
□ DB操作する → @Transactional付けた？
```

これで「どこで何が繋がっているか」が明確になったはずです！