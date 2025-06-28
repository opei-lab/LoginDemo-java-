package com.example.demo.security;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * MFA認証フィルター
 * MFAが有効なユーザーに対して追加認証を要求
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MfaAuthenticationFilter extends OncePerRequestFilter {
    
    private final UserRepository userRepository;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // 除外パス
        String path = request.getRequestURI();
        if (path.startsWith("/login") || path.startsWith("/mfa/verify") || 
            path.startsWith("/css") || path.startsWith("/js") || 
            path.startsWith("/logout") || path.startsWith("/register")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getPrincipal())) {
            
            String username = authentication.getName();
            User user = userRepository.findByUsername(username).orElse(null);
            
            if (user != null && user.isMfaEnabled()) {
                // セッションでMFA検証済みかチェック
                Boolean mfaVerified = (Boolean) request.getSession().getAttribute("MFA_VERIFIED");
                
                if (mfaVerified == null || !mfaVerified) {
                    // MFA検証画面にリダイレクト
                    response.sendRedirect("/mfa/verify");
                    return;
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }
}