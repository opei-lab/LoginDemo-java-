package com.example.demo.service.impl;

import com.example.demo.dto.LoginContext;
import com.example.demo.dto.RiskAssessmentResult;
import com.example.demo.entity.LoginAttempt;
import com.example.demo.entity.TrustedDevice;
import com.example.demo.entity.User;
import com.example.demo.repository.LoginAttemptRepository;
import com.example.demo.repository.TrustedDeviceRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.RiskAssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * リスク評価サービス実装
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskAssessmentServiceImpl implements RiskAssessmentService {
    
    private final LoginAttemptRepository loginAttemptRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final UserRepository userRepository;
    
    @Value("${app.security.risk.failed-attempts-threshold:5}")
    private int failedAttemptsThreshold;
    
    @Value("${app.security.risk.time-window-hours:24}")
    private int timeWindowHours;
    
    @Value("${app.security.risk.trust-device-days:30}")
    private int trustDeviceDays;
    
    @Value("${app.security.risk.unusual-hour-start:0}")
    private int unusualHourStart;
    
    @Value("${app.security.risk.unusual-hour-end:6}")
    private int unusualHourEnd;
    
    @Override
    @Transactional(readOnly = true)
    public RiskAssessmentResult assessLoginRisk(String username, LoginContext context) {
        log.info("リスク評価開始: ユーザー={}", username);
        
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            // ユーザーが存在しない場合も高リスクとして扱う
            return createHighRiskResult("ユーザーが存在しません");
        }
        
        // リスク要因を収集
        List<String> riskFactors = new ArrayList<>();
        int riskScore = 0;
        
        RiskAssessmentResult.RiskDetails.RiskDetailsBuilder detailsBuilder = 
            RiskAssessmentResult.RiskDetails.builder();
        
        // 1. デバイスチェック
        boolean isNewDevice = checkNewDevice(user, context.getDeviceFingerprint());
        if (isNewDevice) {
            riskFactors.add("新しいデバイスからのアクセス");
            riskScore += 20;
            detailsBuilder.newDevice(true);
        }
        
        // 2. 最近の失敗試行チェック
        LocalDateTime timeWindowStart = LocalDateTime.now().minusHours(timeWindowHours);
        long failedAttempts = loginAttemptRepository.countFailedAttempts(username, timeWindowStart);
        if (failedAttempts >= failedAttemptsThreshold) {
            riskFactors.add("複数回のログイン失敗");
            riskScore += 30;
            detailsBuilder.multipleFailedAttempts(true);
        }
        detailsBuilder.recentFailedAttempts((int) failedAttempts);
        
        // 3. IPアドレスチェック
        boolean isSuspiciousIp = checkSuspiciousIp(context);
        if (isSuspiciousIp) {
            riskFactors.add("疑わしいIPアドレス");
            riskScore += 25;
            detailsBuilder.suspiciousIp(true);
        }
        
        // 4. 地理的位置チェック
        boolean isNewLocation = checkNewLocation(username, context);
        if (isNewLocation) {
            riskFactors.add("新しい場所からのアクセス");
            riskScore += 20;
            detailsBuilder.newLocation(true);
        }
        
        // 5. アクセス時間チェック
        boolean isUnusualTime = checkUnusualTime(context.getHourOfDay());
        if (isUnusualTime) {
            riskFactors.add("通常と異なる時間帯のアクセス");
            riskScore += 15;
            detailsBuilder.unusualTime(true);
        }
        
        // 6. 複数IPアドレスチェック
        long distinctIps = loginAttemptRepository.countDistinctIpAddresses(username, timeWindowStart);
        if (distinctIps > 3) {
            riskFactors.add("複数のIPアドレスからのアクセス");
            riskScore += 20;
        }
        detailsBuilder.distinctIpCount((int) distinctIps);
        
        // 7. 国をまたぐアクセスチェック
        List<String> countries = loginAttemptRepository.findDistinctCountryCodes(username, timeWindowStart);
        if (countries.size() > 1) {
            riskFactors.add("複数の国からのアクセス");
            riskScore += 30;
            
            // 物理的に不可能な移動チェック
            if (checkRapidLocationChange(username, context)) {
                riskFactors.add("物理的に不可能な場所の移動");
                riskScore += 40;
                detailsBuilder.rapidLocationChange(true);
            }
        }
        detailsBuilder.distinctCountries(countries);
        
        // リスクスコアの正規化（最大100）
        riskScore = Math.min(riskScore, 100);
        
        // リスクレベルの判定
        RiskAssessmentResult.RiskLevel riskLevel = determineRiskLevel(riskScore);
        
        // 追加認証の必要性を判定
        boolean requiresAdditionalVerification = riskScore > 30;
        
        // 推奨される認証方法
        List<String> recommendedMethods = determineVerificationMethods(riskScore, user);
        
        log.info("リスク評価完了: ユーザー={}, スコア={}, レベル={}", 
                username, riskScore, riskLevel);
        
        return RiskAssessmentResult.builder()
            .riskScore(riskScore)
            .riskLevel(riskLevel)
            .requiresAdditionalVerification(requiresAdditionalVerification)
            .recommendedVerificationMethods(recommendedMethods)
            .riskFactors(riskFactors)
            .riskDetails(detailsBuilder.build())
            .build();
    }
    
    @Override
    @Transactional
    public void trustDevice(String username, String deviceFingerprint, String deviceName) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        
        // 既存のデバイスチェック
        trustedDeviceRepository.findByUserAndDeviceFingerprintAndIsActiveTrue(user, deviceFingerprint)
            .ifPresentOrElse(
                device -> {
                    // 既存デバイスの更新
                    device.setLastUsedAt(LocalDateTime.now());
                    trustedDeviceRepository.save(device);
                },
                () -> {
                    // 新規デバイスの登録
                    TrustedDevice newDevice = new TrustedDevice();
                    newDevice.setUser(user);
                    newDevice.setDeviceFingerprint(deviceFingerprint);
                    newDevice.setDeviceName(deviceName);
                    newDevice.setTrustExpiresAt(LocalDateTime.now().plusDays(trustDeviceDays));
                    trustedDeviceRepository.save(newDevice);
                }
            );
        
        log.info("デバイスを信頼済みとして登録: ユーザー={}, デバイス={}", username, deviceName);
    }
    
    @Override
    @Transactional
    public void removeTrustedDevice(String username, Long deviceId) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        
        TrustedDevice device = trustedDeviceRepository.findById(deviceId)
            .orElseThrow(() -> new RuntimeException("デバイスが見つかりません"));
        
        if (!device.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("権限がありません");
        }
        
        device.setActive(false);
        trustedDeviceRepository.save(device);
        
        log.info("信頼済みデバイスを削除: ユーザー={}, デバイスID={}", username, deviceId);
    }
    
    @Override
    @Transactional
    public void recordLoginAttempt(String username, LoginContext context, boolean successful,
                                 int riskScore, String riskFactors) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUsername(username);
        attempt.setIpAddress(context.getIpAddress());
        attempt.setUserAgent(context.getUserAgent());
        attempt.setDeviceFingerprint(context.getDeviceFingerprint());
        attempt.setSuccessful(successful);
        attempt.setRiskScore(riskScore);
        attempt.setRiskFactors(riskFactors);
        attempt.setCountryCode(context.getCountryCode());
        attempt.setCity(context.getCity());
        attempt.setProxy(context.isProxy());
        attempt.setVpn(context.isVpn());
        
        loginAttemptRepository.save(attempt);
        
        log.debug("ログイン試行を記録: ユーザー={}, 成功={}, リスクスコア={}", 
                 username, successful, riskScore);
    }
    
    /**
     * 新しいデバイスかチェック
     */
    private boolean checkNewDevice(User user, String deviceFingerprint) {
        if (deviceFingerprint == null) {
            return true;
        }
        
        return trustedDeviceRepository
            .findByUserAndDeviceFingerprintAndIsActiveTrue(user, deviceFingerprint)
            .isEmpty();
    }
    
    /**
     * 疑わしいIPアドレスかチェック
     */
    private boolean checkSuspiciousIp(LoginContext context) {
        // プロキシやVPN経由のアクセスを疑わしいとする
        return context.isProxy() || context.isVpn();
    }
    
    /**
     * 新しい場所からのアクセスかチェック
     */
    private boolean checkNewLocation(String username, LoginContext context) {
        // 過去のログイン履歴から場所をチェック
        List<LoginAttempt> recentAttempts = loginAttemptRepository
            .findByUsernameAndAttemptedAtAfter(username, 
                LocalDateTime.now().minusDays(30));
        
        if (recentAttempts.isEmpty()) {
            return false; // 履歴がない場合は新しい場所とはしない
        }
        
        // 現在の国が過去の履歴にあるかチェック
        return recentAttempts.stream()
            .map(LoginAttempt::getCountryCode)
            .filter(code -> code != null && code.equals(context.getCountryCode()))
            .findAny()
            .isEmpty();
    }
    
    /**
     * 通常と異なる時間帯かチェック
     */
    private boolean checkUnusualTime(int hourOfDay) {
        return hourOfDay >= unusualHourStart && hourOfDay <= unusualHourEnd;
    }
    
    /**
     * 物理的に不可能な場所の移動をチェック
     */
    private boolean checkRapidLocationChange(String username, LoginContext context) {
        // 最後の成功したログインを取得
        LoginAttempt lastSuccess = loginAttemptRepository
            .findTopByUsernameAndSuccessfulTrueOrderByAttemptedAtDesc(username);
        
        if (lastSuccess == null || lastSuccess.getCountryCode() == null || 
            context.getCountryCode() == null) {
            return false;
        }
        
        // 異なる国で、時間差が短い場合は疑わしい
        if (!lastSuccess.getCountryCode().equals(context.getCountryCode())) {
            long hoursDiff = ChronoUnit.HOURS.between(
                lastSuccess.getAttemptedAt(), LocalDateTime.now());
            
            // 3時間以内に異なる国からのアクセスは物理的に困難
            return hoursDiff < 3;
        }
        
        return false;
    }
    
    /**
     * リスクレベルを判定
     */
    private RiskAssessmentResult.RiskLevel determineRiskLevel(int riskScore) {
        if (riskScore <= 30) {
            return RiskAssessmentResult.RiskLevel.LOW;
        } else if (riskScore <= 60) {
            return RiskAssessmentResult.RiskLevel.MEDIUM;
        } else if (riskScore <= 80) {
            return RiskAssessmentResult.RiskLevel.HIGH;
        } else {
            return RiskAssessmentResult.RiskLevel.CRITICAL;
        }
    }
    
    /**
     * 推奨される認証方法を決定
     */
    private List<String> determineVerificationMethods(int riskScore, User user) {
        List<String> methods = new ArrayList<>();
        
        if (riskScore > 30) {
            // MFAが有効な場合はTOTPを推奨
            if (user.isMfaEnabled()) {
                methods.add("TOTP");
            }
            
            // メールOTPは常に利用可能
            methods.add("EMAIL_OTP");
        }
        
        if (riskScore > 60) {
            // 高リスクの場合は複数の認証方法を推奨
            methods.add("SECURITY_QUESTIONS");
        }
        
        return methods;
    }
    
    /**
     * 高リスク結果を作成
     */
    private RiskAssessmentResult createHighRiskResult(String reason) {
        return RiskAssessmentResult.builder()
            .riskScore(100)
            .riskLevel(RiskAssessmentResult.RiskLevel.CRITICAL)
            .requiresAdditionalVerification(true)
            .recommendedVerificationMethods(Arrays.asList("BLOCK"))
            .riskFactors(Arrays.asList(reason))
            .build();
    }
}