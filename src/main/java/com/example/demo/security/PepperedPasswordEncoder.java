package com.example.demo.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * ペッパー付きパスワードエンコーダー
 * BCryptの前にペッパーを適用してセキュリティを強化
 */
@Component
public class PepperedPasswordEncoder implements PasswordEncoder {
    
    private final BCryptPasswordEncoder bcryptEncoder;
    private final String pepper;
    
    public PepperedPasswordEncoder(@Value("${app.security.pepper}") String pepper) {
        this.bcryptEncoder = new BCryptPasswordEncoder();
        this.pepper = pepper;
    }
    
    @Override
    public String encode(CharSequence rawPassword) {
        // パスワードにペッパーを適用
        String pepperedPassword = applyPepper(rawPassword.toString());
        // BCryptでハッシュ化（ソルトは自動的に付与される）
        return bcryptEncoder.encode(pepperedPassword);
    }
    
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        // パスワードにペッパーを適用
        String pepperedPassword = applyPepper(rawPassword.toString());
        // BCryptで検証
        return bcryptEncoder.matches(pepperedPassword, encodedPassword);
    }
    
    /**
     * パスワードにペッパーを適用
     * @param password 元のパスワード
     * @return ペッパー適用後のパスワード
     */
    private String applyPepper(String password) {
        try {
            // パスワード + ペッパーをSHA-256でハッシュ化
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String peppered = password + pepper;
            byte[] hash = digest.digest(peppered.getBytes(StandardCharsets.UTF_8));
            // Base64エンコードして返す
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256は必ず利用可能なはずなので、この例外は発生しない
            throw new RuntimeException("SHA-256アルゴリズムが見つかりません", e);
        }
    }
}