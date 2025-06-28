package com.example.demo.repository;

import com.example.demo.entity.BackupCode;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * バックアップコードリポジトリ
 */
@Repository
public interface BackupCodeRepository extends JpaRepository<BackupCode, Long> {
    
    /**
     * ユーザーの未使用バックアップコードを取得
     * @param user ユーザー
     * @return バックアップコードリスト
     */
    List<BackupCode> findByUserAndUsedFalse(User user);
    
    /**
     * ユーザーとコードで検索（未使用のもの）
     * @param user ユーザー
     * @param code コード
     * @return バックアップコード
     */
    Optional<BackupCode> findByUserAndCodeAndUsedFalse(User user, String code);
    
    /**
     * ユーザーのすべてのバックアップコードを削除
     * @param user ユーザー
     */
    @Modifying
    @Query("DELETE FROM BackupCode bc WHERE bc.user = :user")
    void deleteAllByUser(@Param("user") User user);
    
    /**
     * ユーザーの未使用バックアップコード数を取得
     * @param user ユーザー
     * @return 未使用コード数
     */
    long countByUserAndUsedFalse(User user);
}