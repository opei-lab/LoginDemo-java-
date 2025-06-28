package com.example.demo.repository;

import com.example.demo.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ログイン試行リポジトリ
 */
@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    
    /**
     * 指定期間内のユーザーのログイン試行を取得
     */
    List<LoginAttempt> findByUsernameAndAttemptedAtAfter(String username, LocalDateTime after);
    
    /**
     * 指定IPアドレスからの最近のログイン試行を取得
     */
    List<LoginAttempt> findByIpAddressAndAttemptedAtAfter(String ipAddress, LocalDateTime after);
    
    /**
     * ユーザーの最後の成功したログイン試行を取得
     */
    LoginAttempt findTopByUsernameAndSuccessfulTrueOrderByAttemptedAtDesc(String username);
    
    /**
     * 指定期間内の失敗回数を取得
     */
    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.username = :username " +
           "AND la.successful = false AND la.attemptedAt > :after")
    long countFailedAttempts(@Param("username") String username, @Param("after") LocalDateTime after);
    
    /**
     * 異なるIPアドレスからのログイン試行数を取得
     */
    @Query("SELECT COUNT(DISTINCT la.ipAddress) FROM LoginAttempt la " +
           "WHERE la.username = :username AND la.attemptedAt > :after")
    long countDistinctIpAddresses(@Param("username") String username, @Param("after") LocalDateTime after);
    
    /**
     * 異なる国からのログイン試行を検出
     */
    @Query("SELECT DISTINCT la.countryCode FROM LoginAttempt la " +
           "WHERE la.username = :username AND la.attemptedAt > :after " +
           "AND la.countryCode IS NOT NULL")
    List<String> findDistinctCountryCodes(@Param("username") String username, @Param("after") LocalDateTime after);
}