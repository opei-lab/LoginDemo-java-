# Spring Boot 実装手順ガイド

## ログイン機能を例にした実装の流れ

### Step 1: 要件定義
```
1. ユーザーはユーザー名とパスワードでログインできる
2. パスワードは暗号化して保存する
3. ログイン失敗が5回続いたらアカウントをロックする
4. ログイン成功後はホーム画面に遷移する
```

### Step 2: データベース設計（Entity作成）

**User.java** を作成：
```java
@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    private boolean accountNonLocked = true;
}
```

→ **これだけでSpring Bootがテーブルを自動作成！**

### Step 3: Repository層の作成

**UserRepository.java**：
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

→ **実装不要！Spring Data JPAが自動実装**

### Step 4: Service層の作成

**IUserService.java**（インターフェース）：
```java
public interface IUserService extends UserDetailsService {
    User register(User user);
    void recordLoginAttempt(String username, boolean success);
}
```

**UserServiceImpl.java**（実装）：
```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません"));
            
        return org.springframework.security.core.userdetails.User
            .withUsername(user.getUsername())
            .password(user.getPassword())
            .accountLocked(!user.isAccountNonLocked())
            .build();
    }
}
```

### Step 5: Controller層の作成

**AuthController.java**：
```java
@Controller
@RequiredArgsConstructor
public class AuthController {
    
    @GetMapping("/login")
    public String loginPage() {
        return "login";  // src/main/resources/templates/login.html
    }
    
    @GetMapping("/home")
    public String homePage(Model model, Authentication auth) {
        model.addAttribute("username", auth.getName());
        return "home";
    }
}
```

### Step 6: View層の作成（Thymeleaf）

**login.html**：
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>ログイン</title>
</head>
<body>
    <form th:action="@{/login}" method="post">
        <input type="text" name="username" placeholder="ユーザー名" required>
        <input type="password" name="password" placeholder="パスワード" required>
        <button type="submit">ログイン</button>
    </form>
</body>
</html>
```

### Step 7: Security設定

**SecurityConfig.java**：
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/home", true)
            );
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

## 実装の順序（推奨）

1. **Entityを作る** → DBテーブル設計
2. **Repositoryを作る** → データアクセス
3. **Serviceを作る** → ビジネスロジック
4. **Controllerを作る** → HTTPエンドポイント
5. **Viewを作る** → 画面
6. **設定を調整** → Security等

## デバッグのコツ

### 1. H2コンソールでデータ確認
```
http://localhost:8080/h2-console
```

### 2. ログで処理を追跡
```java
@Slf4j  // Lombokアノテーション
public class UserServiceImpl {
    public User register(User user) {
        log.info("ユーザー登録開始: {}", user.getUsername());
        // 処理
        log.info("ユーザー登録完了: {}", user.getId());
    }
}
```

### 3. SQLログを見る
```properties
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
```

## よくあるエラーと対処法

### 1. "Table not found"
**原因**: Entityが認識されていない
**対処**: @EntityScanアノテーションを確認

### 2. "No qualifying bean"
**原因**: DIコンテナにBeanが登録されていない
**対処**: @Service, @Repository等のアノテーションを確認

### 3. "Failed to load ApplicationContext"
**原因**: 設定エラー
**対処**: application.propertiesとアノテーションを確認

## 理解度チェックリスト

- [ ] @Controller と @RestController の違いを説明できる
- [ ] Service層を使う理由を説明できる
- [ ] @Transactional の動作を理解している
- [ ] SecurityFilterChain の役割を理解している
- [ ] Thymeleafでの変数展開方法を知っている