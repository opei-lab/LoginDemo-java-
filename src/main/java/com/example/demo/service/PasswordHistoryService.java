package com.example.demo.service;

import com.example.demo.config.PasswordPolicyConfig;
import com.example.demo.entity.PasswordHistory;
import com.example.demo.entity.User;
import com.example.demo.repository.PasswordHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * パスワード履歴管理サービス
 */
@Service
@RequiredArgsConstructor
public class PasswordHistoryService {
    
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordPolicyConfig passwordPolicyConfig;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * パスワードが履歴に存在するかチェック
     * @param user ユーザー
     * @param newPassword 新しいパスワード（平文）
     * @return 履歴に存在する場合true
     */
    public boolean isPasswordInHistory(User user, String newPassword) {
        List<PasswordHistory> histories = passwordHistoryRepository.findRecentPasswordsByUser(user);
        
        // 設定された履歴数分だけチェック
        int checkCount = Math.min(histories.size(), passwordPolicyConfig.getHistoryCount());
        
        for (int i = 0; i < checkCount; i++) {
            PasswordHistory history = histories.get(i);
            if (passwordEncoder.matches(newPassword, history.getPasswordHash())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * パスワード履歴を追加
     * @param user ユーザー
     * @param passwordHash パスワードハッシュ
     */
    @Transactional
    public void addPasswordHistory(User user, String passwordHash) {
        // 新しい履歴を追加
        PasswordHistory newHistory = new PasswordHistory();
        newHistory.setUser(user);
        newHistory.setPasswordHash(passwordHash);
        passwordHistoryRepository.save(newHistory);
        
        // 古い履歴を削除（設定された保持数を超える場合）
        long historyCount = passwordHistoryRepository.countByUser(user);
        if (historyCount > passwordPolicyConfig.getHistoryCount()) {
            PasswordHistory oldestHistory = passwordHistoryRepository.findOldestByUser(user);
            if (oldestHistory != null) {
                passwordHistoryRepository.delete(oldestHistory);
            }
        }
    }
    
    /**
     * ユーザーのすべてのパスワード履歴を削除
     * @param user ユーザー
     */
    @Transactional
    public void clearPasswordHistory(User user) {
        List<PasswordHistory> histories = passwordHistoryRepository.findRecentPasswordsByUser(user);
        passwordHistoryRepository.deleteAll(histories);
    }
}