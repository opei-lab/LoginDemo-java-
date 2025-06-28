package com.example.demo.service;

import com.example.demo.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * レート制限サービス
 * IPアドレスベースでアクセス頻度を制限
 * 
 * 実際の本番環境では Redis や Hazelcast などの
 * 分散キャッシュを使用することを推奨
 */
@Service
@Slf4j
public class RateLimitService {
    
    // スレッドセーフなマップ
    private final Map<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();
    
    private static final int MAX_ATTEMPTS_PER_MINUTE = 5;
    private static final int BLOCK_DURATION_MINUTES = 5;
    
    /**
     * レート制限のチェックと記録
     * 
     * @param key 制限キー（IPアドレスやユーザーID）
     * @param action アクション名（ログ用）
     * @throws RateLimitException 制限を超えた場合
     */
    public void checkAndRecord(String key, String action) {
        LocalDateTime now = LocalDateTime.now();
        
        RateLimitEntry entry = rateLimitMap.compute(key, (k, existing) -> {
            if (existing == null) {
                return new RateLimitEntry(1, now, null);
            }
            
            // ブロック中かチェック
            if (existing.blockedUntil != null && existing.blockedUntil.isAfter(now)) {
                return existing;
            }
            
            // 1分経過していればリセット
            if (existing.firstAttemptTime.isBefore(now.minusMinutes(1))) {
                return new RateLimitEntry(1, now, null);
            }
            
            // カウントを増やす
            int newCount = existing.attemptCount + 1;
            LocalDateTime blockedUntil = null;
            
            // 制限を超えたらブロック
            if (newCount > MAX_ATTEMPTS_PER_MINUTE) {
                blockedUntil = now.plusMinutes(BLOCK_DURATION_MINUTES);
                log.warn("レート制限発動: key={}, action={}", key, action);
            }
            
            return new RateLimitEntry(newCount, existing.firstAttemptTime, blockedUntil);
        });
        
        // ブロック中の場合は例外をスロー
        if (entry.blockedUntil != null && entry.blockedUntil.isAfter(now)) {
            int retryAfterSeconds = (int) java.time.Duration.between(now, entry.blockedUntil).getSeconds();
            throw new RateLimitException(
                "Too many requests. Please try again later.", 
                retryAfterSeconds
            );
        }
        
        // 制限を超えていたら例外をスロー
        if (entry.attemptCount > MAX_ATTEMPTS_PER_MINUTE) {
            throw new RateLimitException(
                "Rate limit exceeded.", 
                BLOCK_DURATION_MINUTES * 60
            );
        }
    }
    
    /**
     * 古いエントリを定期的にクリーンアップ（1時間ごと）
     * メモリリーク防止
     */
    @Scheduled(fixedDelay = 3600000) // 1時間
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        
        rateLimitMap.entrySet().removeIf(entry -> {
            RateLimitEntry value = entry.getValue();
            return value.firstAttemptTime.isBefore(cutoff) && 
                   (value.blockedUntil == null || value.blockedUntil.isBefore(cutoff));
        });
        
        log.info("レート制限エントリクリーンアップ完了: 残りエントリ数={}", rateLimitMap.size());
    }
    
    /**
     * レート制限エントリ
     */
    private static class RateLimitEntry {
        final int attemptCount;
        final LocalDateTime firstAttemptTime;
        final LocalDateTime blockedUntil;
        
        RateLimitEntry(int attemptCount, LocalDateTime firstAttemptTime, LocalDateTime blockedUntil) {
            this.attemptCount = attemptCount;
            this.firstAttemptTime = firstAttemptTime;
            this.blockedUntil = blockedUntil;
        }
    }
}