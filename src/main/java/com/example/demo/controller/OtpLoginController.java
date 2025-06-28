package com.example.demo.controller;

import com.example.demo.entity.OneTimePassword;
import com.example.demo.entity.User;
import com.example.demo.service.AuditLogService;
import com.example.demo.service.IUserService;
import com.example.demo.service.OtpService;
import com.example.demo.entity.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * OTPログインコントローラー
 * メールアドレスとワンタイムパスワードによるログイン機能
 */
@Controller
@RequestMapping("/auth/otp")
@RequiredArgsConstructor
@Slf4j
public class OtpLoginController {
    
    private final IUserService userService;
    private final OtpService otpService;
    private final AuditLogService auditLogService;
    
    // OTP送信のレート制限（同一IPから1分間に3回まで）
    private final Map<String, Integer> rateLimitMap = new HashMap<>();
    private final Map<String, LocalDateTime> rateLimitTimeMap = new HashMap<>();
    private static final int MAX_ATTEMPTS_PER_MINUTE = 3;
    
    /**
     * OTPログイン画面表示
     */
    @GetMapping("/login")
    public String showOtpLogin(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("otpSent", false);
        if (email != null) {
            model.addAttribute("email", email);
        }
        return "auth/otp-login";
    }
    
    /**
     * OTP送信
     */
    @PostMapping("/send")
    public String sendOtp(@RequestParam String email, 
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        
        String clientIp = getClientIpAddress(request);
        
        try {
            // レート制限チェック
            if (!checkRateLimit(clientIp)) {
                redirectAttributes.addFlashAttribute("error", 
                    "短時間に多くのリクエストが送信されました。しばらく待ってから再試行してください。");
                return "redirect:/auth/otp/login";
            }
            
            // メールアドレスの検証
            User user = userService.findByEmail(email)
                .orElseThrow(() -> {
                    // セキュリティのため、登録されていない場合も同じメッセージを表示
                    auditLogService.logFailure(AuditLog.EventType.OTP_REQUEST_FAILED, 
                        "unknown", "未登録のメールアドレス: " + maskEmail(email));
                    return new RuntimeException("メールアドレスが見つかりません");
                });
            
            // アカウント状態チェック
            if (user.isAccountLocked()) {
                auditLogService.logFailure(AuditLog.EventType.OTP_REQUEST_FAILED, 
                    user.getUsername(), "アカウントロック中");
                redirectAttributes.addFlashAttribute("error", 
                    "アカウントがロックされています。しばらく待ってから再試行してください。");
                return "redirect:/auth/otp/login";
            }
            
            if (!user.isEnabled()) {
                auditLogService.logFailure(AuditLog.EventType.OTP_REQUEST_FAILED, 
                    user.getUsername(), "アカウント無効");
                redirectAttributes.addFlashAttribute("error", "アカウントが無効です。");
                return "redirect:/auth/otp/login";
            }
            
            // OTP生成・送信
            otpService.generateAndSendOtp(user, OneTimePassword.OtpPurpose.LOGIN);
            
            // 監査ログ
            auditLogService.logSuccess(AuditLog.EventType.OTP_SENT, user.getUsername());
            
            // 成功画面へ
            model.addAttribute("otpSent", true);
            model.addAttribute("email", email);
            model.addAttribute("maskedEmail", maskEmail(email));
            model.addAttribute("success", "認証コードを送信しました");
            
            return "auth/otp-login";
            
        } catch (Exception e) {
            log.error("OTP送信エラー: email={}", maskEmail(email), e);
            
            // 開発環境では詳細なエラーメッセージを表示
            String errorMessage = "認証コードの送信に失敗しました。";
            if ("dev".equals(System.getProperty("spring.profiles.active", "dev"))) {
                errorMessage += " 詳細: " + e.getMessage();
            } else {
                errorMessage += " メールアドレスを確認してください。";
            }
            
            redirectAttributes.addFlashAttribute("error", errorMessage);
            return "redirect:/auth/otp/login";
        }
    }
    
    /**
     * OTP検証・ログイン
     */
    @PostMapping("/verify")
    public String verifyOtp(@RequestParam String email,
                          @RequestParam String code,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        
        try {
            // ユーザー検索
            User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
            
            // OTP検証
            boolean isValid = otpService.verifyOtp(user, code, OneTimePassword.OtpPurpose.LOGIN);
            
            if (isValid) {
                // ログイン処理
                UserDetails userDetails = userService.loadUserByUsername(user.getUsername());
                UsernamePasswordAuthenticationToken auth = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
                
                // セッション設定
                session.setAttribute("username", user.getUsername());
                
                // 最終ログイン日時を更新
                user.setLastLoginAt(LocalDateTime.now());
                userService.save(user);
                
                // 監査ログ
                auditLogService.logEvent(AuditLog.EventType.LOGIN_SUCCESS, 
                    user.getUsername(), true, "OTPログイン");
                
                log.info("OTPログイン成功: username={}", user.getUsername());
                
                return "redirect:/home";
            } else {
                // 検証失敗
                auditLogService.logFailure(AuditLog.EventType.LOGIN_FAILURE, 
                    user.getUsername(), "無効なOTP");
                
                redirectAttributes.addFlashAttribute("error", "認証コードが正しくありません");
                redirectAttributes.addFlashAttribute("email", email);
                redirectAttributes.addFlashAttribute("otpSent", true);
                redirectAttributes.addFlashAttribute("maskedEmail", maskEmail(email));
                
                return "redirect:/auth/otp/login";
            }
            
        } catch (Exception e) {
            log.error("OTP検証エラー", e);
            redirectAttributes.addFlashAttribute("error", "ログインに失敗しました");
            return "redirect:/auth/otp/login";
        }
    }
    
    /**
     * OTP再送信
     */
    @PostMapping("/resend")
    public String resendOtp(@RequestParam String email,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        
        // 通常の送信と同じ処理を実行
        return sendOtp(email, request, redirectAttributes, null);
    }
    
    /**
     * レート制限チェック
     */
    private boolean checkRateLimit(String clientIp) {
        LocalDateTime now = LocalDateTime.now();
        
        // 前回のチェック時刻を確認
        LocalDateTime lastCheck = rateLimitTimeMap.get(clientIp);
        if (lastCheck == null || lastCheck.isBefore(now.minusMinutes(1))) {
            // 1分以上経過していればカウントリセット
            rateLimitMap.put(clientIp, 1);
            rateLimitTimeMap.put(clientIp, now);
            return true;
        }
        
        // カウントを増やす
        int count = rateLimitMap.getOrDefault(clientIp, 0) + 1;
        rateLimitMap.put(clientIp, count);
        
        return count <= MAX_ATTEMPTS_PER_MINUTE;
    }
    
    /**
     * メールアドレスをマスク（セキュリティ対策）
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 3) {
            return "***@" + domain;
        } else {
            return localPart.substring(0, 3) + "***@" + domain;
        }
    }
    
    /**
     * クライアントIPアドレス取得
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}