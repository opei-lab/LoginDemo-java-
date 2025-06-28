package com.example.demo.service;

/**
 * メール送信サービスインターフェース
 */
public interface IEmailService {
    
    /**
     * OTPメール送信
     * @param to 宛先
     * @param username ユーザー名
     * @param otpCode ワンタイムパスワード
     * @param validMinutes 有効期限（分）
     */
    void sendOtpEmail(String to, String username, String otpCode, int validMinutes);
    
    /**
     * パスワードリセットメール送信
     * @param to 宛先
     * @param username ユーザー名
     * @param resetCode リセットコード
     * @param validMinutes 有効期限（分）
     */
    void sendPasswordResetEmail(String to, String username, String resetCode, int validMinutes);
    
    /**
     * ウェルカムメール送信
     * @param to 宛先
     * @param username ユーザー名
     */
    void sendWelcomeEmail(String to, String username);
}