package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ユーザーエンティティ
 * 認証情報とセキュリティ関連の情報を管理
 * 
 * @Dataは使用せず、@Getter/@Setterを使用（JPAエンティティのベストプラクティス）
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"oauth2Links", "password", "mfaSecret"})  // セキュリティ情報は除外
@EqualsAndHashCode(onlyExplicitlyIncluded = true)  // IDのみでequals/hashCode
public class User {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include  // IDのみをequals/hashCodeに含める
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    // パスワード最終変更日時
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;
    
    // アカウントロック状態
    @Column(name = "account_locked")
    private boolean accountLocked = false;
    
    // ロック日時
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;
    
    // ログイン失敗回数
    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts = 0;
    
    // 最終ログイン成功日時
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    // 最終ログイン失敗日時
    @Column(name = "last_failed_login_at")
    private LocalDateTime lastFailedLoginAt;
    
    // アカウント作成日時
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // アカウント更新日時
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // メールアドレス（OTP送信用）
    @Column(unique = true)
    private String email;
    
    // 電話番号（SMS送信用）
    @Column(name = "phone_number")
    private String phoneNumber;
    
    // 二要素認証有効化フラグ
    @Column(name = "mfa_enabled")
    private boolean mfaEnabled = false;
    
    // 二要素認証シークレット
    @Column(name = "mfa_secret")
    private String mfaSecret;
    
    // フルネーム
    @Column(name = "full_name")
    private String fullName;
    
    // メール確認済みフラグ
    @Column(name = "email_verified")
    private boolean emailVerified = false;
    
    // アカウント有効化フラグ
    @Column(name = "enabled")
    private boolean enabled = true;
    
    // OAuth2連携情報
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OAuth2UserLink> oauth2Links = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        passwordChangedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
