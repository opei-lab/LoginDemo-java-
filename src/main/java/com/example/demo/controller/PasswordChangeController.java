package com.example.demo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.impl.UserServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * パスワード変更コントローラー
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PasswordChangeController {
    
    private final UserRepository userRepository;
    private final UserServiceImpl userService;
    
    /**
     * パスワード変更画面表示
     */
    @GetMapping("/change-password")
    public String showChangePasswordForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("username", userDetails.getUsername());
        return "change-password";
    }
    
    /**
     * パスワード変更処理
     */
    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam String currentPassword,
                               @RequestParam String newPassword,
                               @RequestParam String confirmPassword,
                               RedirectAttributes redirectAttributes) {
        
        try {
            // パスワード確認
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "新しいパスワードが一致しません");
                return "redirect:/change-password";
            }
            
            // ユーザー取得
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
            
            // パスワード変更
            userService.changePassword(user, currentPassword, newPassword);
            
            log.info("パスワード変更成功: username={}", user.getUsername());
            redirectAttributes.addFlashAttribute("success", "パスワードが正常に変更されました");
            return "redirect:/home";
            
        } catch (IllegalArgumentException e) {
            log.warn("パスワード変更失敗: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/change-password";
        } catch (Exception e) {
            log.error("パスワード変更エラー", e);
            redirectAttributes.addFlashAttribute("error", "パスワード変更に失敗しました");
            return "redirect:/change-password";
        }
    }
}