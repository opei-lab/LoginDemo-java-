# Spring Security 認証状態管理の仕組み

## 🔍 authenticated() の判定ロジック

### 認証情報の保存場所

```java
// Spring Securityは認証情報をここに保存
SecurityContextHolder
    └─ SecurityContext
        └─ Authentication
            ├─ Principal (ユーザー情報)
            ├─ Credentials (パスワード等)
            └─ Authorities (権限)

// 実際のデータ保存場所
HTTPSession
    └─ "SPRING_SECURITY_CONTEXT" 属性
        └─ SecurityContext オブジェクト
```

### 認証判定の流れ

```java
// SecurityFilterChainの内部動作
public class SecurityContextPersistenceFilter extends GenericFilterBean {
    
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpSession session = request.getSession(false);
        
        // 1. セッションから認証情報を復元
        if (session != null) {
            SecurityContext context = (SecurityContext) 
                session.getAttribute("SPRING_SECURITY_CONTEXT");
            if (context != null) {
                SecurityContextHolder.setContext(context);
            }
        }
        
        // 2. リクエスト処理
        chain.doFilter(request, response);
        
        // 3. 認証情報をセッションに保存
        SecurityContext context = SecurityContextHolder.getContext();
        session.setAttribute("SPRING_SECURITY_CONTEXT", context);
    }
}
```

## 📐 URL設計パターン

### 1. 機能別グループ化

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authz -> authz
            // === 認証不要エリア ===
            .requestMatchers(
                "/",            // トップページ
                "/login",       // ログイン
                "/register",    // 新規登録
                "/css/**",      // 静的リソース
                "/js/**",
                "/images/**"
            ).permitAll()
            
            // === 管理者エリア ===
            .requestMatchers("/admin/**").hasRole("ADMIN")
            
            // === ユーザー設定エリア ===
            .requestMatchers(
                "/settings/**",
                "/profile/**"
            ).authenticated()
            
            // === API エリア ===
            .requestMatchers("/api/public/**").permitAll()
            .requestMatchers("/api/**").authenticated()
            
            // === その他すべて ===
            .anyRequest().authenticated()
        );
        
        return http.build();
    }
}
```

### 2. RESTful設計での分離

```java
// 公開API用設定
@Configuration
@Order(1)
public class ApiSecurityConfig {
    
    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")  // /api/** のみに適用
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // JWT用
            )
            .httpBasic();  // Basic認証
            
        return http.build();
    }
}

// Web画面用設定
@Configuration
@Order(2)
public class WebSecurityConfig {
    
    @Bean
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/login", "/register").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
            );
            
        return http.build();
    }
}
```

## 🎯 認証状態の確認方法

### 1. コントローラーでの確認

```java
@Controller
public class HomeController {
    
    // 方法1: @AuthenticationPrincipal
    @GetMapping("/home")
    public String home(@AuthenticationPrincipal UserDetails user, Model model) {
        if (user != null) {
            model.addAttribute("username", user.getUsername());
        }
        return "home";
    }
    
    // 方法2: SecurityContextHolder
    @GetMapping("/profile")
    public String profile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            model.addAttribute("user", auth.getPrincipal());
        }
        return "profile";
    }
    
    // 方法3: Principal
    @GetMapping("/settings")
    public String settings(Principal principal, Model model) {
        if (principal != null) {
            model.addAttribute("username", principal.getName());
        }
        return "settings";
    }
}
```

### 2. Thymeleafでの確認

```html
<!-- 認証状態で表示制御 -->
<div sec:authorize="isAuthenticated()">
    ログイン中: <span sec:authentication="name"></span>
</div>

<div sec:authorize="!isAuthenticated()">
    <a href="/login">ログインしてください</a>
</div>

<!-- 権限で表示制御 -->
<div sec:authorize="hasRole('ADMIN')">
    <a href="/admin">管理画面</a>
</div>
```

## 🔐 セッション管理の詳細

### セッション作成タイミング

```java
// ログイン成功時の内部処理
public class UsernamePasswordAuthenticationFilter {
    
    protected void successfulAuthentication(HttpServletRequest request,
                                          HttpServletResponse response,
                                          FilterChain chain,
                                          Authentication authResult) {
        // 1. SecurityContextに認証情報を設定
        SecurityContextHolder.getContext().setAuthentication(authResult);
        
        // 2. セッションに保存（ここで判定フラグが立つ）
        HttpSession session = request.getSession(true);  // セッション作成
        session.setAttribute("SPRING_SECURITY_CONTEXT", 
                           SecurityContextHolder.getContext());
        
        // 3. 成功ハンドラーを実行
        successHandler.onAuthenticationSuccess(request, response, authResult);
    }
}
```

### セッション設定

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)  // 同時ログイン数制限
                .maxSessionsPreventsLogin(true)  // 新しいログインを拒否
                .sessionRegistry(sessionRegistry())
                .and()
                .sessionFixation().migrateSession()  // セッション固定攻撃対策
                .invalidSessionUrl("/login?expired")
            );
            
        return http.build();
    }
}
```

## 📊 URL設計のベストプラクティス

### 1. 階層的な設計

```
/                      # 公開エリア
├── /login            # 認証不要
├── /register         # 認証不要
├── /about            # 認証不要
│
├── /user/            # 認証必要（一般ユーザー）
│   ├── /home
│   ├── /profile
│   └── /settings
│
├── /admin/           # 認証必要（管理者のみ）
│   ├── /dashboard
│   └── /users
│
└── /api/             # API（別設定）
    ├── /public/      # 認証不要
    └── /v1/          # 認証必要
```

### 2. 機能単位での分離例

```java
// ユーザー管理機能
@Configuration
@EnableWebSecurity
public class ModularSecurityConfig {
    
    // 認証機能
    @Bean
    @Order(1)
    public SecurityFilterChain authFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/auth/**")
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/auth/login", "/auth/register").permitAll()
                .requestMatchers("/auth/logout").authenticated()
            );
        return http.build();
    }
    
    // MFA機能
    @Bean
    @Order(2)
    public SecurityFilterChain mfaFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/mfa/**")
            .authorizeHttpRequests(authz -> authz
                .anyRequest().authenticated()
            );
        return http.build();
    }
    
    // デフォルト設定
    @Bean
    @Order(3)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/css/**", "/js/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

## 💡 認証状態のデバッグ

```java
@Component
@Slf4j
public class AuthenticationDebugger {
    
    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        log.info("認証成功: {}", event.getAuthentication().getName());
        log.info("セッションID: {}", RequestContextHolder.currentRequestAttributes()
            .getSessionId());
    }
    
    @GetMapping("/debug/auth")
    @ResponseBody
    public Map<String, Object> debugAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Map.of(
            "authenticated", auth != null && auth.isAuthenticated(),
            "username", auth != null ? auth.getName() : "anonymous",
            "authorities", auth != null ? auth.getAuthorities() : Collections.emptyList(),
            "sessionId", RequestContextHolder.currentRequestAttributes().getSessionId()
        );
    }
}
```

## まとめ

1. **authenticated()の判定** = セッション内の`SPRING_SECURITY_CONTEXT`の存在確認
2. **URL設計** = 機能単位でグループ化し、SecurityFilterChainで制御
3. **分離可能な単位** = securityMatcher()で特定URLパターンごとに独立した設定が可能