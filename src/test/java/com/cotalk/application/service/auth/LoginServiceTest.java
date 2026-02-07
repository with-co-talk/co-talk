package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.auth.LoginResult;
import com.cotalk.domain.port.inbound.user.UpdateUserOnlineStatusUseCase;
import com.cotalk.domain.port.outbound.AuthTokenPort;
import com.cotalk.domain.port.outbound.PasswordEncoderPort;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginService")
class LoginServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private AuthTokenPort authTokenPort;

    @Mock
    private UpdateUserOnlineStatusUseCase updateUserOnlineStatusUseCase;

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new LoginService(userRepository, passwordEncoder, authTokenPort, updateUserOnlineStatusUseCase);
    }

    @Nested
    @DisplayName("로그인 성공 시")
    class LoginSuccess {

        @Test
        @DisplayName("유효한 자격 증명으로 로그인하면 JWT 토큰을 반환한다")
        void should_ReturnToken_when_ValidCredentials() {
            // given
            String email = "test@example.com";
            String password = "password123";
            String expectedToken = "jwt.token.here";
            Long userId = 1L;

            User user = User.builder()
                    .id(userId)
                    .email(email)
                    .passwordHash("hashedPassword")
                    .nickname("testUser")
                    .build();

            given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
            given(passwordEncoder.matches(password, "hashedPassword")).willReturn(true);
            given(authTokenPort.generateAccessToken(userId)).willReturn(expectedToken);

            // when
            LoginResult result = loginService.login(email, password);

            // then
            assertThat(result.accessToken()).isEqualTo(expectedToken);
            assertThat(result.userId()).isEqualTo(userId);
            // 로그인 시 온라인 상태로 변경되는지 확인
            org.mockito.Mockito.verify(updateUserOnlineStatusUseCase).setOnline(userId);
        }
    }

    @Nested
    @DisplayName("로그인 실패 시")
    class LoginFailure {

        @Test
        @DisplayName("존재하지 않는 이메일이면 예외가 발생한다")
        void should_ThrowException_when_EmailNotFound() {
            // given
            String email = "notfound@example.com";
            given(userRepository.findByEmail(email)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> loginService.login(email, "password"))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("이메일");
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 예외가 발생한다")
        void should_ThrowException_when_PasswordNotMatch() {
            // given
            String email = "test@example.com";
            String wrongPassword = "wrongPassword";

            User user = User.builder()
                    .id(1L)
                    .email(email)
                    .passwordHash("hashedPassword")
                    .nickname("testUser")
                    .build();

            given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
            given(passwordEncoder.matches(wrongPassword, "hashedPassword")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> loginService.login(email, wrongPassword))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("비밀번호");
        }
    }

    @Nested
    @DisplayName("이메일로 사용자 ID 조회")
    class GetUserIdByEmail {

        @Test
        @DisplayName("유효한 이메일로 사용자 ID를 조회할 수 있다")
        void should_ReturnUserId_when_ValidEmail() {
            // given
            String email = "test@example.com";
            Long userId = 1L;

            User user = User.builder()
                    .id(userId)
                    .email(email)
                    .passwordHash("hash")
                    .nickname("testUser")
                    .build();

            given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

            // when
            Long result = loginService.getUserIdByEmail(email);

            // then
            assertThat(result).isEqualTo(userId);
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 예외가 발생한다")
        void should_ThrowException_when_EmailNotFound() {
            // given
            String email = "notfound@example.com";
            given(userRepository.findByEmail(email)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> loginService.getUserIdByEmail(email))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}
