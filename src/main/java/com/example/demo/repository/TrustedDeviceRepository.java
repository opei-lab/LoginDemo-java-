package com.example.demo.repository;

import com.example.demo.entity.TrustedDevice;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 信頼済みデバイスリポジトリ
 */
@Repository
public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, Long> {
    
    /**
     * ユーザーとデバイスフィンガープリントで信頼済みデバイスを検索
     */
    Optional<TrustedDevice> findByUserAndDeviceFingerprintAndIsActiveTrue(
        User user, String deviceFingerprint);
    
    /**
     * ユーザーのアクティブな信頼済みデバイスを取得
     */
    List<TrustedDevice> findByUserAndIsActiveTrue(User user);
    
    /**
     * 期限切れの信頼済みデバイスを無効化
     */
    List<TrustedDevice> findByTrustExpiresAtBeforeAndIsActiveTrue(LocalDateTime now);
    
    /**
     * ユーザーの信頼済みデバイス数を取得
     */
    long countByUserAndIsActiveTrue(User user);
}