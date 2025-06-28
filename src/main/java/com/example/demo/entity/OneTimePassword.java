package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * ワンタイムパスワードエンティティ
 * メール送信されるOTPを管理
 */
@Entity
@Table(name = "one_time_passwords")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OneTimePassword {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "code", nullable = false)
    private String code;
    
    @Column(name = "purpose", nullable = false)
    @Enumerated(EnumType.STRING)
    private OtpPurpose purpose;
    
    @Column(name = "used")
    @Builder.Default
    private boolean used = false;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    /**
     * OTPの有効性をチェック
     * @return 有効な場合true
     */
    public boolean isValid() {
        return !used && LocalDateTime.now().isBefore(expiresAt);
    }
    
    /**
     * OTPの用途
     */
    public enum OtpPurpose {
        LOGIN("ログイン"),
        PASSWORD_RESET("パスワードリセット"),
        EMAIL_VERIFICATION("メールアドレス確認");
        
        private final String description;
        
        OtpPurpose(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}