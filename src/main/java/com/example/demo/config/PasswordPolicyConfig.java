package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * パスワードポリシー設定クラス
 * 環境変数から設定値を読み込む
 */
@Configuration
@ConfigurationProperties(prefix = "app.password.policy")
@Data
public class PasswordPolicyConfig {
    
    // 最小文字数
    private int minLength = 8;
    
    // 最大文字数
    private int maxLength = 128;
    
    // 大文字必須
    private boolean requireUppercase = true;
    
    // 小文字必須
    private boolean requireLowercase = true;
    
    // 数字必須
    private boolean requireDigit = true;
    
    // 特殊文字必須
    private boolean requireSpecialChar = true;
    
    // 許可する特殊文字
    private String specialChars = "@$!%*#?&";
    
    // パスワード履歴保持数
    private int historyCount = 5;
    
    // パスワード有効期限（日数）
    private int expirationDays = 90;
    
    // 同じ文字の連続使用制限
    private int maxConsecutiveChars = 3;
    
    // ユーザー名を含むことを禁止
    private boolean preventUsernameInPassword = true;
    
    // よく使われるパスワードの禁止
    private boolean preventCommonPasswords = true;
}