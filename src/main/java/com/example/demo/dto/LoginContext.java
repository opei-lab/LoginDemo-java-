package com.example.demo.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * ログインコンテキストDTO
 * ログイン試行時の環境情報を保持
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginContext {
    
    /**
     * IPアドレス
     */
    private String ipAddress;
    
    /**
     * ユーザーエージェント
     */
    private String userAgent;
    
    /**
     * デバイスフィンガープリント
     */
    private String deviceFingerprint;
    
    /**
     * 国コード（IPジオロケーションから取得）
     */
    private String countryCode;
    
    /**
     * 都市（IPジオロケーションから取得）
     */
    private String city;
    
    /**
     * プロキシ経由かどうか
     */
    private boolean isProxy;
    
    /**
     * VPN経由かどうか
     */
    private boolean isVpn;
    
    /**
     * ログイン時刻（時間帯分析用）
     */
    private int hourOfDay;
    
    /**
     * 曜日（0=日曜日, 6=土曜日）
     */
    private int dayOfWeek;
}