package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.security.PepperedPasswordEncoder;

/**
 * パスワードエンコーダー設定
 * ペッパー付きBCryptエンコーダーを提供
 */
@Configuration
public class EncoderConfig {

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder(PepperedPasswordEncoder pepperedPasswordEncoder) {
        // PepperedPasswordEncoderを使用
        return pepperedPasswordEncoder;
    }

}
