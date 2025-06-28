package com.example.demo.service;

import com.example.demo.entity.AuditLog;
import com.example.demo.entity.AuditLog.EventType;
import com.example.demo.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 監査ログサービス
 * セキュリティイベントの記録と管理
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {
    
    private final AuditLogRepository auditLogRepository;
    
    // 不審なアクティビティの閾値
    private static final int SUSPICIOUS_LOGIN_ATTEMPTS_THRESHOLD = 10;
    private static final int SUSPICIOUS_ACTIVITY_TIME_WINDOW_HOURS = 1;
    
    /**
     * 監査ログを記録
     * @param eventType イベントタイプ
     * @param username ユーザー名
     * @param success 成功/失敗
     * @param details 詳細情報
     */
    @Transactional
    public void logEvent(EventType eventType, String username, boolean success, String details) {
        try {
            HttpServletRequest request = getCurrentRequest();
            
            AuditLog auditLog = AuditLog.builder()
                .eventType(eventType)
                .username(username)
                .success(success)
                .details(details)
                .ipAddress(getClientIpAddress(request))
                .userAgent(getUserAgent(request))
                .build();
            
            auditLogRepository.save(auditLog);
            
            // 不審なアクティビティの検出
            if (eventType == EventType.LOGIN_FAILURE) {
                checkSuspiciousActivity(username, auditLog.getIpAddress());
            }
            
        } catch (Exception e) {
            log.error("監査ログの記録に失敗しました", e);
        }
    }
    
    /**
     * 簡易版ログ記録（成功イベント用）
     * @param eventType イベントタイプ
     * @param username ユーザー名
     */
    public void logSuccess(EventType eventType, String username) {
        logEvent(eventType, username, true, null);
    }
    
    /**
     * 簡易版ログ記録（失敗イベント用）
     * @param eventType イベントタイプ
     * @param username ユーザー名
     * @param reason 失敗理由
     */
    public void logFailure(EventType eventType, String username, String reason) {
        logEvent(eventType, username, false, reason);
    }
    
    /**
     * OAuth2ログインをログ記録
     * @param username ユーザー名
     * @param provider プロバイダー名
     * @param success 成功/失敗
     * @param details 詳細情報
     */
    public void logOAuth2Login(String username, String provider, boolean success, String details) {
        String message = String.format("OAuth2ログイン (プロバイダー: %s) - %s", provider, details);
        logEvent(EventType.LOGIN_SUCCESS, username, success, message);
    }
    
    /**
     * 不審なアクティビティをチェック
     * @param username ユーザー名
     * @param ipAddress IPアドレス
     */
    private void checkSuspiciousActivity(String username, String ipAddress) {
        LocalDateTime startDate = LocalDateTime.now().minusHours(SUSPICIOUS_ACTIVITY_TIME_WINDOW_HOURS);
        
        // IPアドレスからの失敗試行をチェック
        List<AuditLog> suspiciousLogs = auditLogRepository.findSuspiciousActivitiesByIp(
            ipAddress, startDate);
        
        if (suspiciousLogs.size() >= SUSPICIOUS_LOGIN_ATTEMPTS_THRESHOLD) {
            logEvent(EventType.SUSPICIOUS_ACTIVITY, username, false, 
                String.format("IPアドレス %s から %d 回の失敗試行", ipAddress, suspiciousLogs.size()));
            
            log.warn("不審なアクティビティを検出: IP={}, 失敗回数={}", ipAddress, suspiciousLogs.size());
        }
    }
    
    /**
     * 現在のHTTPリクエストを取得
     * @return HTTPリクエスト
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
    
    /**
     * クライアントのIPアドレスを取得
     * @param request HTTPリクエスト
     * @return IPアドレス
     */
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        // プロキシ経由の場合の対応
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * ユーザーエージェントを取得
     * @param request HTTPリクエスト
     * @return ユーザーエージェント
     */
    private String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.length() > 500) {
            // データベースのカラムサイズに合わせて切り詰め
            return userAgent.substring(0, 497) + "...";
        }
        
        return userAgent != null ? userAgent : "unknown";
    }
}