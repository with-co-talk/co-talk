package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.PasswordResetToken;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.domain.port.outbound.EmailSender;
import com.cotalk.domain.port.outbound.PasswordResetTokenRepository;
import com.cotalk.domain.port.outbound.TimeProvider;
import com.cotalk.domain.port.outbound.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestPasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmailSender emailSender;

    @Mock
    private TimeProvider timeProvider;

    private RequestPasswordResetService service;

    private static final java.time.LocalDateTime FIXED_NOW = java.time.LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void setUp() {
        String frontendUrl = "http://localhost:3000";
        int tokenExpirationMinutes = 30;
        service = new RequestPasswordResetService(
                userRepository,
                tokenRepository,
                emailSender,
                timeProvider,
                frontendUrl,
                tokenExpirationMinutes
        );
    }

    @Test
    @DisplayName("존재하는 이메일로 비밀번호 재설정 요청 시 이메일 발송")
    void should_sendEmail_when_validEmail() {
        // given
        String email = "user@example.com";
        User user = User.builder()
                .id(1L)
                .email(new Email(email))
                .nickname("테스트유저")
                .passwordHash("hash")
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(tokenRepository.save(any(PasswordResetToken.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when
        service.requestPasswordReset(email);

        // then
        verify(tokenRepository).deleteByUserId(user.getId());
        
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUserId()).isEqualTo(user.getId());
        assertThat(savedToken.getEmail()).isEqualTo(new Email(email));
        assertThat(savedToken.getToken()).isNotBlank();

        verify(emailSender).sendPasswordResetEmail(eq(email), anyString());
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 요청 시 이메일 발송 안 함 (보안상 에러 미반환)")
    void should_notSendEmail_when_emailNotExists() {
        // given
        String email = "nonexistent@example.com";
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // when
        service.requestPasswordReset(email);

        // then
        verify(tokenRepository, never()).save(any());
        verify(emailSender, never()).sendPasswordResetEmail(anyString(), anyString());
    }
}
