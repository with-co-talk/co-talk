package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.EmailVerificationToken;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.domain.exception.RateLimitExceededException;
import com.cotalk.domain.port.outbound.EmailSender;
import com.cotalk.domain.port.outbound.EmailVerificationTokenRepository;
import com.cotalk.domain.port.outbound.TimeProvider;
import com.cotalk.domain.port.outbound.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link ResendVerificationService} 단위 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResendVerificationService")
class ResendVerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private EmailSender emailSender;

    @Mock
    private TimeProvider timeProvider;

    private ResendVerificationService service;

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void setUp() {
        String frontendUrl = "http://localhost:3000";
        service = new ResendVerificationService(userRepository, tokenRepository, emailSender, timeProvider, frontendUrl);
    }

    @Test
    @DisplayName("미인증 사용자에게 인증 이메일을 재발송한다")
    void should_resendVerification_when_validUnverifiedEmail() {
        // given
        String email = "user@example.com";

        User user = User.builder()
                .id(10L)
                .email(new Email(email))
                .nickname("테스트유저")
                .passwordHash("hash")
                .emailVerified(false)
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(tokenRepository.findLatestByUserId(10L)).willReturn(Optional.empty());
        given(tokenRepository.save(any(EmailVerificationToken.class)))
                .willAnswer(inv -> inv.getArgument(0));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when
        service.resendVerification(email);

        // then
        verify(tokenRepository).deleteByUserId(10L);
        verify(tokenRepository).save(any(EmailVerificationToken.class));
        verify(emailSender).sendVerificationEmail(eq(email), anyString());
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 아무 동작도 하지 않는다")
    void should_doNothing_when_emailNotFound() {
        // given
        String email = "nonexistent@example.com";
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // when
        service.resendVerification(email);

        // then
        verify(emailSender, never()).sendVerificationEmail(anyString(), anyString());
        verify(tokenRepository, never()).save(any(EmailVerificationToken.class));
    }

    @Test
    @DisplayName("이미 인증된 사용자이면 아무 동작도 하지 않는다")
    void should_doNothing_when_alreadyVerified() {
        // given
        String email = "verified@example.com";

        User user = User.builder()
                .id(10L)
                .email(new Email(email))
                .nickname("테스트유저")
                .passwordHash("hash")
                .emailVerified(true)
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // when
        service.resendVerification(email);

        // then
        verify(emailSender, never()).sendVerificationEmail(anyString(), anyString());
        verify(tokenRepository, never()).save(any(EmailVerificationToken.class));
    }

    @Test
    @DisplayName("쿨다운 시간 내 재요청 시 RateLimitExceededException 발생")
    void should_throwRateLimitException_when_cooldownNotPassed() {
        // given
        String email = "user@example.com";

        User user = User.builder()
                .id(10L)
                .email(new Email(email))
                .nickname("테스트유저")
                .passwordHash("hash")
                .emailVerified(false)
                .build();

        EmailVerificationToken recentToken = EmailVerificationToken.builder()
                .id(1L)
                .token("recent-token")
                .userId(10L)
                .email(new Email(email))
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        // BaseEntity.createdAt은 JPA 감사로만 설정되므로 리플렉션으로 설정
        setCreatedAt(recentToken, FIXED_NOW);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(tokenRepository.findLatestByUserId(10L)).willReturn(Optional.of(recentToken));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when & then
        assertThatThrownBy(() -> service.resendVerification(email))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("요청 한도");
    }

    /**
     * BaseEntity.createdAt 필드를 리플렉션으로 설정한다.
     * JPA 감사 기능이 동작하지 않는 단위 테스트에서 사용한다.
     */
    private void setCreatedAt(Object entity, LocalDateTime createdAt) {
        try {
            Field field = entity.getClass().getSuperclass().getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(entity, createdAt);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("createdAt 필드 설정 실패", e);
        }
    }
}
