package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.EmailVerificationToken;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.InvalidEmailVerificationTokenException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.outbound.EmailVerificationTokenRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * {@link VerifyEmailService} 단위 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VerifyEmailService")
class VerifyEmailServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    private VerifyEmailService service;

    @BeforeEach
    void setUp() {
        service = new VerifyEmailService(tokenRepository, userRepository);
    }

    @Test
    @DisplayName("유효한 토큰으로 이메일 인증 성공")
    void should_verifyEmail_when_validToken() {
        // given
        String token = "valid-token";

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .id(1L)
                .token(token)
                .userId(10L)
                .email("user@example.com")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        User user = User.builder()
                .id(10L)
                .email("user@example.com")
                .nickname("테스트유저")
                .passwordHash("hash")
                .emailVerified(false)
                .build();

        given(tokenRepository.findByToken(token)).willReturn(Optional.of(verificationToken));
        given(userRepository.findById(10L)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));
        given(tokenRepository.save(any(EmailVerificationToken.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        service.verifyEmail(token);

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().isEmailVerified()).isTrue();

        ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().isVerified()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 토큰으로 이메일 인증 시 예외 발생")
    void should_throwNotFoundException_when_tokenNotFound() {
        // given
        String token = "nonexistent-token";
        given(tokenRepository.findByToken(token)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.verifyEmail(token))
                .isInstanceOf(InvalidEmailVerificationTokenException.class)
                .hasMessageContaining("유효하지 않은");
    }

    @Test
    @DisplayName("만료된 토큰으로 이메일 인증 시 예외 발생")
    void should_throwExpiredException_when_tokenExpired() {
        // given
        String token = "expired-token";

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .id(1L)
                .token(token)
                .userId(10L)
                .email("user@example.com")
                .expiresAt(LocalDateTime.now().minusMinutes(1)) // 만료됨
                .build();

        given(tokenRepository.findByToken(token)).willReturn(Optional.of(verificationToken));

        // when & then
        assertThatThrownBy(() -> service.verifyEmail(token))
                .isInstanceOf(InvalidEmailVerificationTokenException.class)
                .hasMessageContaining("만료");
    }

    @Test
    @DisplayName("이미 인증된 토큰으로 이메일 인증 시 예외 발생")
    void should_throwAlreadyVerifiedException_when_tokenAlreadyVerified() {
        // given
        String token = "verified-token";

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .id(1L)
                .token(token)
                .userId(10L)
                .email("user@example.com")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .verifiedAt(LocalDateTime.now().minusMinutes(5)) // 이미 인증됨
                .build();

        given(tokenRepository.findByToken(token)).willReturn(Optional.of(verificationToken));

        // when & then
        assertThatThrownBy(() -> service.verifyEmail(token))
                .isInstanceOf(InvalidEmailVerificationTokenException.class)
                .hasMessageContaining("이미 인증");
    }
}
