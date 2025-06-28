package com.example.demo.controller;

import com.example.demo.entity.BackupCode;
import com.example.demo.entity.User;
import com.example.demo.entity.AuditLog.EventType;
import com.example.demo.repository.BackupCodeRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuditLogService;
import com.example.demo.service.TotpService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

/**
 * MFA検証コントローラー
 * ログイン後の追加認証を処理
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class MfaVerificationController {
    
    private final UserRepository userRepository;
    private final BackupCodeRepository backupCodeRepository;
    private final TotpService totpService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final HttpSession httpSession;
    
    /**
     * MFA検証画面表示
     */
    @GetMapping("/mfa/verify")
    public String showMfaVerification(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        
        // MFAが無効な場合はホームへ
        if (!user.isMfaEnabled()) {
            httpSession.setAttribute("MFA_VERIFIED", true);
            return "redirect:/home";
        }
        
        // 既に検証済みの場合はホームへ
        Boolean mfaVerified = (Boolean) httpSession.getAttribute("MFA_VERIFIED");
        if (mfaVerified != null && mfaVerified) {
            return "redirect:/home";
        }
        
        model.addAttribute("username", user.getUsername());
        return "mfa/verify";
    }
    
    /**
     * TOTPコード検証
     */
    @PostMapping("/mfa/verify")
    @Transactional
    public String verifyMfa(@AuthenticationPrincipal UserDetails userDetails,
                           @RequestParam String code,
                           @RequestParam(required = false) boolean useBackupCode,
                           Model model) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        
        boolean verified = false;
        String verificationMethod = "";
        
        if (useBackupCode) {
            // バックアップコードで検証
            verified = verifyBackupCode(user, code);
            verificationMethod = "バックアップコード";
        } else {
            // TOTPコードで検証
            verified = totpService.verifyCode(user.getMfaSecret(), code);
            verificationMethod = "TOTP";
        }
        
        if (verified) {
            // セッションにMFA検証済みフラグを設定
            httpSession.setAttribute("MFA_VERIFIED", true);
            
            // 監査ログ
            auditLogService.logEvent(EventType.MFA_SUCCESS, user.getUsername(), true, 
                verificationMethod + "による認証成功");
            log.info("MFA認証成功: username={}, method={}", user.getUsername(), verificationMethod);
            
            return "redirect:/home";
        } else {
            // 監査ログ
            auditLogService.logEvent(EventType.MFA_FAILURE, user.getUsername(), false, 
                verificationMethod + "による認証失敗");
            log.warn("MFA認証失敗: username={}, method={}", user.getUsername(), verificationMethod);
            
            model.addAttribute("error", "認証コードが正しくありません");
            model.addAttribute("username", user.getUsername());
            return "mfa/verify";
        }
    }
    
    /**
     * バックアップコードの検証
     * @param user ユーザー
     * @param code 入力されたコード
     * @return 検証結果
     */
    private boolean verifyBackupCode(User user, String code) {
        // 未使用のバックアップコードを取得
        for (BackupCode backupCode : backupCodeRepository.findByUserAndUsedFalse(user)) {
            if (passwordEncoder.matches(code, backupCode.getCode())) {
                // コードを使用済みにする
                backupCode.setUsed(true);
                backupCode.setUsedAt(LocalDateTime.now());
                backupCodeRepository.save(backupCode);
                
                log.info("バックアップコード使用: username={}", user.getUsername());
                return true;
            }
        }
        return false;
    }
}