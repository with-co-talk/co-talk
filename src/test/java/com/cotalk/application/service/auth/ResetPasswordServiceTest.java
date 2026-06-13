package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.PasswordResetToken;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.domain.exception.InvalidPasswordResetTokenException;
import com.cotalk.domain.port.outbound.PasswordEncoderPort;
import com.cotalk.domain.port.outbound.PasswordResetTokenRepository;
import com.cotalk.domain.port.outbound.TimeProvider;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.security.SpringPasswordEncoderAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ResetPasswordServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TimeProvider timeProvider;

    private PasswordEncoderPort passwordEncoder;
    private ResetPasswordService service;

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void setUp() {
        passwordEncoder = new SpringPasswordEncoderAdapter(new BCryptPasswordEncoder());
        service = new ResetPasswordService(tokenRepository, userRepository, passwordEncoder, timeProvider);
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
                .email(new Email("user@example.com"))
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        User user = User.builder()
                .id(10L)
                .email(new Email("user@example.com"))
                .nickname("테스트유저")
                .passwordHash("oldHash")
                .build();

        given(tokenRepository.findByToken(token)).willReturn(Optional.of(resetToken));
        given(userRepository.findById(10L)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));
        given(timeProvider.now()).willReturn(FIXED_NOW);

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
                .email(new Email("user@example.com"))
                .expiresAt(FIXED_NOW.minusMinutes(1)) // 만료됨
                .build();

        given(tokenRepository.findByToken(token)).willReturn(Optional.of(resetToken));
        given(timeProvider.now()).willReturn(FIXED_NOW);

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
                .email(new Email("user@example.com"))
                .expiresAt(FIXED_NOW.plusMinutes(30))
                .usedAt(FIXED_NOW.minusMinutes(5)) // 이미 사용됨
                .build();

        given(tokenRepository.findByToken(token)).willReturn(Optional.of(resetToken));
        given(timeProvider.now()).willReturn(FIXED_NOW);

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
                .email(new Email("user@example.com"))
                .expiresAt(FIXED_NOW.plusMinutes(30))
                .build();

        given(tokenRepository.findByToken(token)).willReturn(Optional.of(resetToken));
        given(timeProvider.now()).willReturn(FIXED_NOW);

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
                .email(new Email("user@example.com"))
                .expiresAt(FIXED_NOW.minusMinutes(1))
                .build();

        given(tokenRepository.findByToken(token)).willReturn(Optional.of(resetToken));
        given(timeProvider.now()).willReturn(FIXED_NOW);

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

    @Test
    @DisplayName("유효한 이메일+코드로 verifyCode 호출 시 예외 없이 성공한다")
    void should_succeed_when_validCode() {
        // given
        String email = "user@example.com";
        String code = "123456";

        PasswordResetToken token = PasswordResetToken.builder()
                .token("some-token")
                .userId(10L)
                .email(new Email(email))
                .verificationCode(code)
                .expiresAt(FIXED_NOW.plusMinutes(30))
                .build();

        given(tokenRepository.findLatestActiveByEmail(email)).willReturn(Optional.of(token));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when & then
        assertDoesNotThrow(() -> service.verifyCode(email, code));
    }

    @Test
    @DisplayName("활성 토큰이 없으면 invalidCode 예외가 발생한다")
    void should_throwInvalidCode_when_codeNotFound() {
        // given
        String email = "user@example.com";
        String code = "123456";

        given(tokenRepository.findLatestActiveByEmail(email)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.verifyCode(email, code))
                .isInstanceOf(InvalidPasswordResetTokenException.class)
                .hasMessageContaining("일치");
    }

    @Test
    @DisplayName("만료된 토큰으로 verifyCode 호출 시 expired 예외가 발생한다")
    void should_throwExpired_when_codeExpired() {
        // given
        String email = "user@example.com";
        String code = "123456";

        PasswordResetToken token = PasswordResetToken.builder()
                .token("some-token")
                .userId(10L)
                .email(new Email(email))
                .verificationCode(code)
                .expiresAt(FIXED_NOW.minusMinutes(1))
                .build();

        given(tokenRepository.findLatestActiveByEmail(email)).willReturn(Optional.of(token));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when & then
        assertThatThrownBy(() -> service.verifyCode(email, code))
                .isInstanceOf(InvalidPasswordResetTokenException.class)
                .hasMessageContaining("만료");
    }

    @Test
    @DisplayName("잘못된 코드로 verifyCode 호출 시 invalidCode 예외가 발생하고 failedAttempts가 원자적으로 증가한다")
    void should_incrementFailedAttempts_when_wrongCode() {
        // given
        String email = "user@example.com";
        String wrongCode = "000000";

        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .token("some-token")
                .userId(10L)
                .email(new Email(email))
                .verificationCode("123456")
                .expiresAt(FIXED_NOW.plusMinutes(30))
                .build();

        given(tokenRepository.findLatestActiveByEmail(email)).willReturn(Optional.of(token));
        given(timeProvider.now()).willReturn(FIXED_NOW);
        // 원자적 증가 후 실패 횟수 1 반환
        given(tokenRepository.incrementFailedAttemptsAndGet(1L)).willReturn(1);

        // when & then
        assertThatThrownBy(() -> service.verifyCode(email, wrongCode))
                .isInstanceOf(InvalidPasswordResetTokenException.class)
                .hasMessageContaining("일치");

        // 원자적 UPDATE로 실패 횟수를 증가시켰는지 검증(lost-update 방지)
        verify(tokenRepository).incrementFailedAttemptsAndGet(1L);
    }

    @Test
    @DisplayName("잘못된 코드 5회 입력 시 토큰이 잠겨 maxAttemptsExceeded 예외가 발생한다")
    void should_lockToken_when_wrongCodeFiveTimes() {
        // given
        String email = "user@example.com";
        String wrongCode = "000000";

        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .token("some-token")
                .userId(10L)
                .email(new Email(email))
                .verificationCode("123456")
                .expiresAt(FIXED_NOW.plusMinutes(30))
                .build();

        given(tokenRepository.findLatestActiveByEmail(email)).willReturn(Optional.of(token));
        given(timeProvider.now()).willReturn(FIXED_NOW);
        // 원자적 증가가 호출될 때마다 1,2,3,4,5 를 반환(DB 상태 시뮬레이션)
        given(tokenRepository.incrementFailedAttemptsAndGet(1L))
                .willReturn(1, 2, 3, 4, 5);

        // when: 4회 오답은 invalidCode, 5회째 오답은 잠금(초과)
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.verifyCode(email, wrongCode))
                    .isInstanceOf(InvalidPasswordResetTokenException.class)
                    .hasMessageContaining("일치");
        }

        assertThatThrownBy(() -> service.verifyCode(email, wrongCode))
                .isInstanceOf(InvalidPasswordResetTokenException.class)
                .hasMessageContaining("초과");
    }

    @Test
    @DisplayName("failedAttempts가 5인 토큰으로 verifyCode 호출 시 maxAttemptsExceeded 예외가 발생한다")
    void should_throwMaxAttempts_when_exceededMaxAttempts() {
        // given
        String email = "user@example.com";
        String code = "123456";

        PasswordResetToken token = PasswordResetToken.builder()
                .token("some-token")
                .userId(10L)
                .email(new Email(email))
                .verificationCode(code)
                .expiresAt(FIXED_NOW.plusMinutes(30))
                .failedAttempts(5)
                .build();

        given(tokenRepository.findLatestActiveByEmail(email)).willReturn(Optional.of(token));

        // when & then
        assertThatThrownBy(() -> service.verifyCode(email, code))
                .isInstanceOf(InvalidPasswordResetTokenException.class)
                .hasMessageContaining("초과");
    }

    @Test
    @DisplayName("이미 사용된 토큰으로 verifyCode 호출 시 alreadyUsed 예외가 발생한다")
    void should_throwAlreadyUsed_when_codeAlreadyUsed() {
        // given
        String email = "user@example.com";
        String code = "123456";

        PasswordResetToken token = PasswordResetToken.builder()
                .token("some-token")
                .userId(10L)
                .email(new Email(email))
                .verificationCode(code)
                .expiresAt(FIXED_NOW.plusMinutes(30))
                .usedAt(FIXED_NOW.minusMinutes(5))
                .build();

        given(tokenRepository.findLatestActiveByEmail(email)).willReturn(Optional.of(token));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when & then
        assertThatThrownBy(() -> service.verifyCode(email, code))
                .isInstanceOf(InvalidPasswordResetTokenException.class)
                .hasMessageContaining("이미 사용");
    }

    @Test
    @DisplayName("유효한 코드로 resetPasswordWithCode 호출 시 비밀번호 변경에 성공한다")
    void should_resetPassword_when_validCode() {
        // given
        String email = "user@example.com";
        String code = "123456";
        String newPassword = "NewPassword1!";

        PasswordResetToken token = PasswordResetToken.builder()
                .token("some-token")
                .userId(10L)
                .email(new Email(email))
                .verificationCode(code)
                .expiresAt(FIXED_NOW.plusMinutes(30))
                .build();

        User user = User.builder()
                .id(10L)
                .email(new Email(email))
                .nickname("테스트유저")
                .passwordHash("oldHash")
                .build();

        given(tokenRepository.findLatestActiveByEmail(email)).willReturn(Optional.of(token));
        given(userRepository.findById(10L)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when
        service.resetPasswordWithCode(email, code, newPassword);

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(passwordEncoder.matches(newPassword, userCaptor.getValue().getPasswordHash())).isTrue();

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().isUsed()).isTrue();
    }

    @Test
    @DisplayName("failedAttempts가 5인 토큰으로 resetPasswordWithCode 호출 시 maxAttemptsExceeded 예외가 발생한다")
    void should_throwMaxAttempts_when_resetWithExceededAttempts() {
        // given
        String email = "user@example.com";
        String code = "123456";

        PasswordResetToken token = PasswordResetToken.builder()
                .token("some-token")
                .userId(10L)
                .email(new Email(email))
                .verificationCode(code)
                .expiresAt(FIXED_NOW.plusMinutes(30))
                .failedAttempts(5)
                .build();

        given(tokenRepository.findLatestActiveByEmail(email)).willReturn(Optional.of(token));

        // when & then
        assertThatThrownBy(() -> service.resetPasswordWithCode(email, code, "NewPassword1!"))
                .isInstanceOf(InvalidPasswordResetTokenException.class)
                .hasMessageContaining("초과");
    }
}
