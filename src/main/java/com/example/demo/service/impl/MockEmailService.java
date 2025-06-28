package com.example.demo.service.impl;

import com.example.demo.service.IEmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

/**
 * 開発環境用のモックメールサービス
 * 実際にはメールを送信せず、コンソールに出力する
 */
@Service
@Profile("dev")
@Slf4j
public class MockEmailService implements IEmailService {
    
    @PostConstruct
    public void init() {
        log.info("===== MockEmailService が有効化されました（開発環境） =====");
    }
    
    @Override
    public void sendOtpEmail(String to, String username, String otpCode, int validMinutes) {
        log.info("========== OTPメール（モック） ==========");
        log.info("宛先: {}", to);
        log.info("ユーザー名: {}", username);
        log.info("OTPコード: {}", otpCode);
        log.info("有効期限: {}分", validMinutes);
        log.info("======================================");
        
        // 開発時はコンソールに表示するだけ
        System.out.println("\n【OTPコード】: " + otpCode + "\n");
    }
    
    @Override
    public void sendPasswordResetEmail(String to, String username, String resetCode, int validMinutes) {
        log.info("========== パスワードリセットメール（モック） ==========");
        log.info("宛先: {}", to);
        log.info("ユーザー名: {}", username);
        log.info("リセットコード: {}", resetCode);
        log.info("有効期限: {}分", validMinutes);
        log.info("================================================");
        
        System.out.println("\n【リセットコード】: " + resetCode + "\n");
    }
    
    @Override
    public void sendWelcomeEmail(String to, String username) {
        log.info("========== ウェルカムメール（モック） ==========");
        log.info("宛先: {}", to);
        log.info("ユーザー名: {}", username);
        log.info("===========================================");
    }
}