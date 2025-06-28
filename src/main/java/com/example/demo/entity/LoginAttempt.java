package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * ログイン試行エンティティ
 * リスクベース認証のためにログイン試行情報を記録する
 */
@Entity
@Table(name = "login_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String username;
    
    @Column(name = "ip_address", nullable = false)
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "device_fingerprint")
    private String deviceFingerprint;
    
    @Column(nullable = false)
    private boolean successful;
    
    @Column(name = "risk_score")
    private int riskScore;
    
    @Column(name = "risk_factors")
    private String riskFactors;
    
    @Column(name = "country_code")
    private String countryCode;
    
    @Column
    private String city;
    
    @Column(name = "is_proxy")
    private boolean isProxy;
    
    @Column(name = "is_vpn")
    private boolean isVpn;
    
    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt = LocalDateTime.now();
    
    @Column(name = "additional_verification_required")
    private boolean additionalVerificationRequired;
    
    @Column(name = "verification_method")
    private String verificationMethod;
}