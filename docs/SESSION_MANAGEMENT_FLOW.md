# セッション管理の責任分担

## 私たちのコード vs Spring Security

### 1. UserServiceImpl（私たちのコード）

```java
@Override
public UserDetails loadUserByUsername(String username) {
    // DBからユーザー取得
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("..."));
    
    // Spring Security用のUserDetailsを返すだけ
    return new org.springframework.security.core.userdetails.User(
        user.getUsername(),
        user.getPassword(),
        Collections.singletonList(() -> "ROLE_USER")
    );
}
```

**役割**: ユーザー情報を提供するだけ（セッション操作なし）

### 2. Spring Securityの自動処理

```
1. POST /login 受信
    ↓
2. UsernamePasswordAuthenticationFilter（Spring）
    ↓
3. AuthenticationManager（Spring）
    ↓
4. UserServiceImpl.loadUserByUsername()（私たち）← ここだけ！
    ↓
5. PasswordEncoder.matches()（Spring）
    ↓
6. 認証成功 → セッション保存（Spring）← 自動！
```

## 明示的なセッション操作箇所

### 1. FormTokenService（CSRF対策）

```java
@Service
public class FormTokenService {
    public String generateToken(HttpSession session, String formId) {
        String token = UUID.randomUUID().toString();
        session.setAttribute("form_token_" + formId, token);
        return token;
    }
}
```

### 2. OAuth2AuthenticationSuccessHandler（追加検証）

```java
@Component
public class OAuth2AuthenticationSuccessHandler {
    public void onAuthenticationSuccess(...) {
        // リスク評価結果を保存
        request.getSession().setAttribute("requiresAdditionalVerification", true);
        request.getSession().setAttribute("riskAssessmentResult", riskResult);
    }
}
```

### 3. MfaAuthenticationFilter（MFA状態）

```java
@Component
public class MfaAuthenticationFilter extends OncePerRequestFilter {
    protected void doFilterInternal(...) {
        Boolean mfaVerified = (Boolean) 
            request.getSession().getAttribute("MFA_VERIFIED");
    }
}
```

## セッション内容の全体像

```
HTTPSession {
    // Spring Securityが自動保存
    "SPRING_SECURITY_CONTEXT": {
        authentication: {
            principal: UserDetails,
            authenticated: true,
            authorities: ["ROLE_USER"]
        }
    },
    
    // 私たちが明示的に保存
    "form_token_register": "uuid-xxxxx",
    "requiresAdditionalVerification": true,
    "MFA_VERIFIED": true,
    "riskAssessmentResult": { ... }
}
```

## デフォルト設定の確認

### SecurityConfigで変更可能な部分

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http
        // セッション管理をカスタマイズしたい場合
        .sessionManagement(session -> session
            // セッション作成ポリシー
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            
            // 同時セッション数制限
            .maximumSessions(1)
            
            // セッション固定攻撃対策
            .sessionFixation().migrateSession()
            
            // セッションタイムアウト時のURL
            .invalidSessionUrl("/login?expired")
        );
}
```

現在のLoginDemoではこれらの設定を省略し、デフォルトに任せています。

## なぜSpring Securityに任せるのか

### メリット

1. **セキュリティのベストプラクティス**
   - セッション固定攻撃対策
   - 適切なCookie設定
   - セキュアな保存方法

2. **統一された管理**
   - すべてのエンドポイントで一貫した認証
   - フィルターチェーンで自動処理

3. **カスタマイズの柔軟性**
   - 必要に応じて設定変更可能
   - JWT認証への切り替えも容易

### 私たちがやるべきこと

```java
// ✅ やること
- UserDetailsの提供
- ビジネスロジック（ロック、履歴等）
- アプリ固有の情報管理

// ❌ やらないこと
- 認証情報のセッション保存
- セッションIDの生成
- Cookieの管理
```

## セッション保存のタイミング詳細

```
1. ユーザーがログインフォームを送信
   POST /login (username=user1, password=pass123)

2. Spring Securityのフィルターが受信
   UsernamePasswordAuthenticationFilter

3. 認証処理
   AuthenticationManager → UserServiceImpl.loadUserByUsername()

4. パスワード照合
   PasswordEncoder.matches(入力, DB値)

5. 認証成功 ← ここでセッション保存！
   SecurityContext → HTTPSession
   
6. レスポンス
   Set-Cookie: JSESSIONID=xxxxx
   Location: /home
```

## まとめ

- **認証情報のセッション保存**: Spring Securityが自動処理
- **私たちのコード**: UserDetailsを返すだけ
- **明示的なセッション操作**: アプリ固有の情報のみ

この分離により、セキュリティのベストプラクティスが保証され、
私たちはビジネスロジックに集中できます。