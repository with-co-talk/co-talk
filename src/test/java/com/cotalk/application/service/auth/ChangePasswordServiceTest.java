package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.InvalidCredentialsException;
import com.cotalk.domain.exception.RateLimitExceededException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.outbound.PasswordEncoderPort;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link ChangePasswordService} 단위 테스트.
 * 비밀번호 변경 유스케이스의 정상/예외 시나리오를 검증한다.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    private ChangePasswordService changePasswordService;

    private static final Long USER_ID = 1L;
    private static final String CURRENT_PASSWORD = "OldPassword1!";
    private static final String NEW_PASSWORD = "NewPassword1!";
    private static final String ENCODED_CURRENT = "$2a$10$currentEncoded";
    private static final String ENCODED_NEW = "$2a$10$newEncoded";

    @BeforeEach
    void setUp() {
        changePasswordService = new ChangePasswordService(userRepository, passwordEncoder);
    }

    /**
     * 정상적인 비밀번호 변경 시나리오를 검증한다.
     * 현재 비밀번호가 일치하고, 새 비밀번호가 보안 요구사항을 충족하면
     * 비밀번호가 변경되어야 한다.
     */
    @Test
    @DisplayName("유효한 현재 비밀번호와 새 비밀번호로 비밀번호 변경 성공")
    void should_changePassword_when_validCurrentPasswordAndNewPassword() {
        // given
        User user = User.builder()
                .id(USER_ID)
                .email("user@example.com")
                .nickname("테스트유저")
                .passwordHash(ENCODED_CURRENT)
                .build();

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT)).willReturn(true);
        given(passwordEncoder.encode(NEW_PASSWORD)).willReturn(ENCODED_NEW);
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        changePasswordService.changePassword(USER_ID, CURRENT_PASSWORD, NEW_PASSWORD);

        // then
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(NEW_PASSWORD);
    }

    /**
     * 존재하지 않는 사용자 ID로 비밀번호 변경 시
     * {@link UserNotFoundException}이 발생해야 한다.
     */
    @Test
    @DisplayName("사용자를 찾을 수 없을 때 UserNotFoundException 발생")
    void should_throwUserNotFoundException_when_userNotFound() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                changePasswordService.changePassword(USER_ID, CURRENT_PASSWORD, NEW_PASSWORD))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    /**
     * 현재 비밀번호가 일치하지 않을 때
     * {@link InvalidCredentialsException}이 발생해야 한다.
     */
    @Test
    @DisplayName("현재 비밀번호가 틀렸을 때 InvalidCredentialsException 발생")
    void should_throwInvalidCredentialsException_when_wrongCurrentPassword() {
        // given
        User user = User.builder()
                .id(USER_ID)
                .email("user@example.com")
                .nickname("테스트유저")
                .passwordHash(ENCODED_CURRENT)
                .build();

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(eq("wrongPassword"), eq(ENCODED_CURRENT))).willReturn(false);

        // when & then
        assertThatThrownBy(() ->
                changePasswordService.changePassword(USER_ID, "wrongPassword", NEW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("현재 비밀번호가 일치하지 않습니다.");

        verify(userRepository, never()).save(any());
    }

    /**
     * 새 비밀번호가 보안 요구사항을 충족하지 않을 때
     * {@link IllegalArgumentException}이 발생해야 한다.
     */
    @Test
    @DisplayName("약한 새 비밀번호로 변경 시 IllegalArgumentException 발생")
    void should_throwIllegalArgumentException_when_weakNewPassword() {
        // given
        User user = User.builder()
                .id(USER_ID)
                .email("user@example.com")
                .nickname("테스트유저")
                .passwordHash(ENCODED_CURRENT)
                .build();

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT)).willReturn(true);

        // when & then
        assertThatThrownBy(() ->
                changePasswordService.changePassword(USER_ID, CURRENT_PASSWORD, "weak"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("대문자, 소문자, 숫자, 특수문자");

        verify(userRepository, never()).save(any());
    }

    /**
     * 5회 연속 비밀번호 실패 시
     * {@link RateLimitExceededException}이 발생해야 한다.
     */
    @Test
    @DisplayName("5회 연속 실패 시 RateLimitExceededException 발생")
    void should_throwRateLimitExceededException_when_fiveConsecutiveFailures() {
        // given
        User user = User.builder()
                .id(USER_ID)
                .email("user@example.com")
                .nickname("테스트유저")
                .passwordHash(ENCODED_CURRENT)
                .build();

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(eq("wrongPassword"), eq(ENCODED_CURRENT))).willReturn(false);

        // 5회 연속 실패
        for (int i = 0; i < 5; i++) {
            try {
                changePasswordService.changePassword(USER_ID, "wrongPassword", NEW_PASSWORD);
            } catch (InvalidCredentialsException ignored) {
                // 예상된 예외
            }
        }

        // when & then - 6번째 시도에서 rate limit 발생
        assertThatThrownBy(() ->
                changePasswordService.changePassword(USER_ID, "wrongPassword", NEW_PASSWORD))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("요청 한도를 초과했습니다.");
    }

    /**
     * 비밀번호 변경 성공 시 실패 카운터가 초기화되어야 한다.
     * 이전 실패 기록이 있더라도 성공하면 카운터가 리셋된다.
     */
    @Test
    @DisplayName("비밀번호 변경 성공 시 실패 카운터 초기화")
    void should_resetFailureCount_when_successfulChange() {
        // given
        User user = User.builder()
                .id(USER_ID)
                .email("user@example.com")
                .nickname("테스트유저")
                .passwordHash(ENCODED_CURRENT)
                .build();

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(eq("wrongPassword"), eq(ENCODED_CURRENT))).willReturn(false);
        given(passwordEncoder.matches(eq(CURRENT_PASSWORD), eq(ENCODED_CURRENT))).willReturn(true);
        given(passwordEncoder.encode(NEW_PASSWORD)).willReturn(ENCODED_NEW);
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        // 4회 실패
        for (int i = 0; i < 4; i++) {
            try {
                changePasswordService.changePassword(USER_ID, "wrongPassword", NEW_PASSWORD);
            } catch (InvalidCredentialsException ignored) {
                // 예상된 예외
            }
        }

        // 성공 - 카운터 초기화
        changePasswordService.changePassword(USER_ID, CURRENT_PASSWORD, NEW_PASSWORD);

        // 다시 4회 실패 - rate limit에 걸리지 않아야 함
        for (int i = 0; i < 4; i++) {
            try {
                changePasswordService.changePassword(USER_ID, "wrongPassword", NEW_PASSWORD);
            } catch (InvalidCredentialsException ignored) {
                // 예상된 예외 - RateLimitExceededException이 아님
            }
        }

        // then - 5번째도 InvalidCredentialsException (카운터가 리셋되었으므로)
        assertThatThrownBy(() ->
                changePasswordService.changePassword(USER_ID, "wrongPassword", NEW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
