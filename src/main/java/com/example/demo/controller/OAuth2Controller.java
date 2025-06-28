package com.example.demo.controller;

import com.example.demo.entity.OAuth2UserLink;
import com.example.demo.entity.User;
import com.example.demo.repository.OAuth2UserLinkRepository;
import com.example.demo.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * OAuth2連携管理コントローラー
 */
@Controller
@RequestMapping("/oauth2")
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {
    
    private final IUserService userService;
    private final OAuth2UserLinkRepository oauth2UserLinkRepository;
    
    /**
     * OAuth2連携管理画面
     */
    @GetMapping("/manage")
    public String manageOAuth2Links(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        
        // 連携済みのプロバイダーを取得
        List<OAuth2UserLink> links = oauth2UserLinkRepository.findByUserAndIsActiveTrue(user);
        
        // 利用可能なプロバイダー
        List<String> availableProviders = List.of("google", "github", "microsoft");
        
        model.addAttribute("links", links);
        model.addAttribute("availableProviders", availableProviders);
        model.addAttribute("user", user);
        
        return "oauth2/manage";
    }
    
    /**
     * OAuth2連携を削除
     */
    @PostMapping("/unlink/{linkId}")
    public String unlinkOAuth2Provider(@PathVariable Long linkId,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        
        User user = userService.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        
        OAuth2UserLink link = oauth2UserLinkRepository.findById(linkId)
            .orElseThrow(() -> new RuntimeException("連携情報が見つかりません"));
        
        // 権限チェック
        if (!link.getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "権限がありません");
            return "redirect:/oauth2/manage";
        }
        
        // 連携を無効化
        link.setActive(false);
        oauth2UserLinkRepository.save(link);
        
        log.info("OAuth2連携を解除: ユーザー={}, プロバイダー={}", 
                user.getUsername(), link.getProvider());
        
        redirectAttributes.addFlashAttribute("success", 
            link.getProvider() + "との連携を解除しました");
        
        return "redirect:/oauth2/manage";
    }
    
    /**
     * OAuth2連携開始（既存アカウントに連携追加）
     */
    @GetMapping("/link/{provider}")
    public String linkOAuth2Provider(@PathVariable String provider,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        
        // 認証されていない場合はログインページへ
        if (userDetails == null) {
            return "redirect:/login";
        }
        
        // OAuth2認証フローへリダイレクト
        return "redirect:/oauth2/authorization/" + provider;
    }
}