package com.example.demo.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * セキュリティ設定の基本的なテスト
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("セキュリティ設定テスト")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("未認証ユーザーはログインページにリダイレクトされること")
    void testUnauthenticatedAccess_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/home"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("ログインページは未認証でもアクセス可能であること")
    void testLoginPage_AccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("登録ページは未認証でもアクセス可能であること")
    void testRegisterPage_AccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/register"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("静的リソースへのアクセスが許可されること")
    void testStaticResources_Accessible() throws Exception {
        mockMvc.perform(get("/css/auth.css"))
            .andExpect(status().isOk());
    }
}