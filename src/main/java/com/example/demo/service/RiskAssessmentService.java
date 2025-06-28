package com.example.demo.service;

import com.example.demo.dto.RiskAssessmentResult;
import com.example.demo.dto.LoginContext;

/**
 * リスク評価サービスインターフェース
 */
public interface RiskAssessmentService {
    
    /**
     * ログイン試行のリスクを評価
     * 
     * @param username ユーザー名
     * @param context ログインコンテキスト（IP、ユーザーエージェントなど）
     * @return リスク評価結果
     */
    RiskAssessmentResult assessLoginRisk(String username, LoginContext context);
    
    /**
     * デバイスを信頼済みとして登録
     * 
     * @param username ユーザー名
     * @param deviceFingerprint デバイスフィンガープリント
     * @param deviceName デバイス名（オプション）
     */
    void trustDevice(String username, String deviceFingerprint, String deviceName);
    
    /**
     * 信頼済みデバイスを削除
     * 
     * @param username ユーザー名
     * @param deviceId デバイスID
     */
    void removeTrustedDevice(String username, Long deviceId);
    
    /**
     * ログイン試行を記録
     * 
     * @param username ユーザー名
     * @param context ログインコンテキスト
     * @param successful 成功フラグ
     * @param riskScore リスクスコア
     * @param riskFactors リスク要因
     */
    void recordLoginAttempt(String username, LoginContext context, boolean successful, 
                          int riskScore, String riskFactors);
}