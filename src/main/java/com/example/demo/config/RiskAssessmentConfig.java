package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * リスクベース認証の設定
 */
@Configuration
@ConfigurationProperties(prefix = "app.security.risk")
@Data
public class RiskAssessmentConfig {
    
    /**
     * 失敗試行回数の閾値
     */
    private int failedAttemptsThreshold = 5;
    
    /**
     * リスク評価の時間窓（時間）
     */
    private int timeWindowHours = 24;
    
    /**
     * デバイス信頼期間（日）
     */
    private int trustDeviceDays = 30;
    
    /**
     * 通常と異なる時間帯の開始時刻
     */
    private int unusualHourStart = 0;
    
    /**
     * 通常と異なる時間帯の終了時刻
     */
    private int unusualHourEnd = 6;
    
    /**
     * 低リスクスコアの閾値
     */
    private int lowRiskThreshold = 30;
    
    /**
     * 中リスクスコアの閾値
     */
    private int mediumRiskThreshold = 60;
    
    /**
     * 高リスクスコアの閾値
     */
    private int highRiskThreshold = 80;
    
    /**
     * IPジオロケーションサービスを有効化
     */
    private boolean enableGeolocation = true;
    
    /**
     * デバイスフィンガープリントを有効化
     */
    private boolean enableDeviceFingerprint = true;
}