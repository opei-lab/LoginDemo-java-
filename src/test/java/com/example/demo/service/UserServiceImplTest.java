package com.example.demo.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.config.PasswordPolicyConfig;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.impl.UserServiceImpl;
import com.example.demo.validator.PasswordValidator;
import com.example.demo.validator.PasswordValidator.ValidationResult;

/**
 * UserServiceImplのユニットテスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImplテスト")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordHistoryService passwordHistoryService;

    @Mock
    private AuditLogService auditLogService;
    
    @Mock
    private PasswordValidator passwordValidator;
    
    @Mock
    private PasswordPolicyConfig passwordPolicyConfig;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setEmail("test@example.com");
        testUser.setEnabled(true);
        testUser.setAccountLocked(false);
        testUser.setFailedLoginAttempts(0);
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("ユーザー登録が正常に動作すること")
    void testRegisterUser_Success() {
        // Given
        ValidationResult validResult = new ValidationResult(true, Collections.emptyList());
        when(passwordValidator.validate(anyString(), anyString())).thenReturn(validResult);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("password123");

        User result = userService.register(newUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).save(any(User.class));
        verify(passwordHistoryService).addPasswordHistory(any(User.class), anyString());
        verify(auditLogService).logSuccess(any(), eq("newuser"));
    }

    @Test
    @DisplayName("パスワードが要件を満たさない場合、例外が発生すること")
    void testRegisterUser_InvalidPassword() {
        // Given
        ValidationResult invalidResult = new ValidationResult(false, 
            Collections.singletonList("パスワードは8文字以上必要です"));
        when(passwordValidator.validate(anyString(), anyString())).thenReturn(invalidResult);

        // When/Then
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("short");

        assertThatThrownBy(() -> userService.register(newUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("パスワードが要件を満たしていません");
    }

    @Test
    @DisplayName("アカウントロックが正しく動作すること")
    void testAccountLocking() {
        // Given
        testUser.setFailedLoginAttempts(4); // 次で5回目
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.handleLoginFailure("testuser");

        // Then
        assertThat(testUser.isAccountLocked()).isTrue();
        assertThat(testUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(testUser.getLockedAt()).isNotNull();
        verify(auditLogService).logEvent(any(), eq("testuser"), eq(false), contains("連続"));
    }

    @Test
    @DisplayName("ログイン成功時に失敗回数がリセットされること")
    void testResetFailedAttempts() {
        // Given
        testUser.setFailedLoginAttempts(3);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.handleLoginSuccess("testuser");

        // Then
        assertThat(testUser.getFailedLoginAttempts()).isZero();
        assertThat(testUser.getLastLoginAt()).isNotNull();
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("ロックされたアカウントのロード時にロック解除期間をチェックすること")
    void testLoadUserByUsername_ChecksLockExpiry() {
        // Given
        testUser.setAccountLocked(true);
        testUser.setLockedAt(LocalDateTime.now().minusMinutes(31)); // 30分経過
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDetails result = userService.loadUserByUsername("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(testUser.isAccountLocked()).isFalse(); // 自動解除されている
        assertThat(testUser.getFailedLoginAttempts()).isZero();
        verify(auditLogService).logSuccess(any(), eq("testuser"));
    }

    @Test
    @DisplayName("パスワード変更が正常に動作すること")
    void testChangePassword_Success() {
        // Given
        String oldPassword = "oldPassword";
        String newPassword = "newPassword123";
        ValidationResult validResult = new ValidationResult(true, Collections.emptyList());
        
        when(passwordEncoder.matches(oldPassword, testUser.getPassword())).thenReturn(true);
        when(passwordValidator.validate(newPassword, testUser.getUsername())).thenReturn(validResult);
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");
        when(passwordHistoryService.isPasswordInHistory(testUser, newPassword)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.changePassword(testUser, oldPassword, newPassword);

        // Then
        assertThat(testUser.getPassword()).isEqualTo("encodedNewPassword");
        assertThat(testUser.getPasswordChangedAt()).isNotNull();
        verify(passwordHistoryService).addPasswordHistory(testUser, "encodedNewPassword");
        verify(auditLogService).logSuccess(any(), eq("testuser"));
    }

    @Test
    @DisplayName("メールアドレスでユーザーを検索できること")
    void testFindByEmail() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByEmail("test@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }
}