package com.example.demo.security;

import com.example.demo.dto.LoginContext;
import com.example.demo.dto.RiskAssessmentResult;
import com.example.demo.service.RiskAssessmentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * OAuth2認証成功ハンドラー
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    
    private final RiskAssessmentService riskAssessmentService;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String username = oauth2User.getAttribute("username");
        String provider = oauth2User.getAttribute("provider");
        
        log.info("OAuth2認証成功: ユーザー={}, プロバイダー={}", username, provider);
        
        // リスク評価用のコンテキストを構築
        LoginContext context = buildLoginContext(request);
        
        // リスク評価を実行
        RiskAssessmentResult riskResult = riskAssessmentService.assessLoginRisk(username, context);
        
        // ログイン試行を記録
        riskAssessmentService.recordLoginAttempt(username, context, true,
            riskResult.getRiskScore(), String.join(", ", riskResult.getRiskFactors()));
        
        // リスクレベルが高い場合は追加認証へ
        if (riskResult.isRequiresAdditionalVerification()) {
            request.getSession().setAttribute("requiresAdditionalVerification", true);
            request.getSession().setAttribute("riskAssessmentResult", riskResult);
            request.getSession().setAttribute("oauth2Authentication", true);
            request.getSession().setAttribute("username", username);
            
            // 追加認証ページへリダイレクト
            getRedirectStrategy().sendRedirect(request, response, "/auth/additional-verification");
            return;
        }
        
        // 低リスクの場合は通常の処理を継続
        super.onAuthenticationSuccess(request, response, authentication);
    }
    
    /**
     * ログインコンテキストを構築
     */
    private LoginContext buildLoginContext(HttpServletRequest request) {
        LocalDateTime now = LocalDateTime.now();
        
        return LoginContext.builder()
            .ipAddress(getClientIpAddress(request))
            .userAgent(request.getHeader("User-Agent"))
            .deviceFingerprint(request.getParameter("deviceFingerprint"))
            .hourOfDay(now.getHour())
            .dayOfWeek(now.getDayOfWeek().getValue() % 7)
            .build();
    }
    
    /**
     * クライアントのIPアドレスを取得
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