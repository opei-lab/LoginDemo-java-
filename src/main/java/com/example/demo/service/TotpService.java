package com.example.demo.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * TOTP（Time-based One-Time Password）管理サービス
 * Google Authenticator等のアプリと連携
 */
@Service
@Slf4j
public class TotpService {
    
    @Value("${spring.application.name:LoginDemo}")
    private String applicationName;
    
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    
    /**
     * 新しいシークレットキーを生成
     * @return Base32エンコードされたシークレット
     */
    public String generateSecret() {
        return secretGenerator.generate();
    }
    
    /**
     * QRコードを生成（Base64エンコード）
     * @param username ユーザー名
     * @param secret シークレットキー
     * @return Base64エンコードされたQRコード画像
     */
    public String generateQrCodeBase64(String username, String secret) {
        try {
            QrData qrData = new QrData.Builder()
                .label(username)
                .secret(secret)
                .issuer(applicationName)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
            
            // QRコードのURI生成
            String otpAuthUri = qrData.getUri();
            
            // QRコード画像生成
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(otpAuthUri, BarcodeFormat.QR_CODE, 200, 200);
            
            // 画像をバイト配列に変換
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            
            // Base64エンコード
            byte[] qrCodeBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(qrCodeBytes);
            
        } catch (WriterException | IOException e) {
            log.error("QRコード生成エラー", e);
            throw new RuntimeException("QRコードの生成に失敗しました", e);
        }
    }
    
    /**
     * TOTPコードを検証
     * @param secret シークレットキー
     * @param code 6桁のコード
     * @return 検証結果
     */
    public boolean verifyCode(String secret, String code) {
        try {
            return codeVerifier.isValidCode(secret, code);
        } catch (Exception e) {
            log.error("TOTPコード検証エラー", e);
            return false;
        }
    }
    
    /**
     * バックアップコードを生成（8桁のランダム数字）
     * @param count 生成する個数
     * @return バックアップコードの配列
     */
    public String[] generateBackupCodes(int count) {
        String[] backupCodes = new String[count];
        for (int i = 0; i < count; i++) {
            backupCodes[i] = generateBackupCode();
        }
        return backupCodes;
    }
    
    /**
     * 単一のバックアップコードを生成
     * @return 8桁のバックアップコード
     */
    private String generateBackupCode() {
        int code = (int) (Math.random() * 100000000);
        return String.format("%08d", code);
    }
}