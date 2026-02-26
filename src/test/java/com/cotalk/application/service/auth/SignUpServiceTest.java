package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.EmailVerificationToken;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.outbound.EmailSender;
import com.cotalk.domain.port.outbound.EmailVerificationTokenRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.PasswordEncoderPort;
import com.cotalk.domain.port.outbound.TimeProvider;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.validator.UserValidator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignUpService")
class SignUpServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private UserValidator userValidator;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private EmailSender emailSender;

    @Mock
    private TimeProvider timeProvider;

    private SignUpService signUpService;

    private static final java.time.LocalDateTime FIXED_NOW = java.time.LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void setUp() {
        String frontendUrl = "http://localhost:3000";
        signUpService = new SignUpService(userRepository, passwordEncoder, idGenerator, userValidator,
                emailVerificationTokenRepository, emailSender, timeProvider, frontendUrl);
    }

    @Nested
    @DisplayName("회원가입 성공 시")
    class SignUpSuccess {

        @Test
        @DisplayName("유효한 정보로 회원가입하면 사용자 ID를 반환한다")
        void should_ReturnUserId_when_ValidInputsProvided() {
            // given
            String email = "test@example.com";
            String password = "password123";
            String nickname = "testUser";
            String encodedPassword = "encodedPassword";
            Long expectedUserId = 123456789L;

            given(userRepository.existsByEmail(email)).willReturn(false);
            given(userRepository.existsByNickname(nickname)).willReturn(false);
            given(passwordEncoder.encode(password)).willReturn(encodedPassword);
            given(idGenerator.nextId()).willReturn(expectedUserId);
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(timeProvider.now()).willReturn(FIXED_NOW);

            // when
            Long result = signUpService.signUp(email, password, nickname);

            // then
            assertThat(result).isEqualTo(expectedUserId);
        }

        @Test
        @DisplayName("비밀번호는 암호화되어 저장된다")
        void should_EncodePassword_when_SigningUp() {
            // given
            String email = "test@example.com";
            String password = "password123";
            String nickname = "testUser";
            String encodedPassword = "encodedPassword";

            given(userRepository.existsByEmail(email)).willReturn(false);
            given(userRepository.existsByNickname(nickname)).willReturn(false);
            given(passwordEncoder.encode(password)).willReturn(encodedPassword);
            given(idGenerator.nextId()).willReturn(1L);
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(timeProvider.now()).willReturn(FIXED_NOW);

            // when
            signUpService.signUp(email, password, nickname);

            // then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo(encodedPassword);
        }

        @Test
        @DisplayName("Snowflake ID가 사용자 ID로 설정된다")
        void should_UseSnowflakeId_when_SigningUp() {
            // given
            String email = "test@example.com";
            String password = "password123";
            String nickname = "testUser";
            Long snowflakeId = 987654321L;

            given(userRepository.existsByEmail(email)).willReturn(false);
            given(userRepository.existsByNickname(nickname)).willReturn(false);
            given(passwordEncoder.encode(password)).willReturn("encoded");
            given(idGenerator.nextId()).willReturn(snowflakeId);
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(timeProvider.now()).willReturn(FIXED_NOW);

            // when
            signUpService.signUp(email, password, nickname);

            // then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getId()).isEqualTo(snowflakeId);
        }
    }

    @Nested
    @DisplayName("회원가입 실패 시")
    class SignUpFailure {

        @Test
        @DisplayName("이미 존재하는 이메일이면 예외가 발생한다")
        void should_ThrowException_when_EmailAlreadyExists() {
            // given
            String email = "existing@example.com";
            given(userRepository.existsByEmail(email)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> signUpService.signUp(email, "password123", "nickname"))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("이메일");
        }

        @Test
        @DisplayName("이미 존재하는 닉네임이면 예외가 발생한다")
        void should_ThrowException_when_NicknameAlreadyExists() {
            // given
            String email = "test@example.com";
            String nickname = "existingNickname";
            given(userRepository.existsByEmail(email)).willReturn(false);
            given(userRepository.existsByNickname(nickname)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> signUpService.signUp(email, "password123", nickname))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("닉네임");
        }

        @Test
        @DisplayName("잘못된 이메일 형식이면 예외가 발생한다")
        void should_ThrowException_when_InvalidEmailFormat() {
            // given
            String invalidEmail = "invalid-email";

            // when & then - Email VO 생성 시 IllegalArgumentException 발생
            assertThatThrownBy(() -> signUpService.signUp(invalidEmail, "password123", "nickname"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이메일");
        }

        @Test
        @DisplayName("비밀번호가 8자 미만이면 예외가 발생한다")
        void should_ThrowException_when_PasswordTooShort() {
            // given
            String shortPassword = "short";
            willThrow(new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다."))
                    .given(userValidator).validatePassword(shortPassword);

            // when & then
            assertThatThrownBy(() -> signUpService.signUp("test@example.com", shortPassword, "nickname"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("비밀번호");
        }
    }
}
