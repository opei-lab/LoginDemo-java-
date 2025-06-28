package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 信頼済みデバイスエンティティ
 * ユーザーが以前正常にログインしたデバイスを記録
 */
@Entity
@Table(name = "trusted_devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrustedDevice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "device_fingerprint", nullable = false)
    private String deviceFingerprint;
    
    @Column(name = "device_name")
    private String deviceName;
    
    @Column(name = "last_ip_address")
    private String lastIpAddress;
    
    @Column(name = "last_user_agent")
    private String lastUserAgent;
    
    @Column(name = "last_country_code")
    private String lastCountryCode;
    
    @Column(name = "last_city")
    private String lastCity;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "last_used_at", nullable = false)
    private LocalDateTime lastUsedAt = LocalDateTime.now();
    
    @Column(name = "trust_expires_at")
    private LocalDateTime trustExpiresAt;
    
    @Column(name = "is_active")
    private boolean isActive = true;
}