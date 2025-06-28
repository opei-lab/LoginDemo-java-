package com.example.demo.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;

/**
 * グローバル例外ハンドラー
 * アプリケーション全体の例外を一元的に処理
 * 
 * @ControllerAdviceは、すべてのコントローラーに対する横断的な処理を定義
 * ここで捕捉した例外は「大域脱出」のように、呼び出し元に戻らずここで処理される
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * ビジネスロジック例外のハンドリング
     */
    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(BusinessException e, Model model, HttpServletRequest request) {
        log.warn("ビジネスエラー: {}", e.getMessage());
        
        model.addAttribute("error", e.getMessage());
        model.addAttribute("errorCode", e.getErrorCode());
        
        // リクエストパスに応じて適切なページへ遷移
        String path = request.getRequestURI();
        if (path.contains("/register")) {
            return "register";
        } else if (path.contains("/login")) {
            return "login";
        }
        
        return "error/business-error";
    }
    
    /**
     * 認証関連例外のハンドリング
     */
    @ExceptionHandler(AuthenticationException.class)
    public String handleAuthenticationException(AuthenticationException e, 
                                              RedirectAttributes redirectAttributes) {
        log.warn("認証エラー: {}", e.getMessage());
        
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/login";
    }
    
    /**
     * レート制限例外のハンドリング
     */
    @ExceptionHandler(RateLimitException.class)
    public String handleRateLimitException(RateLimitException e, Model model) {
        log.warn("レート制限: {}", e.getMessage());
        
        model.addAttribute("error", "アクセスが制限されています。しばらく待ってから再試行してください。");
        model.addAttribute("retryAfter", e.getRetryAfterSeconds());
        
        return "error/rate-limit";
    }
    
    /**
     * その他の予期しない例外のハンドリング
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception e, Model model) {
        log.error("予期しないエラー", e);
        
        // 本番環境では詳細なエラー情報を隠す
        model.addAttribute("error", "システムエラーが発生しました。");
        model.addAttribute("errorId", System.currentTimeMillis()); // エラー追跡用ID
        
        return "error/system-error";
    }
}