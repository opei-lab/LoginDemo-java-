package com.example.demo.dto;

import jakarta.validation.constraints.*;

/**
 * ユーザー登録用DTO（Java 14+ レコードクラス）
 * 不変オブジェクトとして実装
 */
public record UserRegistrationDto(
    @NotBlank(message = "ユーザー名は必須です")
    @Size(min = 3, max = 20, message = "ユーザー名は3〜20文字で入力してください")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "ユーザー名は英数字とアンダースコアのみ使用可能です")
    String username,
    
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    String email,
    
    @NotBlank(message = "パスワードは必須です")
    @Size(min = 8, message = "パスワードは8文字以上で入力してください")
    String password,
    
    @NotBlank(message = "パスワード確認は必須です")
    String confirmPassword
) {
    /**
     * パスワードと確認用パスワードが一致するか検証
     */
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
    
    /**
     * Userエンティティへの変換用ファクトリメソッド
     */
    public com.example.demo.entity.User toUser() {
        var user = new com.example.demo.entity.User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        return user;
    }
}