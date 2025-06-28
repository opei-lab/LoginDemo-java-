package com.example.demo.service.impl;

import com.example.demo.service.IEmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * メール送信サービス
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!dev") // dev以外のプロファイルで有効
public class EmailServiceImpl implements IEmailService {
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${spring.application.name:LoginDemo}")
    private String applicationName;
    
    /**
     * シンプルなテキストメール送信
     * @param to 宛先
     * @param subject 件名
     * @param text 本文
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            log.info("メール送信成功: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("メール送信失敗: to={}, subject={}", to, subject, e);
            throw new RuntimeException("メール送信に失敗しました", e);
        }
    }
    
    /**
     * OTPメール送信
     * @param to 宛先
     * @param username ユーザー名
     * @param otp ワンタイムパスワード
     * @param expirationMinutes 有効期限（分）
     */
    @Override
    public void sendOtpEmail(String to, String username, String otp, int expirationMinutes) {
        String subject = String.format("[%s] ワンタイムパスワード", applicationName);
        
        String text = String.format(
            "%s 様\n\n" +
            "ワンタイムパスワードのご案内です。\n\n" +
            "認証コード: %s\n\n" +
            "このコードは %d 分間有効です。\n" +
            "他の人には教えないでください。\n\n" +
            "心当たりがない場合は、このメールを無視してください。\n\n" +
            "%s",
            username, otp, expirationMinutes, applicationName
        );
        
        sendSimpleEmail(to, subject, text);
    }
    
    /**
     * パスワードリセットメール送信
     * @param to 宛先
     * @param username ユーザー名
     * @param resetCode リセットコード
     * @param expirationMinutes 有効期限（分）
     */
    @Override
    public void sendPasswordResetEmail(String to, String username, String resetCode, int expirationMinutes) {
        String subject = String.format("[%s] パスワードリセット", applicationName);
        
        String text = String.format(
            "%s 様\n\n" +
            "パスワードリセットのリクエストを受け付けました。\n\n" +
            "リセットコード: %s\n\n" +
            "このコードは %d 分間有効です。\n" +
            "パスワードリセット画面でこのコードを入力してください。\n\n" +
            "このリクエストに心当たりがない場合は、このメールを無視してください。\n" +
            "あなたのパスワードは変更されません。\n\n" +
            "%s",
            username, resetCode, expirationMinutes, applicationName
        );
        
        sendSimpleEmail(to, subject, text);
    }
    
    /**
     * HTMLメール送信（将来の拡張用）
     * @param to 宛先
     * @param subject 件名
     * @param templateName テンプレート名
     * @param context コンテキスト
     */
    public void sendHtmlEmail(String to, String subject, String templateName, Context context) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            
            String htmlContent = templateEngine.process(templateName, context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("HTMLメール送信成功: to={}, subject={}", to, subject);
        } catch (MessagingException e) {
            log.error("HTMLメール送信失敗: to={}, subject={}", to, subject, e);
            throw new RuntimeException("メール送信に失敗しました", e);
        }
    }
    
    @Override
    public void sendWelcomeEmail(String to, String username) {
        String subject = String.format("[%s] アカウント登録完了", applicationName);
        
        String text = String.format(
            "%s 様\n\n" +
            "アカウントの登録が完了しました。\n" +
            "ご利用ありがとうございます。\n\n" +
            "%s",
            username, applicationName
        );
        
        sendSimpleEmail(to, subject, text);
    }
}