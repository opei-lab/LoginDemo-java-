package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 監査ログエンティティ
 * セキュリティ関連のイベントを記録
 */
@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // イベントタイプ
    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType;
    
    // ユーザー名
    @Column(name = "username")
    private String username;
    
    // IPアドレス
    @Column(name = "ip_address")
    private String ipAddress;
    
    // ユーザーエージェント
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    // イベントの詳細
    @Column(name = "details", length = 1000)
    private String details;
    
    // 成功/失敗フラグ
    @Column(name = "success")
    private boolean success;
    
    // イベント発生日時
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    /**
     * イベントタイプ定義
     */
    public enum EventType {
        // 認証関連
        LOGIN_SUCCESS("ログイン成功"),
        LOGIN_FAILURE("ログイン失敗"),
        LOGOUT("ログアウト"),
        ACCOUNT_LOCKED("アカウントロック"),
        ACCOUNT_UNLOCKED("アカウントロック解除"),
        
        // ユーザー管理関連
        USER_REGISTERED("ユーザー登録"),
        PASSWORD_CHANGED("パスワード変更"),
        PASSWORD_RESET_REQUESTED("パスワードリセット要求"),
        PASSWORD_RESET_COMPLETED("パスワードリセット完了"),
        
        // MFA関連
        MFA_ENABLED("MFA有効化"),
        MFA_DISABLED("MFA無効化"),
        MFA_SUCCESS("MFA認証成功"),
        MFA_FAILURE("MFA認証失敗"),
        
        // OTP関連
        OTP_SENT("OTP送信"),
        OTP_REQUEST_FAILED("OTP送信失敗"),
        OTP_VERIFIED("OTP検証成功"),
        OTP_FAILED("OTP検証失敗"),
        
        // セッション関連
        SESSION_CREATED("セッション作成"),
        SESSION_EXPIRED("セッション期限切れ"),
        SESSION_INVALIDATED("セッション無効化"),
        
        // セキュリティ関連
        SUSPICIOUS_ACTIVITY("不審なアクティビティ"),
        ACCESS_DENIED("アクセス拒否");
        
        private final String description;
        
        EventType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}