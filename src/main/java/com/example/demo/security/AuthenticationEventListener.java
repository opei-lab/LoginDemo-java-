package com.example.demo.security;

import com.example.demo.entity.AuditLog.EventType;
import com.example.demo.service.AuditLogService;
import com.example.demo.service.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * 認証イベントリスナー
 * ログイン成功・失敗時の処理を行う
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationEventListener {
    
    private final UserServiceImpl userService;
    private final AuditLogService auditLogService;
    
    /**
     * ログイン成功時の処理
     * @param event 認証成功イベント
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        userService.handleLoginSuccess(username);
        auditLogService.logSuccess(EventType.LOGIN_SUCCESS, username);
    }
    
    /**
     * ログイン失敗時の処理
     * @param event 認証失敗イベント
     */
    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        userService.handleLoginFailure(username);
        auditLogService.logFailure(EventType.LOGIN_FAILURE, username, "認証失敗");
    }
    
    /**
     * ログアウト成功時の処理
     * @param event ログアウト成功イベント
     */
    @EventListener
    public void onLogoutSuccess(LogoutSuccessEvent event) {
        String username = event.getAuthentication().getName();
        auditLogService.logSuccess(EventType.LOGOUT, username);
    }
}