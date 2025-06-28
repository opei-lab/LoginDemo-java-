package com.example.demo.service;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FormTokenServiceのユニットテスト
 * 二重送信防止機能のテスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FormTokenServiceテスト")
class FormTokenServiceTest {

    @Mock
    private HttpSession session;

    @InjectMocks
    private FormTokenService formTokenService;

    @BeforeEach
    void setUp() {
        // セッションのモック設定をリセット
        reset(session);
    }

    @Test
    @DisplayName("トークンが正常に生成されること")
    void testGenerateToken() {
        // When
        String token = formTokenService.generateToken(session, "testForm");

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        verify(session).setAttribute(eq("FORM_TOKEN_testForm"), anyString());
    }

    @Test
    @DisplayName("有効なトークンが検証を通過すること")
    void testValidateToken_Success() {
        // Given
        String validToken = "valid-token-123";
        when(session.getAttribute("FORM_TOKEN_testForm")).thenReturn(validToken);

        // When
        boolean result = formTokenService.validateAndRemoveToken(session, "testForm", validToken);

        // Then
        assertThat(result).isTrue();
        verify(session).removeAttribute("FORM_TOKEN_testForm");
    }

    @Test
    @DisplayName("無効なトークンが検証で失敗すること")
    void testValidateToken_InvalidToken() {
        // Given
        when(session.getAttribute("FORM_TOKEN_testForm")).thenReturn("stored-token");

        // When
        boolean result = formTokenService.validateAndRemoveToken(session, "testForm", "wrong-token");

        // Then
        assertThat(result).isFalse();
        verify(session, never()).removeAttribute(anyString());
    }

    @Test
    @DisplayName("nullトークンが検証で失敗すること")
    void testValidateToken_NullToken() {
        // When
        boolean result = formTokenService.validateAndRemoveToken(session, "testForm", null);

        // Then
        assertThat(result).isFalse();
        verify(session, never()).getAttribute(anyString());
    }

    @Test
    @DisplayName("空のトークンが検証で失敗すること")
    void testValidateToken_EmptyToken() {
        // When
        boolean result = formTokenService.validateAndRemoveToken(session, "testForm", "");

        // Then
        assertThat(result).isFalse();
        verify(session, never()).getAttribute(anyString());
    }

    @Test
    @DisplayName("セッションにトークンが存在しない場合、検証が失敗すること")
    void testValidateToken_NoTokenInSession() {
        // Given
        when(session.getAttribute("FORM_TOKEN_testForm")).thenReturn(null);

        // When
        boolean result = formTokenService.validateAndRemoveToken(session, "testForm", "any-token");

        // Then
        assertThat(result).isFalse();
        verify(session, never()).removeAttribute(anyString());
    }

    @Test
    @DisplayName("一度使用したトークンは再利用できないこと")
    void testValidateToken_OneTimeUse() {
        // Given
        String token = "one-time-token";
        when(session.getAttribute("FORM_TOKEN_testForm"))
            .thenReturn(token)
            .thenReturn(null); // 2回目は削除済み

        // When
        boolean firstValidation = formTokenService.validateAndRemoveToken(session, "testForm", token);
        boolean secondValidation = formTokenService.validateAndRemoveToken(session, "testForm", token);

        // Then
        assertThat(firstValidation).isTrue();
        assertThat(secondValidation).isFalse();
        verify(session, times(1)).removeAttribute("FORM_TOKEN_testForm");
    }

    @Test
    @DisplayName("異なるフォーム名で独立したトークンが生成されること")
    void testGenerateToken_DifferentForms() {
        // When
        String token1 = formTokenService.generateToken(session, "form1");
        String token2 = formTokenService.generateToken(session, "form2");

        // Then
        assertThat(token1).isNotEqualTo(token2);
        verify(session).setAttribute(eq("FORM_TOKEN_form1"), anyString());
        verify(session).setAttribute(eq("FORM_TOKEN_form2"), anyString());
    }
}