package com.example.demo.service;

import com.example.demo.entity.OneTimePassword;
import com.example.demo.entity.OneTimePassword.OtpPurpose;
import com.example.demo.entity.User;
import com.example.demo.repository.OneTimePasswordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * ワンタイムパスワード管理サービス
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {
    
    private final OneTimePasswordRepository otpRepository;
    private final IEmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Value("${app.otp.expiration-minutes:5}")
    private int otpExpirationMinutes;
    
    @Value("${app.otp.length:6}")
    private int otpLength;
    
    /**
     * OTPを生成して送信
     * @param user ユーザー
     * @param purpose 用途
     * @return 生成されたOTP
     */
    @Transactional
    public String generateAndSendOtp(User user, OtpPurpose purpose) {
        // 既存の未使用OTPを無効化
        otpRepository.invalidateUserOtps(user, purpose);
        
        // 新しいOTP生成
        String code = generateOtpCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpExpirationMinutes);
        
        OneTimePassword otp = OneTimePassword.builder()
            .user(user)
            .code(code)
            .purpose(purpose)
            .expiresAt(expiresAt)
            .used(false)
            .build();
        
        otpRepository.save(otp);
        
        // メール送信
        sendOtpEmail(user, code, purpose);
        
        log.info("OTP生成・送信: username={}, purpose={}", user.getUsername(), purpose);
        return code;
    }
    
    /**
     * OTPを検証
     * @param user ユーザー
     * @param code 入力されたコード
     * @param purpose 用途
     * @return 検証結果
     */
    @Transactional
    public boolean verifyOtp(User user, String code, OtpPurpose purpose) {
        Optional<OneTimePassword> otpOpt = otpRepository.findValidOtpByCode(
            user, code, LocalDateTime.now());
        
        if (otpOpt.isPresent()) {
            OneTimePassword otp = otpOpt.get();
            if (otp.getPurpose() == purpose && otp.isValid()) {
                // OTPを使用済みにする
                otp.setUsed(true);
                otpRepository.save(otp);
                
                log.info("OTP検証成功: username={}, purpose={}", user.getUsername(), purpose);
                return true;
            }
        }
        
        log.warn("OTP検証失敗: username={}, purpose={}", user.getUsername(), purpose);
        return false;
    }
    
    /**
     * 最新のOTPを取得（再送信用）
     * @param user ユーザー
     * @param purpose 用途
     * @return OTP（存在しない場合は空）
     */
    public Optional<OneTimePassword> getLatestOtp(User user, OtpPurpose purpose) {
        return otpRepository.findLatestValidOtp(user, purpose, LocalDateTime.now());
    }
    
    /**
     * OTPコード生成
     * @return 生成されたコード
     */
    private String generateOtpCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            code.append(secureRandom.nextInt(10));
        }
        return code.toString();
    }
    
    /**
     * OTPメール送信
     * @param user ユーザー
     * @param code OTPコード
     * @param purpose 用途
     */
    private void sendOtpEmail(User user, String code, OtpPurpose purpose) {
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new IllegalStateException("メールアドレスが設定されていません");
        }
        
        switch (purpose) {
            case LOGIN:
                emailService.sendOtpEmail(user.getEmail(), user.getUsername(), 
                    code, otpExpirationMinutes);
                break;
            case PASSWORD_RESET:
                emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), 
                    code, otpExpirationMinutes);
                break;
            case EMAIL_VERIFICATION:
                // 将来の実装用
                break;
        }
    }
    
    /**
     * 期限切れOTPの定期削除（1時間ごと）
     */
    @Scheduled(fixedRate = 3600000) // 1時間
    @Transactional
    public void cleanupExpiredOtps() {
        int deleted = otpRepository.deleteExpiredOtps(LocalDateTime.now());
        if (deleted > 0) {
            log.info("期限切れOTP削除: {}件", deleted);
        }
    }
}