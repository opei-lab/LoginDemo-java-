# URL設計とセキュリティ分離パターン

## 🏗️ URL設計の基本原則

### 1. RESTful設計
```
/users          GET     # 一覧表示
/users/{id}     GET     # 詳細表示
/users/new      GET     # 新規作成フォーム
/users          POST    # 作成実行
/users/{id}/edit GET    # 編集フォーム
/users/{id}     PUT     # 更新実行
/users/{id}     DELETE  # 削除実行
```

### 2. 機能別階層設計
```
/auth/          # 認証関連
├── login
├── logout
└── register

/user/          # ユーザー機能
├── profile
├── settings
└── preferences

/admin/         # 管理機能
├── dashboard
├── users
└── logs
```

## 🔒 セキュリティ設定の分離パターン

### パターン1: 単一設定（小規模アプリ）

```java
@Configuration
public class SimpleSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authz -> authz
            // 公開エリア
            .requestMatchers("/", "/login", "/register", "/css/**").permitAll()
            // 管理者エリア
            .requestMatchers("/admin/**").hasRole("ADMIN")
            // その他は認証必要
            .anyRequest().authenticated()
        );
        
        return http.build();
    }
}
```

### パターン2: 機能別分離（中規模アプリ）

```java
@Configuration
@EnableWebSecurity
public class ModularSecurityConfig {
    
    // 公開API用
    @Bean
    @Order(1)
    public SecurityFilterChain publicApiChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/public/**")
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()
            )
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            
        return http.build();
    }
    
    // 認証API用
    @Bean
    @Order(2)
    public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .authorizeHttpRequests(authz -> authz
                .anyRequest().authenticated()
            )
            .httpBasic()
            .and()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            
        return http.build();
    }
    
    // Web画面用
    @Bean
    @Order(3)
    public SecurityFilterChain webChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/login", "/register").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/home")
            );
            
        return http.build();
    }
}
```

### パターン3: マイクロサービス風分離（大規模アプリ）

```java
// 認証サービス設定
@Configuration
@ConditionalOnProperty(name = "app.module", havingValue = "auth")
public class AuthModuleConfig {
    
    @Bean
    public SecurityFilterChain authChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/auth/**")
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/auth/login", "/auth/register").permitAll()
                .requestMatchers("/auth/verify", "/auth/reset").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}

// ユーザーサービス設定
@Configuration
@ConditionalOnProperty(name = "app.module", havingValue = "user")
public class UserModuleConfig {
    
    @Bean
    public SecurityFilterChain userChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/user/**")
            .authorizeHttpRequests(authz -> authz
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

## 📋 実際のLoginDemoでの実装

### 現在のURL構成

```
認証関連:
/login          [GET/POST]  # ログイン
/logout         [POST]      # ログアウト
/register       [GET/POST]  # 新規登録

ユーザー機能:
/home           [GET]       # ホーム画面
/change-password [GET/POST] # パスワード変更

MFA関連:
/mfa/setup      [GET/POST]  # MFA設定
/mfa/verify     [GET/POST]  # MFA検証
/mfa/manage     [GET]       # MFA管理

OAuth2関連:
/oauth2/authorize/{provider} [GET]  # OAuth開始
/login/oauth2/code/{provider} [GET] # OAuthコールバック
```

### セキュリティ設定の実装

```java
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // 認証不要
                .requestMatchers(
                    "/login", 
                    "/register", 
                    "/css/**", 
                    "/js/**"
                ).permitAll()
                
                // OAuth2関連
                .requestMatchers(
                    "/oauth2/**", 
                    "/login/oauth2/**"
                ).permitAll()
                
                // 追加認証
                .requestMatchers("/auth/**").permitAll()
                
                // MFA（認証後）
                .requestMatchers("/mfa/verify").authenticated()
                
                // パスワード変更（認証後）
                .requestMatchers("/change-password").authenticated()
                
                // その他はすべて認証必要
                .anyRequest().authenticated()
            );
            
        return http.build();
    }
}
```

## 🎯 分離の判断基準

### いつ分離すべきか

1. **異なる認証方式が必要な場合**
   - Web画面: セッションベース認証
   - API: トークンベース認証

2. **異なるセキュリティ要件がある場合**
   - 公開API: CSRF無効、レート制限
   - 管理画面: IP制限、2FA必須

3. **独立したデプロイが必要な場合**
   - 各モジュールを別サーバーに配置

### 分離のメリット・デメリット

| メリット | デメリット |
|---------|-----------|
| 設定の明確化 | 設定ファイルの増加 |
| 独立したテスト | 複雑性の増加 |
| 柔軟な拡張 | 初期設定の手間 |

## 💡 実装のコツ

### 1. URLパターンの統一

```java
// Good: 一貫性のあるパターン
/api/v1/users
/api/v1/products
/api/v1/orders

// Bad: バラバラなパターン
/users
/api/products
/v1/orders
```

### 2. 認証不要URLの明示

```java
private static final String[] PUBLIC_URLS = {
    "/",
    "/login",
    "/register",
    "/api/public/**",
    "/css/**",
    "/js/**",
    "/images/**"
};

// 使用
.requestMatchers(PUBLIC_URLS).permitAll()
```

### 3. テスト可能な設計

```java
@TestConfiguration
public class TestSecurityConfig {
    
    @Bean
    @Primary
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        // テスト用の簡略化された設定
        http.authorizeHttpRequests(authz -> authz
            .anyRequest().permitAll()
        );
        return http.build();
    }
}
```