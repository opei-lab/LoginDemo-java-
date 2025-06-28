package com.example.demo.repository;

import com.example.demo.entity.PasswordHistory;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * パスワード履歴リポジトリ
 */
@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {
    
    /**
     * ユーザーのパスワード履歴を新しい順に取得
     * @param user ユーザー
     * @param limit 取得件数
     * @return パスワード履歴リスト
     */
    @Query("SELECT ph FROM PasswordHistory ph WHERE ph.user = :user ORDER BY ph.createdAt DESC")
    List<PasswordHistory> findRecentPasswordsByUser(@Param("user") User user);
    
    /**
     * ユーザーのパスワード履歴件数を取得
     * @param user ユーザー
     * @return 履歴件数
     */
    long countByUser(User user);
    
    /**
     * ユーザーの最も古いパスワード履歴を取得
     * @param user ユーザー
     * @return 最も古いパスワード履歴
     */
    @Query("SELECT ph FROM PasswordHistory ph WHERE ph.user = :user ORDER BY ph.createdAt ASC LIMIT 1")
    PasswordHistory findOldestByUser(@Param("user") User user);
}