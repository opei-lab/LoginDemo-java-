package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * OAuth2プロバイダーとの連携情報エンティティ
 */
@Entity
@Table(name = "oauth2_user_links", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "user")
@EqualsAndHashCode(exclude = "user")
public class OAuth2UserLink {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String provider; // google, github, microsoft など
    
    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId; // プロバイダー側のユーザーID
    
    @Column(name = "provider_email")
    private String providerEmail;
    
    @Column(name = "provider_name")
    private String providerName;
    
    @Column(name = "provider_picture")
    private String providerPicture;
    
    @Column(name = "access_token", length = 1000)
    private String accessToken;
    
    @Column(name = "refresh_token", length = 1000)
    private String refreshToken;
    
    @Column(name = "token_expiry")
    private LocalDateTime tokenExpiry;
    
    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt = LocalDateTime.now();
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @Column(name = "is_active")
    private boolean isActive = true;
}