package com.example.demo.repository;

import com.example.demo.entity.OAuth2UserLink;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * OAuth2ユーザー連携リポジトリ
 */
@Repository
public interface OAuth2UserLinkRepository extends JpaRepository<OAuth2UserLink, Long> {
    
    /**
     * プロバイダーとプロバイダーユーザーIDで検索
     */
    Optional<OAuth2UserLink> findByProviderAndProviderUserId(String provider, String providerUserId);
    
    /**
     * プロバイダーとメールアドレスで検索
     */
    Optional<OAuth2UserLink> findByProviderAndProviderEmail(String provider, String providerEmail);
    
    /**
     * ユーザーの連携済みプロバイダーを取得
     */
    List<OAuth2UserLink> findByUserAndIsActiveTrue(User user);
    
    /**
     * ユーザーとプロバイダーで検索
     */
    Optional<OAuth2UserLink> findByUserAndProviderAndIsActiveTrue(User user, String provider);
}