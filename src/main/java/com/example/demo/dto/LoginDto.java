package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * ログイン用DTO（レコードクラス）
 */
public record LoginDto(
    @NotBlank(message = "ユーザー名は必須です")
    String username,
    
    @NotBlank(message = "パスワードは必須です")
    String password,
    
    boolean rememberMe
) {}