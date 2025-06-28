# Spring Security ログイン認証の真の仕組み

## ❌ よくある誤解

**誤解**: `.loginPage("/login")` で認証判断している
**誤解**: 画面遷移で認証状態を管理している

## ✅ 実際の仕組み

### 1. formLogin の設定は「どこで・どうやって」を指定するだけ

```java
.formLogin(form -> form
    .loginPage("/login")           // ログインフォームの場所
    .loginProcessingUrl("/login")  // POSTを受け付けるURL（デフォルト）
    .defaultSuccessUrl("/home", true)  // 成功後のリダイレクト先
    .failureUrl("/login?error")    // 失敗後のリダイレクト先
)
```

これらは単なる**設定**です。認証判断はしていません。

### 2. 実際の認証処理フロー

```
1. ユーザーが POST /login を送信
    ↓
2. UsernamePasswordAuthenticationFilter が受け取る
    ↓
3. AuthenticationManager に認証を依頼
    ↓
4. UserDetailsService.loadUserByUsername() 呼び出し
    ↓
5. パスワード照合
    ↓
6. 認証成功なら SecurityContext に保存
    ↓
7. HTTPセッションに保存
```

## 🔍 認証状態の保存と確認

### 認証成功時の処理（Spring Security内部）

```java
public class UsernamePasswordAuthenticationFilter {
    
    protected void successfulAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain,
            Authentication authResult) {
        
        // 1. 認証情報をSecurityContextに設定
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);
        
        // 2. セッションに保存（ここが重要！）
        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", context);
        
        // 3. リダイレクト（これは単なる画面遷移）
        getSuccessHandler().onAuthenticationSuccess(request, response, authResult);
    }
}
```

### 認証状態の確認（すべてのリクエストで実行）

```java
public class SecurityContextPersistenceFilter {
    
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
        HttpServletRequest request = (HttpServletRequest) req;
        
        // 1. 毎回セッションから認証情報を復元
        HttpSession session = request.getSession(false);
        if (session != null) {
            SecurityContext contextFromSession = (SecurityContext) 
                session.getAttribute("SPRING_SECURITY_CONTEXT");
            
            if (contextFromSession != null) {
                // 認証済み！
                SecurityContextHolder.setContext(contextFromSession);
            }
        }
        
        // 2. リクエスト処理
        chain.doFilter(request, response);
    }
}
```

## 📊 具体例で理解する

### シナリオ1: 初回アクセス

```
1. GET /home
    ↓
2. SecurityFilterChain
    - .anyRequest().authenticated() にマッチ
    - セッションチェック → 認証情報なし
    - AccessDeniedException 発生
    ↓
3. ExceptionTranslationFilter
    - 未認証なので /login へリダイレクト
```

### シナリオ2: ログイン実行

```
1. POST /login (username=user1, password=pass123)
    ↓
2. UsernamePasswordAuthenticationFilter
    - フォームデータ取得
    - 認証処理実行
    ↓
3. UserDetailsService
    - DBからユーザー情報取得
    - パスワード照合
    ↓
4. 認証成功
    - セッションに保存: session["SPRING_SECURITY_CONTEXT"] = 認証情報
    - リダイレクト: /home
```

### シナリオ3: ログイン後のアクセス

```
1. GET /home (Cookie: JSESSIONID=xxxxx)
    ↓
2. SecurityContextPersistenceFilter
    - セッションID(xxxxx)から復元
    - session["SPRING_SECURITY_CONTEXT"] → 認証情報あり！
    ↓
3. SecurityFilterChain
    - .anyRequest().authenticated() → OK
    ↓
4. AuthController.home() 実行
```

## 💡 重要ポイント

### 1. 認証状態はセッションで管理

```java
// 実際の保存場所
HTTPSession {
    "SPRING_SECURITY_CONTEXT": SecurityContext {
        authentication: UsernamePasswordAuthenticationToken {
            principal: UserDetails { username: "user1" },
            authenticated: true,
            authorities: ["ROLE_USER"]
        }
    }
}
```

### 2. authenticated() の判定

```java
// Spring Securityの内部実装（簡略化）
public boolean isAuthenticated() {
    // 現在のリクエストの認証情報を取得
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    
    // null でなく、認証済みフラグが true なら OK
    return auth != null && auth.isAuthenticated();
}
```

### 3. ログアウトの仕組み

```java
.logout(logout -> logout
    .logoutUrl("/logout")
    .logoutSuccessUrl("/login?logout")
    .invalidateHttpSession(true)  // セッション破棄
    .deleteCookies("JSESSIONID")  // Cookie削除
)

// ログアウト時の内部処理
public void logout(HttpServletRequest request, HttpServletResponse response) {
    // セッションを無効化
    HttpSession session = request.getSession(false);
    if (session != null) {
        session.invalidate();  // ここで認証情報も消える
    }
}
```

## 🎯 まとめ

1. **formLogin設定** = ログイン画面の場所とリダイレクト先の指定のみ
2. **認証判断** = セッション内の認証情報の有無で判定
3. **画面遷移** = 認証の結果であって、認証の手段ではない

```
認証状態 = セッションの中身
画面遷移 = 認証結果の表現
```

## デバッグ用コード

```java
@RestController
@RequestMapping("/debug")
public class AuthDebugController {
    
    @GetMapping("/session")
    public Map<String, Object> debugSession(HttpSession session) {
        SecurityContext context = (SecurityContext) 
            session.getAttribute("SPRING_SECURITY_CONTEXT");
            
        return Map.of(
            "sessionId", session.getId(),
            "hasSecurityContext", context != null,
            "isAuthenticated", context != null && 
                context.getAuthentication() != null &&
                context.getAuthentication().isAuthenticated(),
            "username", context != null && 
                context.getAuthentication() != null ?
                context.getAuthentication().getName() : "anonymous"
        );
    }
}
```

アクセスして確認:
- ログイン前: `http://localhost:8080/debug/session` → `isAuthenticated: false`
- ログイン後: `http://localhost:8080/debug/session` → `isAuthenticated: true`