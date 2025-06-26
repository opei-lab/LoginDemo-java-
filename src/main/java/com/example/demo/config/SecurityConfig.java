package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// authorizeHttpRequests を使う
import org.springframework.security.web.SecurityFilterChain;

import com.example.demo.service.IUserService;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final IUserService userService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
          .userDetailsService(userService)
          .authorizeHttpRequests(authz -> authz
              // ログイン／登録／CSS は未認証でも OK とする
              .requestMatchers("/login", "/register", "/css/**").permitAll()
              // それ以外は認証必須
              .anyRequest().authenticated()
          )
          .formLogin(form -> form
              .loginPage("/login")
              .defaultSuccessUrl("/home", true)
          )
          .logout(logout -> logout
              .logoutSuccessUrl("/login?logout")
          );
        return http.build();
    }
}