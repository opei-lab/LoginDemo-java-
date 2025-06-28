# Spring Boot コア概念の理解

## 1. @Controller vs @RestController

### @Controller（View を返す）
```java
@Controller
public class WebController {
    @GetMapping("/hello")
    public String hello(Model model) {
        model.addAttribute("message", "こんにちは");
        return "hello";  // → templates/hello.html を表示
    }
}
```
**特徴**：
- HTMLページ（View）を返す
- Thymeleafなどのテンプレートエンジンと連携
- 従来のWebアプリケーション向け

### @RestController（データを返す）
```java
@RestController  // = @Controller + @ResponseBody
public class ApiController {
    @GetMapping("/api/hello")
    public Map<String, String> hello() {
        return Map.of("message", "こんにちは");  // → JSON: {"message": "こんにちは"}
    }
}
```
**特徴**：
- JSONやXMLなどのデータを返す
- RESTful API向け
- フロントエンドとバックエンドが分離したSPA向け

### 使い分けの例
```java
// 画面遷移のある従来型Webアプリ
@Controller
public class UserWebController {
    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        return "user-list";  // user-list.html
    }
}

// React/Vue.jsなどのSPA向けAPI
@RestController
@RequestMapping("/api")
public class UserApiController {
    @GetMapping("/users")
    public List<UserDto> getUsers() {
        return userService.findAll();  // JSON配列
    }
}
```

## 2. Service層を使う理由

### 悪い例（Service層なし）
```java
@Controller
public class BadController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmailService emailService;
    
    @PostMapping("/register")
    public String register(User user) {
        // コントローラーにビジネスロジックが混在😱
        if (userRepository.existsByUsername(user.getUsername())) {
            return "error";
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        emailService.sendWelcomeMail(user.getEmail());
        return "success";
    }
}
```

### 良い例（Service層あり）
```java
@Controller
public class GoodController {
    @Autowired
    private UserService userService;  // 依存は1つだけ
    
    @PostMapping("/register")
    public String register(User user) {
        userService.register(user);  // シンプル！
        return "success";
    }
}

@Service
@Transactional
public class UserService {
    // ビジネスロジックを集約
    public void register(User user) {
        validateUsername(user);
        encodePassword(user);
        save(user);
        sendWelcomeMail(user);
    }
    
    // 再利用可能なメソッド
    public void changePassword(User user, String newPassword) {
        // 別のコントローラーからも使える
    }
}
```

**Service層のメリット**：
1. **ビジネスロジックの集約** - 複雑な処理を整理
2. **再利用性** - 複数のコントローラーから利用可能
3. **テストしやすさ** - ビジネスロジックを単独でテスト
4. **トランザクション境界** - Service層でトランザクション管理

## 3. @Transactional の動作

### 基本的な動作
```java
@Service
public class TransactionExample {
    
    @Transactional
    public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
        // トランザクション開始
        Account from = accountRepo.findById(fromId);
        Account to = accountRepo.findById(toId);
        
        from.withdraw(amount);  // 残高を減らす
        to.deposit(amount);     // 残高を増やす
        
        accountRepo.save(from);
        accountRepo.save(to);
        // メソッド終了時に自動コミット
    }
    // 例外発生時は自動ロールバック
}
```

### 詳細な制御
```java
@Service
public class UserService {
    
    // 読み取り専用（パフォーマンス向上）
    @Transactional(readOnly = true)
    public User findUser(Long id) {
        return userRepository.findById(id);
    }
    
    // 特定の例外でロールバック
    @Transactional(rollbackFor = BusinessException.class)
    public void updateUser(User user) throws BusinessException {
        if (!isValid(user)) {
            throw new BusinessException("Invalid data");  // ロールバック
        }
        userRepository.save(user);
    }
    
    // タイムアウト設定
    @Transactional(timeout = 5)  // 5秒でタイムアウト
    public void slowOperation() {
        // 時間のかかる処理
    }
}
```

**@Transactionalの効果**：
1. **自動コミット/ロールバック**
2. **複数のDB操作をまとめて実行**
3. **データの整合性保証**
4. **例外時の自動ロールバック**

