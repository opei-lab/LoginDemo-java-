package com.example.demo.repository;

import com.example.demo.entity.AuditLog;
import com.example.demo.entity.AuditLog.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 監査ログリポジトリ
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    /**
     * ユーザー名で監査ログを検索
     * @param username ユーザー名
     * @param pageable ページング情報
     * @return 監査ログページ
     */
    Page<AuditLog> findByUsername(String username, Pageable pageable);
    
    /**
     * イベントタイプで監査ログを検索
     * @param eventType イベントタイプ
     * @param pageable ページング情報
     * @return 監査ログページ
     */
    Page<AuditLog> findByEventType(EventType eventType, Pageable pageable);
    
    /**
     * 期間内の監査ログを検索
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @param pageable ページング情報
     * @return 監査ログページ
     */
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    Page<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                  @Param("endDate") LocalDateTime endDate, 
                                  Pageable pageable);
    
    /**
     * 失敗したログイン試行を検索
     * @param username ユーザー名
     * @param startDate 開始日時
     * @return 失敗したログイン試行のリスト
     */
    @Query("SELECT a FROM AuditLog a WHERE a.username = :username AND a.eventType = 'LOGIN_FAILURE' AND a.createdAt >= :startDate")
    List<AuditLog> findFailedLoginAttempts(@Param("username") String username, 
                                          @Param("startDate") LocalDateTime startDate);
    
    /**
     * IPアドレスごとの不審なアクティビティを検索
     * @param ipAddress IPアドレス
     * @param hours 過去何時間分
     * @return 監査ログリスト
     */
    @Query("SELECT a FROM AuditLog a WHERE a.ipAddress = :ipAddress AND a.createdAt >= :startDate AND (a.eventType = 'LOGIN_FAILURE' OR a.eventType = 'SUSPICIOUS_ACTIVITY')")
    List<AuditLog> findSuspiciousActivitiesByIp(@Param("ipAddress") String ipAddress, 
                                               @Param("startDate") LocalDateTime startDate);
}