package com.example.demo.dto;

import java.util.List;

/**
 * MFA設定情報DTO（レコードクラス）
 */
public record MfaSetupDto(
    String secretKey,
    String qrCodeUri,
    List<String> backupCodes
) {}