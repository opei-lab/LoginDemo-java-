package com.example.demo.repository;

import com.example.demo.entity.OneTimePassword;
import com.example.demo.entity.OneTimePassword.OtpPurpose;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * ワンタイムパスワードリポジトリ
 */
@Repository
public interface OneTimePasswordRepository extends JpaRepository<OneTimePassword, Long> {
    
    /**
     * ユーザーと用途で有効なOTPを検索
     * @param user ユーザー
     * @param purpose 用途
     * @return 有効なOTP
     */
    @Query("SELECT otp FROM OneTimePassword otp WHERE otp.user = :user AND otp.purpose = :purpose " +
           "AND otp.used = false AND otp.expiresAt > :now ORDER BY otp.createdAt DESC")
    Optional<OneTimePassword> findLatestValidOtp(@Param("user") User user, 
                                               @Param("purpose") OtpPurpose purpose,
                                               @Param("now") LocalDateTime now);
    
    /**
     * ユーザーとコードで有効なOTPを検索
     * @param user ユーザー
     * @param code コード
     * @return 有効なOTP
     */
    @Query("SELECT otp FROM OneTimePassword otp WHERE otp.user = :user AND otp.code = :code " +
           "AND otp.used = false AND otp.expiresAt > :now")
    Optional<OneTimePassword> findValidOtpByCode(@Param("user") User user, 
                                               @Param("code") String code,
                                               @Param("now") LocalDateTime now);
    
    /**
     * 期限切れのOTPを削除
     * @param now 現在時刻
     * @return 削除件数
     */
    @Modifying
    @Query("DELETE FROM OneTimePassword otp WHERE otp.expiresAt < :now OR otp.used = true")
    int deleteExpiredOtps(@Param("now") LocalDateTime now);
    
    /**
     * ユーザーの未使用OTPを無効化
     * @param user ユーザー
     * @param purpose 用途
     */
    @Modifying
    @Query("UPDATE OneTimePassword otp SET otp.used = true WHERE otp.user = :user " +
           "AND otp.purpose = :purpose AND otp.used = false")
    void invalidateUserOtps(@Param("user") User user, @Param("purpose") OtpPurpose purpose);
}