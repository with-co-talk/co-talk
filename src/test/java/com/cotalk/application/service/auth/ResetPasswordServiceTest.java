package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.PasswordResetToken;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.InvalidPasswordResetTokenException;
import com.cotalk.domain.port.outbound.PasswordResetTokenRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ResetPasswordServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private ResetPasswordService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new ResetPasswordService(tokenRepository, userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("유효한 토큰으로 비밀번호 재설정 성공")
    void should_resetPassword_when_validToken() {
        // given
        String token = "valid-token";
        String newPassword = "newPassword123!";
        
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .token(token)
                .userId(10L)
                .email("user@example.com")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        User user = User.builder()
                .id(10L)
                .email("user@example.com")
                .nickname("테스트유저")
                .passwordHash("oldHash")
                .build();

        given(tokenRepository.findByToken(token)).willReturn(Optional.of(resetToken));
        given(userRepository.findById(10L)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        service.resetPassword(token, newPassword);

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        
        User savedUser = userCaptor.getValue();
        assertThat(passwordEncoder.matches(newPassword, savedUser.getPasswordHash())).isTrue();

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().isUsed()).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰으로 비밀번호 재설정 시 예외")
    void should_throwException_when_tokenExpired() {
        // given
        String token = "expired-token";
        
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .token(token)
                .userId(10L)
                .email("user@example.com")
                .expiresAt(LocalDateTime.now().minusMinutes(1)) // 만료됨
                .build();

        given(tokenRepository.findByToken(token)).willReturn(Optional.of(resetToken));

        // when & then
        assertThatThrownBy(() -> service.resetPassword(token, "newPassword"))
                .isInstanceOf(InvalidPasswordResetTokenException.class)
                .hasMessageContaining("만료");
    }

    @Test
    @DisplayName("이미 사용된 토큰으로 비밀번호 재설정 시 예외")
    void should_throwException_when_tokenAlreadyUsed() {
        // given
        String token = "used-token";
        
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .token(token)
                .userId(10L)
                .email("user@example.com")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .usedAt(LocalDateTime.now().minusMinutes(5)) // 이미 사용됨
                .build();

        given(tokenRepository.findByToken(token)).willReturn(Optional.of(resetToken));

        // when & then
        assertThatThrownBy(() -> service.resetPassword(token, "newPassword"))
                .isInstanceOf(InvalidPasswordResetTokenException.class)
                .hasMessageContaining("이미 사용");
    }

    @Test
    @DisplayName("존재하지 않는 토큰으로 비밀번호 재설정 시 예외")
    void should_throwException_when_tokenNotFound() {
        // given
        String token = "nonexistent-token";
        given(tokenRepository.findByToken(token)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.resetPassword(token, "newPassword"))
                .isInstanceOf(InvalidPasswordResetTokenException.class)
                .hasMessageContaining("유효하지 않은");
    }

    @Test
    @DisplayName("토큰 유효성 검증 - 유효한 토큰")
    void should_returnTrue_when_tokenValid() {
        // given
        String token = "valid-token";
        
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .userId(10L)
                .email("user@example.com")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        given(tokenRepository.findByToken(token)).willReturn(Optional.of(resetToken));

        // when
        boolean result = service.validateToken(token);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("토큰 유효성 검증 - 만료된 토큰")
    void should_returnFalse_when_tokenExpired() {
        // given
        String token = "expired-token";
        
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .userId(10L)
                .email("user@example.com")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        given(tokenRepository.findByToken(token)).willReturn(Optional.of(resetToken));

        // when
        boolean result = service.validateToken(token);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("토큰 유효성 검증 - 존재하지 않는 토큰")
    void should_returnFalse_when_tokenNotFound() {
        // given
        String token = "nonexistent-token";
        given(tokenRepository.findByToken(token)).willReturn(Optional.empty());

        // when
        boolean result = service.validateToken(token);

        // then
        assertThat(result).isFalse();
    }
}
