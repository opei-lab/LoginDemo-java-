package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.demo.security.MfaAuthenticationFilter;
import com.example.demo.security.OAuth2AuthenticationSuccessHandler;
import com.example.demo.security.OAuth2AuthenticationFailureHandler;
import com.example.demo.service.IUserService;
import com.example.demo.service.OAuth2UserService;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final IUserService userService;
    private final MfaAuthenticationFilter mfaAuthenticationFilter;
    private final OAuth2UserService oauth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oauth2FailureHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
          .userDetailsService(userService)
          .authorizeHttpRequests(authz -> authz
              // ログイン／登録／CSS は未認証でも OK とする
              .requestMatchers("/login", "/register", "/css/**", "/js/**", "/images/**").permitAll()
              // OAuth2関連のエンドポイント
              .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
              // 追加認証関連
              .requestMatchers("/auth/**").permitAll()
              // MFA検証も認証後にアクセス可能
              .requestMatchers("/mfa/verify").authenticated()
              // パスワード変更は認証後にアクセス可能
              .requestMatchers("/change-password").authenticated()
              // それ以外は認証必須
              .anyRequest().authenticated()
          )
          .formLogin(form -> form
              .loginPage("/login")
              .defaultSuccessUrl("/home", true)
          )
          .logout(logout -> logout
              .logoutSuccessUrl("/login?logout")
              .invalidateHttpSession(true)
              .deleteCookies("JSESSIONID")
          )
          // OAuth2ログイン設定
          .oauth2Login(oauth2 -> oauth2
              .loginPage("/login")
              .userInfoEndpoint(userInfo -> userInfo
                  .userService(oauth2UserService)
              )
              .successHandler(oauth2SuccessHandler)
              .failureHandler(oauth2FailureHandler)
          )
          // MFAフィルターを追加
          .addFilterAfter(mfaAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}