## 4. SecurityFilterChain の役割

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. 認証不要なページを設定
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/css/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()  // その他は認証必要
            )
            
            // 2. ログイン設定
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/home")
                .failureUrl("/login?error")
            )
            
            // 3. ログアウト設定
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
            );
            
        return http.build();
    }
}
```

**SecurityFilterChainの役割**：
1. **アクセス制御** - どのURLに誰がアクセスできるか
2. **認証方式の設定** - フォーム認証、OAuth2など
3. **セキュリティヘッダー** - XSS、CSRF対策
4. **セッション管理** - 同時ログイン制限など

### フィルターチェーンの流れ
```
リクエスト → [認証フィルター] → [認可フィルター] → [CSRF フィルター] → Controller
                ↓                    ↓                    ↓
            認証チェック        権限チェック         トークンチェック
```

## 5. Thymeleafでの変数展開

### 基本的な変数展開
```html
<!-- Controller で model.addAttribute("user", user) -->

<!-- テキスト表示 -->
<p th:text="${user.name}">デフォルト値</p>
<!-- 出力: <p>田中太郎</p> -->

<!-- HTML表示（エスケープなし） -->
<div th:utext="${user.bio}">自己紹介</div>

<!-- 属性に使用 -->
<a th:href="@{/users/{id}(id=${user.id})}">プロフィール</a>
<!-- 出力: <a href="/users/123">プロフィール</a> -->
```

### 条件分岐
```html
<!-- if文 -->
<div th:if="${user.premium}">
    <span class="badge">プレミアム会員</span>
</div>

<!-- unless（if not） -->
<div th:unless="${user.emailVerified}">
    <a href="/verify-email">メールアドレスを確認してください</a>
</div>

<!-- switch文 -->
<div th:switch="${user.role}">
    <p th:case="'ADMIN'">管理者</p>
    <p th:case="'USER'">一般ユーザー</p>
    <p th:case="*">ゲスト</p>
</div>
```

### 繰り返し処理
```html
<!-- リスト表示 -->
<table>
    <tr th:each="user : ${users}">
        <td th:text="${user.id}">1</td>
        <td th:text="${user.name}">名前</td>
        <td th:text="${user.email}">メール</td>
    </tr>
</table>

<!-- インデックス付き -->
<ul>
    <li th:each="item, stat : ${items}">
        <span th:text="${stat.index}">0</span>: 
        <span th:text="${item.name}">アイテム名</span>
        <!-- stat.index, stat.count, stat.first, stat.last など使用可能 -->
    </li>
</ul>
```

### フォーム連携
```html
<form th:action="@{/register}" th:object="${user}" method="post">
    <!-- th:field で自動的にname, id, value属性を設定 -->
    <input type="text" th:field="*{username}" />
    <!-- エラー表示 -->
    <span th:if="${#fields.hasErrors('username')}" 
          th:errors="*{username}">エラー</span>
    
    <input type="email" th:field="*{email}" />
    <button type="submit">登録</button>
</form>
```

### セキュリティ連携
```html
<!-- 認証状態で表示制御 -->
<div sec:authorize="isAuthenticated()">
    ようこそ、<span sec:authentication="name">ユーザー名</span>さん
</div>

<div sec:authorize="hasRole('ADMIN')">
    <a href="/admin">管理画面</a>
</div>

<!-- ログイン/ログアウトボタンの出し分け -->
<div sec:authorize="!isAuthenticated()">
    <a href="/login">ログイン</a>
</div>
<div sec:authorize="isAuthenticated()">
    <form th:action="@{/logout}" method="post">
        <button type="submit">ログアウト</button>
    </form>
</div>
```

### ユーティリティオブジェクト
```html
<!-- 日付フォーマット -->
<span th:text="${#dates.format(user.createdAt, 'yyyy年MM月dd日')}">
    2024年1月1日
</span>

<!-- 数値フォーマット -->
<span th:text="${#numbers.formatDecimal(product.price, 0, 'COMMA', 0, 'POINT')}">
    1,234
</span>

<!-- 文字列操作 -->
<span th:text="${#strings.abbreviate(user.bio, 100)}">
    自己紹介の最初の100文字...
</span>
```

## まとめ

これらの概念を理解することで：
- **@Controller/@RestController** → 適切なAPIエンドポイント設計
- **Service層** → 保守性の高いアーキテクチャ
- **@Transactional** → データ整合性の保証
- **SecurityFilterChain** → セキュアなアクセス制御
- **Thymeleaf** → 動的なHTMLレンダリング

が可能になります。