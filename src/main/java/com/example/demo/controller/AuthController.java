package com.example.demo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.IUserService;
import com.example.demo.service.FormTokenService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final IUserService userService;
    private final UserRepository userRepository;
    private final FormTokenService formTokenService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegister(Model model, HttpSession session) {
        model.addAttribute("user", new User());
        // フォームトークンを生成
        String token = formTokenService.generateToken(session, "register");
        model.addAttribute("formToken", token);
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute User user, 
                         @RequestParam String confirmPassword,
                         @RequestParam(name = "formToken", required = false) String formToken,
                         HttpSession session,
                         Model model) {
        try {
            // トークン検証
            if (!formTokenService.validateAndRemoveToken(session, "register", formToken)) {
                log.warn("二重送信検出: username={}", user.getUsername());
                model.addAttribute("error", "二重送信が検出されました。もう一度お試しください。");
                model.addAttribute("formToken", formTokenService.generateToken(session, "register"));
                return "register";
            }
            // パスワード確認チェック
            if (!user.getPassword().equals(confirmPassword)) {
                model.addAttribute("error", "パスワードが一致しません");
                model.addAttribute("formToken", formTokenService.generateToken(session, "register"));
                return "register";
            }
            
            userService.register(user);
            log.info("新規ユーザー登録成功: username={}", user.getUsername());
            return "redirect:/login?registered";
            
        } catch (IllegalArgumentException e) {
            log.warn("ユーザー登録失敗: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("formToken", formTokenService.generateToken(session, "register"));
            return "register";
        } catch (Exception e) {
            log.error("ユーザー登録エラー", e);
            model.addAttribute("error", "アカウント作成に失敗しました");
            model.addAttribute("formToken", formTokenService.generateToken(session, "register"));
            return "register";
        }
    }

    @GetMapping("/home")
    public String home(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        
        model.addAttribute("user", user);
        return "home";
    }
}
