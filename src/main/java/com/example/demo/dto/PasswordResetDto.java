package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * パスワードリセット用DTO（レコードクラス）
 */
public record PasswordResetDto(
    @NotBlank(message = "トークンは必須です")
    String token,
    
    @NotBlank(message = "新しいパスワードは必須です")
    @Size(min = 8, message = "パスワードは8文字以上で入力してください")
    String newPassword,
    
    @NotBlank(message = "パスワード確認は必須です")
    String confirmPassword
) {
    /**
     * パスワードと確認用パスワードが一致するか検証
     */
    public boolean isPasswordMatching() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}