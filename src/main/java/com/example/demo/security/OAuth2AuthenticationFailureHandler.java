package com.example.demo.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2認証失敗ハンドラー
 */
@Component
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                      AuthenticationException exception) throws IOException, ServletException {
        
        log.error("OAuth2認証失敗: {}", exception.getMessage());
        
        // エラーメッセージをセッションに保存
        request.getSession().setAttribute("oauth2Error", "ソーシャルログインに失敗しました。再度お試しください。");
        
        // ログインページへリダイレクト
        getRedirectStrategy().sendRedirect(request, response, "/login?oauth2error");
    }
}