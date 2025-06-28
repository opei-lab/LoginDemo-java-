package com.example.demo.service.impl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.config.PasswordPolicyConfig;
import com.example.demo.entity.AuditLog.EventType;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuditLogService;
import com.example.demo.service.IUserService;
import com.example.demo.service.PasswordHistoryService;
import com.example.demo.validator.PasswordValidator;
import com.example.demo.validator.PasswordValidator.ValidationResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements IUserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final PasswordHistoryService passwordHistoryService;
    private final PasswordPolicyConfig passwordPolicyConfig;
    private final AuditLogService auditLogService;
    
    // アカウントロックの閾値
    private static final int MAX_FAILED_ATTEMPTS = 5;
    // ロック期間（分）
    private static final int LOCK_DURATION_MINUTES = 30;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() ->
                new UsernameNotFoundException("ユーザーが見つかりません: " + username));
        
        // アカウントロックチェック
        if (user.isAccountLocked()) {
            // ロック期間が経過していれば自動解除
            if (user.getLockedAt() != null && 
                user.getLockedAt().plusMinutes(LOCK_DURATION_MINUTES).isBefore(LocalDateTime.now())) {
                unlockAccount(user);
            } else {
                throw new LockedException("アカウントがロックされています");
            }
        }
        
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            Collections.singletonList(() -> "ROLE_USER")
        );
    }

    @Override
    @Transactional
    public User register(User user) {
        // パスワードポリシーチェック
        ValidationResult validationResult = passwordValidator.validate(
            user.getPassword(), user.getUsername());
        
        if (!validationResult.isValid()) {
            throw new IllegalArgumentException(
                "パスワードが要件を満たしていません: " + validationResult.getErrorMessage());
        }
        
        // パスワードのエンコード
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        
        // ユーザー保存
        User savedUser = userRepository.save(user);
        
        // パスワード履歴に追加
        passwordHistoryService.addPasswordHistory(savedUser, encodedPassword);
        
        log.info("新規ユーザー登録: username={}", user.getUsername());
        auditLogService.logSuccess(EventType.USER_REGISTERED, user.getUsername());
        
        return savedUser;
    }
    
    /**
     * ログイン成功時の処理
     * @param username ユーザー名
     */
    @Transactional
    public void handleLoginSuccess(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("ログイン成功: username={}", username);
        });
    }
    
    /**
     * ログイン失敗時の処理
     * @param username ユーザー名
     */
    @Transactional
    public void handleLoginFailure(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            int failedAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failedAttempts);
            user.setLastFailedLoginAt(LocalDateTime.now());
            
            // 失敗回数が閾値に達したらアカウントロック
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                user.setAccountLocked(true);
                user.setLockedAt(LocalDateTime.now());
                log.warn("アカウントロック: username={}, 失敗回数={}", username, failedAttempts);
                auditLogService.logEvent(EventType.ACCOUNT_LOCKED, username, false, 
                    String.format("連続%d回のログイン失敗", failedAttempts));
            }
            
            userRepository.save(user);
            log.warn("ログイン失敗: username={}, 失敗回数={}", username, failedAttempts);
        });
    }
    
    /**
     * パスワード変更
     * @param user ユーザー
     * @param oldPassword 現在のパスワード
     * @param newPassword 新しいパスワード
     */
    @Transactional
    public void changePassword(User user, String oldPassword, String newPassword) {
        // 現在のパスワードチェック
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("現在のパスワードが正しくありません");
        }
        
        // パスワードポリシーチェック
        ValidationResult validationResult = passwordValidator.validate(
            newPassword, user.getUsername());
        
        if (!validationResult.isValid()) {
            throw new IllegalArgumentException(
                "新しいパスワードが要件を満たしていません: " + validationResult.getErrorMessage());
        }
        
        // パスワード履歴チェック
        if (passwordHistoryService.isPasswordInHistory(user, newPassword)) {
            throw new IllegalArgumentException(
                String.format("過去%d回分のパスワードは再利用できません", 
                    passwordPolicyConfig.getHistoryCount()));
        }
        
        // パスワード更新
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // パスワード履歴に追加
        passwordHistoryService.addPasswordHistory(user, encodedPassword);
        
        log.info("パスワード変更完了: username={}", user.getUsername());
        auditLogService.logSuccess(EventType.PASSWORD_CHANGED, user.getUsername());
    }
    
    /**
     * アカウントロック解除
     * @param user ユーザー
     */
    private void unlockAccount(User user) {
        user.setAccountLocked(false);
        user.setLockedAt(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        log.info("アカウントロック解除: username={}", user.getUsername());
        auditLogService.logSuccess(EventType.ACCOUNT_UNLOCKED, user.getUsername());
    }
    
    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Override
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }
}
