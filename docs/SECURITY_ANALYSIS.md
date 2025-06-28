# セキュリティ分析：ブルートフォース vs パスワードスプレー攻撃

## 現在の実装状況

### 1. 実装済みの対策

```java
// UserServiceImpl.java の現在の実装
public void recordLoginAttempt(String username, boolean success) {
    // ユーザー単位でログイン試行を記録
    if (!success) {
        int failedAttempts = loginAttemptRepository
            .countRecentFailedAttempts(username, 30); // 30分以内
        
        if (failedAttempts >= 5) {
            // そのユーザーのみロック
            userRepository.lockAccount(username);
        }
    }
}
```

### 防げる攻撃 ✅
- **単純なブルートフォース**: 1つのアカウントに対する総当たり
  - user1 + password1 ❌
  - user1 + password2 ❌
  - user1 + password3 ❌
  - user1 + password4 ❌
  - user1 + password5 ❌ → **アカウントロック**

### 防げない攻撃 ❌
- **パスワードスプレー**: 多数のアカウントに同じパスワード
  - user1 + Password123! ❌
  - user2 + Password123! ❌
  - user3 + Password123! ✅ （もし弱いパスワードなら）
  - user4 + Password123! ❌
  - user5 + Password123! ❌
  - （各ユーザーは1回しか失敗していないのでロックされない）

## 追加すべき対策

### 1. IP単位のレート制限

```java
@Service
@RequiredArgsConstructor
public class EnhancedLoginAttemptService {
    private final LoginAttemptRepository repository;
    private final LoadingCache<String, Integer> attemptsCache;
    
    @PostConstruct
    public void init() {
        attemptsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Integer>() {
                @Override
                public Integer load(String key) {
                    return 0;
                }
            });
    }
    
    public void recordAttempt(String username, String ipAddress, boolean success) {
        // 1. 従来のユーザー単位の記録
        repository.save(new LoginAttempt(username, ipAddress, success));
        
        // 2. IP単位のカウント
        if (!success) {
            int attempts = attemptsCache.getUnchecked(ipAddress);
            attemptsCache.put(ipAddress, attempts + 1);
            
            // IP単位で閾値チェック
            if (attempts > 20) { // 15分で20回失敗
                throw new TooManyAttemptsException("IP temporarily blocked");
            }
        }
    }
}
```

### 2. 異常パターン検知

```java
@Component
public class AnomalyDetector {
    
    // パスワードスプレー検知
    public boolean detectPasswordSpray(LocalDateTime timeWindow) {
        // 同一パスワードハッシュで複数アカウントへの試行を検知
        List<LoginAttempt> attempts = repository.findFailedInTimeWindow(timeWindow);
        
        Map<String, Set<String>> passwordToUsers = new HashMap<>();
        // 実装上はパスワードハッシュではなく、失敗パターンで推測
        
        // 短時間に多数の異なるユーザーへの失敗
        Map<String, Long> ipToUniqueUsers = attempts.stream()
            .collect(Collectors.groupingBy(
                LoginAttempt::getIpAddress,
                Collectors.mapping(LoginAttempt::getUsername, 
                    Collectors.toSet())
            ))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> (long) e.getValue().size()
            ));
        
        // 1つのIPから10以上の異なるユーザーへの試行
        return ipToUniqueUsers.values().stream()
            .anyMatch(count -> count > 10);
    }
}
```

### 3. グローバルレート制限

```java
@Configuration
public class GlobalRateLimitConfig {
    
    @Bean
    public RateLimiter globalLoginRateLimiter() {
        // システム全体で1秒に10回までのログイン試行
        return RateLimiter.create(10.0);
    }
    
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
        FilterRegistrationBean<RateLimitFilter> registration = 
            new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter(globalLoginRateLimiter()));
        registration.addUrlPatterns("/login");
        return registration;
    }
}
```

### 4. CAPTCHA統合

```java
@Controller
public class EnhancedAuthController {
    
    @PostMapping("/login")
    public String login(@RequestParam String username, 
                       @RequestParam String password,
                       @RequestParam(required = false) String captcha,
                       HttpServletRequest request) {
        
        String ip = getClientIP(request);
        
        // 失敗回数に応じてCAPTCHA要求
        int failures = attemptService.getRecentFailures(ip);
        if (failures > 3 && !captchaService.verify(captcha)) {
            return "login?error=captcha";
        }
        
        // 通常のログイン処理
    }
}
```

### 5. 行動分析ベースの検知

```java
@Component
public class BehaviorAnalyzer {
    
    public RiskScore analyzeLoginBehavior(LoginRequest request) {
        RiskScore score = new RiskScore();
        
        // 1. 地理的異常
        if (isUnusualLocation(request.getIpAddress(), request.getUsername())) {
            score.add(30);
        }
        
        // 2. 時間的異常
        if (isUnusualTime(request.getTimestamp(), request.getUsername())) {
            score.add(20);
        }
        
        // 3. デバイス異常
        if (isNewDevice(request.getUserAgent(), request.getUsername())) {
            score.add(25);
        }
        
        // 4. 速度異常（人間には不可能な速さ）
        if (isTooFast(request.getUsername())) {
            score.add(40);
        }
        
        return score;
    }
}
```

## 実装の優先順位

1. **必須**: IP単位のレート制限
2. **推奨**: 異常パターン検知
3. **推奨**: CAPTCHA（3回失敗後）
4. **オプション**: 行動分析
5. **オプション**: WAF連携

## データベース設計の拡張

```sql
-- IP単位の集計用
CREATE TABLE ip_rate_limits (
    ip_address VARCHAR(45) PRIMARY KEY,
    attempt_count INT NOT NULL DEFAULT 0,
    first_attempt_at TIMESTAMP NOT NULL,
    last_attempt_at TIMESTAMP NOT NULL,
    blocked_until TIMESTAMP
);

-- 異常検知用
CREATE TABLE anomaly_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(50) NOT NULL, -- 'PASSWORD_SPRAY', 'BRUTE_FORCE', etc
    ip_address VARCHAR(45),
    details JSON,
    detected_at TIMESTAMP NOT NULL,
    severity VARCHAR(20) -- 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
);
```

## モニタリングとアラート

```java
@Component
@Slf4j
public class SecurityMonitor {
    
    @EventListener
    public void handleSecurityEvent(SecurityEvent event) {
        if (event instanceof PasswordSprayDetectedEvent) {
            // 1. ログ記録
            log.warn("Password spray detected from IP: {}", event.getIpAddress());
            
            // 2. メトリクス送信
            meterRegistry.counter("security.password_spray").increment();
            
            // 3. アラート送信
            alertService.sendAlert(Alert.critical()
                .title("Password Spray Attack Detected")
                .addDetail("IP", event.getIpAddress())
                .addDetail("Affected Users", event.getAffectedUsers())
                .build());
            
            // 4. 自動対応
            firewallService.temporaryBlock(event.getIpAddress(), Duration.ofHours(1));
        }
    }
}
```

## まとめ

現在の実装は基本的なブルートフォースには有効ですが、より巧妙な攻撃には追加対策が必要です：

| 攻撃タイプ | 現在の対策 | 必要な追加対策 |
|-----------|-----------|---------------|
| 単純ブルートフォース | ✅ アカウントロック | - |
| パスワードスプレー | ❌ 効果なし | IP単位制限、異常検知 |
| 分散型攻撃 | ❌ 効果なし | グローバル制限、CAPTCHA |
| クレデンシャルスタッフィング | ⚠️ 部分的 | 行動分析、MFA強制 |