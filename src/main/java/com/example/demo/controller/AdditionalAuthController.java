package com.example.demo.controller;

import com.example.demo.dto.LoginContext;
import com.example.demo.dto.RiskAssessmentResult;
import com.example.demo.entity.OneTimePassword;
import com.example.demo.entity.User;
import com.example.demo.service.OtpService;
import com.example.demo.service.RiskAssessmentService;
import com.example.demo.service.TotpService;
import com.example.demo.service.IUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 追加認証コントローラー
 */
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AdditionalAuthController {
    
    private final IUserService userService;
    private final TotpService totpService;
    private final OtpService otpService;
    private final RiskAssessmentService riskAssessmentService;
    
    /**
     * 追加認証画面表示
     */
    @GetMapping("/additional-verification")
    public String showAdditionalVerification(HttpSession session, Model model) {
        // プライマリ認証が成功していない場合はログイン画面へ
        Boolean primaryAuthSuccess = (Boolean) session.getAttribute("primaryAuthenticationSuccess");
        if (primaryAuthSuccess == null || !primaryAuthSuccess) {
            return "redirect:/login";
        }
        
        // リスク評価結果を取得
        RiskAssessmentResult riskResult = (RiskAssessmentResult) session.getAttribute("riskAssessmentResult");
        if (riskResult == null) {
            return "redirect:/login";
        }
        
        return "auth/additional-verification";
    }
    
    /**
     * 追加認証の検証
     */
    @PostMapping("/verify-additional")
    public String verifyAdditional(
            @RequestParam String verificationType,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) boolean trustDevice,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        // セッションから情報を取得
        Boolean primaryAuthSuccess = (Boolean) session.getAttribute("primaryAuthenticationSuccess");
        if (primaryAuthSuccess == null || !primaryAuthSuccess) {
            return "redirect:/login";
        }
        
        String username = (String) session.getAttribute("username");
        if (username == null) {
            // セキュリティコンテキストから取得を試みる
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                username = auth.getName();
            } else {
                return "redirect:/login";
            }
        }
        
        boolean verificationSuccess = false;
        
        // 認証タイプに応じて検証
        switch (verificationType) {
            case "TOTP":
                verificationSuccess = verifyTotp(username, code);
                break;
            case "EMAIL_OTP":
                verificationSuccess = verifyEmailOtp(username, code);
                break;
            case "SECURITY_QUESTIONS":
                // TODO: セキュリティ質問の実装
                verificationSuccess = false;
                break;
            default:
                log.warn("不明な認証タイプ: {}", verificationType);
        }
        
        if (verificationSuccess) {
            // 認証成功
            completeAuthentication(username, session);
            
            // デバイスを信頼する場合
            if (trustDevice) {
                LoginContext context = (LoginContext) session.getAttribute("loginContext");
                if (context != null && context.getDeviceFingerprint() != null) {
                    riskAssessmentService.trustDevice(username, 
                        context.getDeviceFingerprint(), context.getUserAgent());
                }
            }
            
            // セッションクリーンアップ
            session.removeAttribute("requiresAdditionalVerification");
            session.removeAttribute("riskAssessmentResult");
            session.removeAttribute("primaryAuthenticationSuccess");
            session.removeAttribute("loginContext");
            
            return "redirect:/home";
        } else {
            // 認証失敗
            redirectAttributes.addFlashAttribute("error", "認証に失敗しました");
            return "redirect:/auth/additional-verification";
        }
    }
    
    /**
     * メールOTP送信
     */
    @PostMapping("/send-email-otp")
    public String sendEmailOtp(HttpSession session, RedirectAttributes redirectAttributes) {
        String username = getUsername(session);
        if (username == null) {
            return "redirect:/login";
        }
        
        try {
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
            otpService.generateAndSendOtp(user, OneTimePassword.OtpPurpose.LOGIN);
            redirectAttributes.addFlashAttribute("success", "認証コードをメールで送信しました");
            return "redirect:/auth/verify-email-otp";
        } catch (Exception e) {
            log.error("OTP送信エラー", e);
            redirectAttributes.addFlashAttribute("error", "認証コードの送信に失敗しました");
            return "redirect:/auth/additional-verification";
        }
    }
    
    /**
     * メールOTP検証画面
     */
    @GetMapping("/verify-email-otp")
    public String showVerifyEmailOtp(HttpSession session) {
        if (getUsername(session) == null) {
            return "redirect:/login";
        }
        return "auth/verify-email-otp";
    }
    
    /**
     * TOTP検証
     */
    private boolean verifyTotp(String username, String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        
        try {
            return totpService.verifyCode(username, code);
        } catch (Exception e) {
            log.error("TOTP検証エラー", e);
            return false;
        }
    }
    
    /**
     * メールOTP検証
     */
    private boolean verifyEmailOtp(String username, String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        
        try {
            User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
            return otpService.verifyOtp(user, code, OneTimePassword.OtpPurpose.LOGIN);
        } catch (Exception e) {
            log.error("メールOTP検証エラー", e);
            return false;
        }
    }
    
    /**
     * 認証完了処理
     */
    private void completeAuthentication(String username, HttpSession session) {
        try {
            UserDetails userDetails = userService.loadUserByUsername(username);
            Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            log.info("追加認証完了: {}", username);
        } catch (Exception e) {
            log.error("認証完了処理エラー", e);
        }
    }
    
    /**
     * セッションからユーザー名を取得
     */
    private String getUsername(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                username = auth.getName();
            }
        }
        return username;
    }
}