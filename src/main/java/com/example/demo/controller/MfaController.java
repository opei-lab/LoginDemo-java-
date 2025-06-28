package com.example.demo.controller;

import com.example.demo.entity.BackupCode;
import com.example.demo.entity.User;
import com.example.demo.repository.BackupCodeRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuditLogService;
import com.example.demo.service.TotpService;
import com.example.demo.entity.AuditLog.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;

/**
 * 多要素認証（MFA）設定コントローラー
 */
@Controller
@RequestMapping("/mfa")
@RequiredArgsConstructor
@Slf4j
public class MfaController {
    
    private final UserRepository userRepository;
    private final BackupCodeRepository backupCodeRepository;
    private final TotpService totpService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    
    private static final int BACKUP_CODE_COUNT = 10;
    
    /**
     * MFA設定画面表示
     */
    @GetMapping("/setup")
    public String showMfaSetup(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        
        if (user.isMfaEnabled()) {
            return "redirect:/mfa/manage";
        }
        
        // 新しいシークレット生成
        String secret = totpService.generateSecret();
        String qrCodeBase64 = totpService.generateQrCodeBase64(user.getUsername(), secret);
        
        model.addAttribute("secret", secret);
        model.addAttribute("qrCode", qrCodeBase64);
        model.addAttribute("username", user.getUsername());
        
        return "mfa/setup";
    }
    
    /**
     * MFA有効化処理
     */
    @PostMapping("/enable")
    @Transactional
    public String enableMfa(@AuthenticationPrincipal UserDetails userDetails,
                           @RequestParam String secret,
                           @RequestParam String code,
                           @RequestParam String password,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        
        // パスワード確認
        if (!passwordEncoder.matches(password, user.getPassword())) {
            model.addAttribute("error", "パスワードが正しくありません");
            model.addAttribute("secret", secret);
            model.addAttribute("qrCode", totpService.generateQrCodeBase64(user.getUsername(), secret));
            return "mfa/setup";
        }
        
        // TOTPコード検証
        if (!totpService.verifyCode(secret, code)) {
            model.addAttribute("error", "認証コードが正しくありません");
            model.addAttribute("secret", secret);
            model.addAttribute("qrCode", totpService.generateQrCodeBase64(user.getUsername(), secret));
            return "mfa/setup";
        }
        
        // MFA有効化
        user.setMfaEnabled(true);
        user.setMfaSecret(secret);
        userRepository.save(user);
        
        // バックアップコード生成
        String[] backupCodes = totpService.generateBackupCodes(BACKUP_CODE_COUNT);
        for (String backupCode : backupCodes) {
            BackupCode bc = BackupCode.builder()
                .user(user)
                .code(passwordEncoder.encode(backupCode))
                .used(false)
                .build();
            backupCodeRepository.save(bc);
        }
        
        // 監査ログ
        auditLogService.logSuccess(EventType.MFA_ENABLED, user.getUsername());
        log.info("MFA有効化: username={}", user.getUsername());
        
        model.addAttribute("backupCodes", Arrays.asList(backupCodes));
        return "mfa/backup-codes";
    }
    
    /**
     * MFA管理画面
     */
    @GetMapping("/manage")
    public String manageMfa(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        
        if (!user.isMfaEnabled()) {
            return "redirect:/mfa/setup";
        }
        
        long unusedBackupCodes = backupCodeRepository.countByUserAndUsedFalse(user);
        
        model.addAttribute("user", user);
        model.addAttribute("unusedBackupCodes", unusedBackupCodes);
        
        return "mfa/manage";
    }
    
    /**
     * MFA無効化処理
     */
    @PostMapping("/disable")
    @Transactional
    public String disableMfa(@AuthenticationPrincipal UserDetails userDetails,
                            @RequestParam String password,
                            RedirectAttributes redirectAttributes) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        
        // パスワード確認
        if (!passwordEncoder.matches(password, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "パスワードが正しくありません");
            return "redirect:/mfa/manage";
        }
        
        // MFA無効化
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
        
        // バックアップコード削除
        backupCodeRepository.deleteAllByUser(user);
        
        // 監査ログ
        auditLogService.logSuccess(EventType.MFA_DISABLED, user.getUsername());
        log.info("MFA無効化: username={}", user.getUsername());
        
        redirectAttributes.addFlashAttribute("success", "二要素認証を無効化しました");
        return "redirect:/home";
    }
    
    /**
     * 新しいバックアップコード生成
     */
    @PostMapping("/regenerate-backup-codes")
    @Transactional
    public String regenerateBackupCodes(@AuthenticationPrincipal UserDetails userDetails,
                                       @RequestParam String password,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        
        // パスワード確認
        if (!passwordEncoder.matches(password, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "パスワードが正しくありません");
            return "redirect:/mfa/manage";
        }
        
        // 既存のバックアップコード削除
        backupCodeRepository.deleteAllByUser(user);
        
        // 新しいバックアップコード生成
        String[] backupCodes = totpService.generateBackupCodes(BACKUP_CODE_COUNT);
        for (String backupCode : backupCodes) {
            BackupCode bc = BackupCode.builder()
                .user(user)
                .code(passwordEncoder.encode(backupCode))
                .used(false)
                .build();
            backupCodeRepository.save(bc);
        }
        
        log.info("バックアップコード再生成: username={}", user.getUsername());
        
        model.addAttribute("backupCodes", Arrays.asList(backupCodes));
        model.addAttribute("regenerated", true);
        return "mfa/backup-codes";
    }
}