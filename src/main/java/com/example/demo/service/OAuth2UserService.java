package com.example.demo.service;

import com.example.demo.entity.OAuth2UserLink;
import com.example.demo.entity.User;
import com.example.demo.repository.OAuth2UserLinkRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * OAuth2ユーザーサービス
 * OAuth2プロバイダーからのユーザー情報を処理
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserService extends DefaultOAuth2UserService {
    
    private final UserRepository userRepository;
    private final OAuth2UserLinkRepository oauth2UserLinkRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // デフォルトのOAuth2ユーザー情報を取得
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        // プロバイダー情報を取得
        String provider = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        // プロバイダー固有のユーザー情報を抽出
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(provider, attributes);
        
        log.info("OAuth2ログイン試行: プロバイダー={}, メール={}", provider, userInfo.getEmail());
        
        // 既存の連携を確認
        OAuth2UserLink existingLink = oauth2UserLinkRepository
            .findByProviderAndProviderUserId(provider, userInfo.getId())
            .orElse(null);
        
        User user;
        if (existingLink != null) {
            // 既存の連携がある場合
            user = existingLink.getUser();
            updateOAuth2UserLink(existingLink, userInfo, userRequest);
            log.info("既存のOAuth2連携でログイン: ユーザー={}", user.getUsername());
        } else {
            // 新規連携の場合
            user = findOrCreateUser(provider, userInfo);
            createOAuth2UserLink(user, provider, userInfo, userRequest);
            log.info("新規OAuth2連携作成: ユーザー={}", user.getUsername());
        }
        
        // 監査ログを記録
        auditLogService.logOAuth2Login(user.getUsername(), provider, true, 
            "OAuth2ログイン成功");
        
        // カスタム属性を追加
        Map<String, Object> customAttributes = new HashMap<>(attributes);
        customAttributes.put("userId", user.getId());
        customAttributes.put("username", user.getUsername());
        customAttributes.put("provider", provider);
        
        return new DefaultOAuth2User(
            Collections.singleton(() -> "ROLE_USER"),
            customAttributes,
            "username"
        );
    }
    
    /**
     * ユーザーを検索または作成
     */
    private User findOrCreateUser(String provider, OAuth2UserInfo userInfo) {
        // メールアドレスで既存ユーザーを検索
        Optional<User> existingUser = userRepository.findByEmail(userInfo.getEmail());
        
        if (existingUser.isPresent()) {
            // 既存ユーザーがいる場合は連携
            return existingUser.get();
        } else {
            // 新規ユーザーを作成
            return createNewUser(provider, userInfo);
        }
    }
    
    /**
     * 新規ユーザーを作成
     */
    private User createNewUser(String provider, OAuth2UserInfo userInfo) {
        User newUser = new User();
        
        // ユーザー名を生成（プロバイダー名 + プロバイダーID）
        String username = generateUniqueUsername(provider, userInfo);
        newUser.setUsername(username);
        
        // メールアドレスを設定
        newUser.setEmail(userInfo.getEmail());
        
        // ランダムパスワードを設定（OAuth2ユーザーは直接ログインしない）
        String randomPassword = UUID.randomUUID().toString();
        newUser.setPassword(passwordEncoder.encode(randomPassword));
        
        // 名前を設定
        newUser.setFullName(userInfo.getName());
        
        // OAuth2ユーザーはメール確認済みとする
        newUser.setEmailVerified(true);
        
        // アカウントを有効化
        newUser.setEnabled(true);
        newUser.setAccountLocked(false);
        
        return userRepository.save(newUser);
    }
    
    /**
     * ユニークなユーザー名を生成
     */
    private String generateUniqueUsername(String provider, OAuth2UserInfo userInfo) {
        String baseUsername = provider + "_" + userInfo.getId();
        String username = baseUsername;
        int counter = 1;
        
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + "_" + counter++;
        }
        
        return username;
    }
    
    /**
     * OAuth2連携を作成
     */
    private void createOAuth2UserLink(User user, String provider, OAuth2UserInfo userInfo, 
                                    OAuth2UserRequest userRequest) {
        OAuth2UserLink link = new OAuth2UserLink();
        link.setUser(user);
        link.setProvider(provider);
        link.setProviderUserId(userInfo.getId());
        link.setProviderEmail(userInfo.getEmail());
        link.setProviderName(userInfo.getName());
        link.setProviderPicture(userInfo.getImageUrl());
        link.setAccessToken(userRequest.getAccessToken().getTokenValue());
        link.setLinkedAt(LocalDateTime.now());
        link.setLastUsedAt(LocalDateTime.now());
        link.setActive(true);
        
        oauth2UserLinkRepository.save(link);
    }
    
    /**
     * OAuth2連携を更新
     */
    private void updateOAuth2UserLink(OAuth2UserLink link, OAuth2UserInfo userInfo, 
                                    OAuth2UserRequest userRequest) {
        link.setProviderEmail(userInfo.getEmail());
        link.setProviderName(userInfo.getName());
        link.setProviderPicture(userInfo.getImageUrl());
        link.setAccessToken(userRequest.getAccessToken().getTokenValue());
        link.setLastUsedAt(LocalDateTime.now());
        
        oauth2UserLinkRepository.save(link);
    }
    
    /**
     * OAuth2ユーザー情報インターフェース
     */
    public interface OAuth2UserInfo {
        String getId();
        String getName();
        String getEmail();
        String getImageUrl();
    }
    
    /**
     * OAuth2ユーザー情報ファクトリー
     */
    public static class OAuth2UserInfoFactory {
        public static OAuth2UserInfo getOAuth2UserInfo(String provider, Map<String, Object> attributes) {
            switch (provider.toLowerCase()) {
                case "google":
                    return new GoogleOAuth2UserInfo(attributes);
                case "github":
                    return new GitHubOAuth2UserInfo(attributes);
                case "microsoft":
                    return new MicrosoftOAuth2UserInfo(attributes);
                default:
                    throw new OAuth2AuthenticationException("サポートされていないプロバイダー: " + provider);
            }
        }
    }
    
    /**
     * Google OAuth2ユーザー情報
     */
    public static class GoogleOAuth2UserInfo implements OAuth2UserInfo {
        private final Map<String, Object> attributes;
        
        public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
            this.attributes = attributes;
        }
        
        @Override
        public String getId() {
            return (String) attributes.get("sub");
        }
        
        @Override
        public String getName() {
            return (String) attributes.get("name");
        }
        
        @Override
        public String getEmail() {
            return (String) attributes.get("email");
        }
        
        @Override
        public String getImageUrl() {
            return (String) attributes.get("picture");
        }
    }
    
    /**
     * GitHub OAuth2ユーザー情報
     */
    public static class GitHubOAuth2UserInfo implements OAuth2UserInfo {
        private final Map<String, Object> attributes;
        
        public GitHubOAuth2UserInfo(Map<String, Object> attributes) {
            this.attributes = attributes;
        }
        
        @Override
        public String getId() {
            return ((Integer) attributes.get("id")).toString();
        }
        
        @Override
        public String getName() {
            return (String) attributes.get("name");
        }
        
        @Override
        public String getEmail() {
            return (String) attributes.get("email");
        }
        
        @Override
        public String getImageUrl() {
            return (String) attributes.get("avatar_url");
        }
    }
    
    /**
     * Microsoft OAuth2ユーザー情報
     */
    public static class MicrosoftOAuth2UserInfo implements OAuth2UserInfo {
        private final Map<String, Object> attributes;
        
        public MicrosoftOAuth2UserInfo(Map<String, Object> attributes) {
            this.attributes = attributes;
        }
        
        @Override
        public String getId() {
            return (String) attributes.get("id");
        }
        
        @Override
        public String getName() {
            return (String) attributes.get("displayName");
        }
        
        @Override
        public String getEmail() {
            return (String) attributes.get("mail");
        }
        
        @Override
        public String getImageUrl() {
            return null; // Microsoftは画像URLを直接提供しない
        }
    }
}