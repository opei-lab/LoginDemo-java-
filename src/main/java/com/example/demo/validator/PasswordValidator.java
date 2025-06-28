package com.example.demo.validator;

import com.example.demo.config.PasswordPolicyConfig;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * パスワードバリデーター
 * パスワードポリシーに基づいて検証を行う
 */
@Component
@RequiredArgsConstructor
public class PasswordValidator {
    
    private final PasswordPolicyConfig passwordPolicy;
    
    // よく使われるパスワードのリスト（本番環境では外部ファイルから読み込む）
    private static final Set<String> COMMON_PASSWORDS = Set.of(
        "password", "123456", "12345678", "qwerty", "abc123",
        "monkey", "1234567", "letmein", "trustno1", "dragon",
        "baseball", "111111", "iloveyou", "master", "sunshine",
        "ashley", "bailey", "passw0rd", "shadow", "123123",
        "654321", "superman", "qazwsx", "michael", "football"
    );
    
    /**
     * パスワードの検証を実行
     * @param password 検証するパスワード
     * @param username ユーザー名（パスワードに含まれていないかチェック用）
     * @return 検証結果
     */
    public ValidationResult validate(String password, String username) {
        List<String> errors = new ArrayList<>();
        
        if (password == null || password.isEmpty()) {
            errors.add("パスワードは必須です");
            return new ValidationResult(false, errors);
        }
        
        // 長さチェック
        if (password.length() < passwordPolicy.getMinLength()) {
            errors.add(String.format("パスワードは%d文字以上必要です", passwordPolicy.getMinLength()));
        }
        if (password.length() > passwordPolicy.getMaxLength()) {
            errors.add(String.format("パスワードは%d文字以下にしてください", passwordPolicy.getMaxLength()));
        }
        
        // 文字種チェック
        if (passwordPolicy.isRequireUppercase() && !containsUppercase(password)) {
            errors.add("大文字を1文字以上含めてください");
        }
        if (passwordPolicy.isRequireLowercase() && !containsLowercase(password)) {
            errors.add("小文字を1文字以上含めてください");
        }
        if (passwordPolicy.isRequireDigit() && !containsDigit(password)) {
            errors.add("数字を1文字以上含めてください");
        }
        if (passwordPolicy.isRequireSpecialChar() && !containsSpecialChar(password)) {
            errors.add(String.format("特殊文字（%s）を1文字以上含めてください", passwordPolicy.getSpecialChars()));
        }
        
        // 連続文字チェック
        if (hasConsecutiveChars(password)) {
            errors.add(String.format("同じ文字を%d文字以上連続で使用することはできません", 
                passwordPolicy.getMaxConsecutiveChars()));
        }
        
        // ユーザー名を含むかチェック
        if (passwordPolicy.isPreventUsernameInPassword() && username != null && 
            password.toLowerCase().contains(username.toLowerCase())) {
            errors.add("パスワードにユーザー名を含めることはできません");
        }
        
        // よく使われるパスワードチェック
        if (passwordPolicy.isPreventCommonPasswords() && 
            COMMON_PASSWORDS.contains(password.toLowerCase())) {
            errors.add("よく使われるパスワードは使用できません");
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    private boolean containsUppercase(String password) {
        return password.chars().anyMatch(Character::isUpperCase);
    }
    
    private boolean containsLowercase(String password) {
        return password.chars().anyMatch(Character::isLowerCase);
    }
    
    private boolean containsDigit(String password) {
        return password.chars().anyMatch(Character::isDigit);
    }
    
    private boolean containsSpecialChar(String password) {
        String specialCharsPattern = "[" + Pattern.quote(passwordPolicy.getSpecialChars()) + "]";
        return password.matches(".*" + specialCharsPattern + ".*");
    }
    
    private boolean hasConsecutiveChars(String password) {
        if (password.length() < passwordPolicy.getMaxConsecutiveChars()) {
            return false;
        }
        
        int consecutiveCount = 1;
        char previousChar = password.charAt(0);
        
        for (int i = 1; i < password.length(); i++) {
            char currentChar = password.charAt(i);
            if (currentChar == previousChar) {
                consecutiveCount++;
                if (consecutiveCount >= passwordPolicy.getMaxConsecutiveChars()) {
                    return true;
                }
            } else {
                consecutiveCount = 1;
                previousChar = currentChar;
            }
        }
        
        return false;
    }
    
    /**
     * 検証結果クラス
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public String getErrorMessage() {
            return String.join("、", errors);
        }
    }
}