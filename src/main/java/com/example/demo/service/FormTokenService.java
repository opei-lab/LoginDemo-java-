package com.example.demo.service;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * フォームトークンサービス
 * 二重送信防止のためのワンタイムトークンを管理
 */
@Service
@Slf4j
public class FormTokenService {
    
    private static final String TOKEN_SESSION_PREFIX = "FORM_TOKEN_";
    
    /**
     * 新しいフォームトークンを生成してセッションに保存
     * 
     * @param session HTTPセッション
     * @param formName フォーム名
     * @return 生成されたトークン
     */
    public String generateToken(HttpSession session, String formName) {
        String token = UUID.randomUUID().toString();
        String sessionKey = TOKEN_SESSION_PREFIX + formName;
        
        session.setAttribute(sessionKey, token);
        log.debug("フォームトークン生成: form={}, token={}", formName, token);
        
        return token;
    }
    
    /**
     * トークンを検証し、有効な場合はセッションから削除
     * 
     * @param session HTTPセッション
     * @param formName フォーム名
     * @param submittedToken 送信されたトークン
     * @return トークンが有効な場合true
     */
    public boolean validateAndRemoveToken(HttpSession session, String formName, String submittedToken) {
        if (submittedToken == null || submittedToken.isEmpty()) {
            log.warn("トークン検証失敗: トークンが空です form={}", formName);
            return false;
        }
        
        String sessionKey = TOKEN_SESSION_PREFIX + formName;
        String sessionToken = (String) session.getAttribute(sessionKey);
        
        if (sessionToken == null) {
            log.warn("トークン検証失敗: セッションにトークンが存在しません form={}", formName);
            return false;
        }
        
        if (!sessionToken.equals(submittedToken)) {
            log.warn("トークン検証失敗: トークンが一致しません form={}", formName);
            return false;
        }
        
        // 使用済みトークンを削除（ワンタイム）
        session.removeAttribute(sessionKey);
        log.debug("トークン検証成功: form={}", formName);
        
        return true;
    }
}