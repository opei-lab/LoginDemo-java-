package com.example.demo.security;

import com.example.demo.dto.LoginContext;
import com.example.demo.dto.RiskAssessmentResult;
import com.example.demo.service.RiskAssessmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * リスクベース認証フィルター
 */
@Slf4j
public class RiskBasedAuthenticationFilter extends AbstractAuthenticationProcessingFilter {
    
    private final RiskAssessmentService riskAssessmentService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public RiskBasedAuthenticationFilter(String defaultFilterProcessesUrl, 
                                       AuthenticationManager authenticationManager,
                                       RiskAssessmentService riskAssessmentService) {
        super(new PostRequestMatcher(defaultFilterProcessesUrl));
        setAuthenticationManager(authenticationManager);
        this.riskAssessmentService = riskAssessmentService;
    }
    
    /**
     * POSTリクエストのみを許可するRequestMatcher
     */
    private static class PostRequestMatcher implements RequestMatcher {
        private final String pattern;
        
        public PostRequestMatcher(String pattern) {
            this.pattern = pattern;
        }
        
        @Override
        public boolean matches(HttpServletRequest request) {
            return "POST".equalsIgnoreCase(request.getMethod()) && 
                   request.getServletPath().equals(pattern);
        }
    }
    
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, 
                                              HttpServletResponse response) 
            throws AuthenticationException, IOException {
        
        String username = obtainUsername(request);
        String password = obtainPassword(request);
        
        if (username == null) username = "";
        if (password == null) password = "";
        
        username = username.trim();
        
        // ログインコンテキストを構築
        LoginContext context = buildLoginContext(request);
        
        // リスク評価を実行
        RiskAssessmentResult riskResult = riskAssessmentService.assessLoginRisk(username, context);
        
        log.info("リスク評価結果: ユーザー={}, スコア={}, レベル={}", 
                username, riskResult.getRiskScore(), riskResult.getRiskLevel());
        
        // リスクが高すぎる場合はブロック
        if (riskResult.getRiskLevel() == RiskAssessmentResult.RiskLevel.CRITICAL) {
            // ログイン試行を記録
            riskAssessmentService.recordLoginAttempt(username, context, false, 
                riskResult.getRiskScore(), String.join(", ", riskResult.getRiskFactors()));
            
            throw new RiskBasedAuthenticationException("アクセスがブロックされました", riskResult);
        }
        
        // リスク情報をリクエストに保存（後続の処理で使用）
        request.setAttribute("riskAssessmentResult", riskResult);
        request.setAttribute("loginContext", context);
        
        // 通常の認証処理を実行
        UsernamePasswordAuthenticationToken authRequest = 
            new UsernamePasswordAuthenticationToken(username, password);
        
        setDetails(request, authRequest);
        
        return this.getAuthenticationManager().authenticate(authRequest);
    }
    
    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                          HttpServletResponse response,
                                          FilterChain chain,
                                          Authentication authResult) throws IOException, ServletException {
        
        // リスク評価結果を取得
        RiskAssessmentResult riskResult = 
            (RiskAssessmentResult) request.getAttribute("riskAssessmentResult");
        LoginContext context = 
            (LoginContext) request.getAttribute("loginContext");
        
        String username = authResult.getName();
        
        // ログイン成功を記録
        riskAssessmentService.recordLoginAttempt(username, context, true,
            riskResult.getRiskScore(), String.join(", ", riskResult.getRiskFactors()));
        
        // 追加認証が必要な場合
        if (riskResult.isRequiresAdditionalVerification()) {
            // セッションに追加認証情報を保存
            request.getSession().setAttribute("requiresAdditionalVerification", true);
            request.getSession().setAttribute("riskAssessmentResult", riskResult);
            request.getSession().setAttribute("primaryAuthenticationSuccess", true);
            
            // 追加認証ページへリダイレクト
            response.sendRedirect("/auth/additional-verification");
            return;
        }
        
        // 低リスクの場合はデバイスを信頼済みとして登録することを検討
        if (riskResult.getRiskLevel() == RiskAssessmentResult.RiskLevel.LOW && 
            context.getDeviceFingerprint() != null) {
            riskAssessmentService.trustDevice(username, context.getDeviceFingerprint(), 
                context.getUserAgent());
        }
        
        // 通常のログイン成功処理
        super.successfulAuthentication(request, response, chain, authResult);
    }
    
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            AuthenticationException failed) throws IOException {
        
        // リスク評価結果を取得
        RiskAssessmentResult riskResult = 
            (RiskAssessmentResult) request.getAttribute("riskAssessmentResult");
        LoginContext context = 
            (LoginContext) request.getAttribute("loginContext");
        
        String username = obtainUsername(request);
        
        // ログイン失敗を記録
        if (riskResult != null && context != null) {
            riskAssessmentService.recordLoginAttempt(username, context, false,
                riskResult.getRiskScore(), String.join(", ", riskResult.getRiskFactors()));
        }
        
        // エラーレスポンスを作成
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Authentication failed");
        errorResponse.put("message", failed.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        
        if (failed instanceof RiskBasedAuthenticationException) {
            RiskBasedAuthenticationException riskException = (RiskBasedAuthenticationException) failed;
            errorResponse.put("riskLevel", riskException.getRiskResult().getRiskLevel());
            errorResponse.put("riskFactors", riskException.getRiskResult().getRiskFactors());
        }
        
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
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
            // 実際の実装では、IPジオロケーションサービスを使用して以下を取得
            .countryCode(null) // TODO: IP Geolocation APIを使用
            .city(null) // TODO: IP Geolocation APIを使用
            .isProxy(false) // TODO: IP評価サービスを使用
            .isVpn(false) // TODO: IP評価サービスを使用
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
    
    private String obtainUsername(HttpServletRequest request) {
        return request.getParameter("username");
    }
    
    private String obtainPassword(HttpServletRequest request) {
        return request.getParameter("password");
    }
    
    private void setDetails(HttpServletRequest request, 
                          UsernamePasswordAuthenticationToken authRequest) {
        authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
    }
    
    /**
     * リスクベース認証例外
     */
    public static class RiskBasedAuthenticationException extends AuthenticationException {
        private final RiskAssessmentResult riskResult;
        
        public RiskBasedAuthenticationException(String msg, RiskAssessmentResult riskResult) {
            super(msg);
            this.riskResult = riskResult;
        }
        
        public RiskAssessmentResult getRiskResult() {
            return riskResult;
        }
    }
}