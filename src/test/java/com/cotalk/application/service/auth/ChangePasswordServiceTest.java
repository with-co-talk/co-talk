package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.PasswordMismatchException;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ChangePasswordService changePasswordService;

    private static final Long USER_ID = 1L;
    private static final String CURRENT_PASSWORD = "OldPassword1!";
    private static final String NEW_PASSWORD = "NewPassword1!";
    private static final String ENCODED_CURRENT = "$2a$10$currentEncoded";
    private static final String ENCODED_NEW = "$2a$10$newEncoded";

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        changePasswordService = new ChangePasswordService(userRepository, passwordEncoder, redisTemplate);
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
     * {@link PasswordMismatchException}이 발생해야 한다.
     */
    @Test
    @DisplayName("현재 비밀번호가 틀렸을 때 PasswordMismatchException 발생")
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
                .isInstanceOf(PasswordMismatchException.class)
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

        // Redis increment mock: 1~5 순차 반환
        given(valueOperations.increment(anyString()))
                .willReturn(1L, 2L, 3L, 4L, 5L);
        given(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).willReturn(true);

        // 5회 연속 실패
        for (int i = 0; i < 5; i++) {
            try {
                changePasswordService.changePassword(USER_ID, "wrongPassword", NEW_PASSWORD);
            } catch (PasswordMismatchException ignored) {
                // 예상된 예외
            }
        }

        // 6번째 시도 - Redis에 5가 저장되어 있으므로 rate limit 발생
        given(valueOperations.get("password:fail:" + USER_ID)).willReturn("5");
        given(redisTemplate.getExpire("password:fail:" + USER_ID, TimeUnit.SECONDS)).willReturn(500L);

        // when & then
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

        // Redis: checkRateLimit에서 get → null (아직 실패 없음)
        given(valueOperations.get(anyString())).willReturn(null);
        // Redis: recordFailure에서 increment
        given(valueOperations.increment(anyString())).willReturn(1L, 2L, 3L, 4L);
        given(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(redisTemplate.delete(anyString())).willReturn(true);

        // 4회 실패
        for (int i = 0; i < 4; i++) {
            try {
                changePasswordService.changePassword(USER_ID, "wrongPassword", NEW_PASSWORD);
            } catch (PasswordMismatchException ignored) {
                // 예상된 예외
            }
        }

        // 성공 - Redis에서 키 삭제 (카운터 초기화)
        changePasswordService.changePassword(USER_ID, CURRENT_PASSWORD, NEW_PASSWORD);

        // then - delete가 호출되었는지 검증
        verify(redisTemplate).delete("password:fail:" + USER_ID);
    }
}
