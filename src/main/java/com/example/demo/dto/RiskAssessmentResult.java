package com.example.demo.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * リスク評価結果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessmentResult {
    
    /**
     * リスクスコア（0-100）
     */
    private int riskScore;
    
    /**
     * リスクレベル
     */
    private RiskLevel riskLevel;
    
    /**
     * 追加認証が必要かどうか
     */
    private boolean requiresAdditionalVerification;
    
    /**
     * 推奨される認証方法
     */
    private List<String> recommendedVerificationMethods;
    
    /**
     * リスク要因のリスト
     */
    private List<String> riskFactors;
    
    /**
     * 詳細なリスク分析
     */
    private RiskDetails riskDetails;
    
    /**
     * リスクレベル列挙型
     */
    public enum RiskLevel {
        LOW,      // 低リスク（0-30）
        MEDIUM,   // 中リスク（31-60）
        HIGH,     // 高リスク（61-80）
        CRITICAL  // 重大リスク（81-100）
    }
    
    /**
     * リスク詳細
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskDetails {
        private boolean newDevice;
        private boolean newLocation;
        private boolean suspiciousIp;
        private boolean unusualTime;
        private boolean multipleFailedAttempts;
        private boolean rapidLocationChange;
        private int recentFailedAttempts;
        private int distinctIpCount;
        private List<String> distinctCountries;
    }
}