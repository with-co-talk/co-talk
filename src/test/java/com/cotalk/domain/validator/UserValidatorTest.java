package com.cotalk.domain.validator;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.SelfActionNotAllowedException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DisplayName("UserValidator 테스트")
@ExtendWith(MockitoExtension.class)
class UserValidatorTest {

    @Mock
    private UserRepository userRepository;

    private UserValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UserValidator(userRepository);
    }

    @Nested
    @DisplayName("비밀번호 검증")
    class PasswordValidation {

        @Test
        @DisplayName("8자 이상 비밀번호는 유효함")
        void should_notThrowException_when_validPassword() {
            // given
            String validPassword = "password123";

            // when & then
            assertThatCode(() -> validator.validatePassword(validPassword))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"1234567", "short"})
        @DisplayName("8자 미만 비밀번호는 예외 발생")
        void should_throwException_when_shortPassword(String shortPassword) {
            // when & then
            assertThatThrownBy(() -> validator.validatePassword(shortPassword))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("비밀번호는 최소 8자 이상이어야 합니다");
        }
    }

    @Nested
    @DisplayName("닉네임 검증")
    class NicknameValidation {

        @Test
        @DisplayName("유효한 닉네임이면 예외가 발생하지 않음")
        void should_notThrowException_when_validNickname() {
            // given
            String validNickname = "홍길동";

            // when & then
            assertThatCode(() -> validator.validateNickname(validNickname))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  "})
        @DisplayName("빈 닉네임이면 예외 발생")
        void should_throwException_when_emptyNickname(String emptyNickname) {
            // when & then
            assertThatThrownBy(() -> validator.validateNickname(emptyNickname))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("닉네임은 비어있을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("자기 자신 액션 검증")
    class SelfActionValidation {

        @Test
        @DisplayName("다른 사용자에 대한 액션이면 예외가 발생하지 않음")
        void should_notThrowException_when_differentUsers() {
            // given
            Long actorId = 1L;
            Long targetId = 2L;

            // when & then
            assertThatCode(() -> validator.validateNotSelfAction(actorId, targetId, "차단"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("자기 자신에 대한 액션이면 SelfActionNotAllowedException 발생")
        void should_throwException_when_sameUser() {
            // given
            Long userId = 1L;

            // when & then
            assertThatThrownBy(() -> validator.validateNotSelfAction(userId, userId, "차단"))
                    .isInstanceOf(SelfActionNotAllowedException.class)
                    .hasMessageContaining("자기 자신을 차단할 수 없습니다");
        }

        @Test
        @DisplayName("액션 유형이 메시지에 포함됨")
        void should_includeActionType_inMessage() {
            // given
            Long userId = 1L;

            // when & then
            assertThatThrownBy(() -> validator.validateNotSelfAction(userId, userId, "친구 요청"))
                    .isInstanceOf(SelfActionNotAllowedException.class)
                    .hasMessageContaining("친구 요청");
        }
    }

    @Nested
    @DisplayName("사용자 존재 검증")
    class UserExistsValidation {

        @Test
        @DisplayName("사용자가 존재하면 User 반환")
        void should_returnUser_when_userExists() {
            // given
            Long userId = 1L;
            User user = User.builder().id(userId).nickname("테스트").build();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // when
            User result = validator.validateUserExists(userId);

            // then
            assertThat(result).isEqualTo(user);
        }

        @Test
        @DisplayName("사용자가 존재하지 않으면 UserNotFoundException 발생")
        void should_throwException_when_userNotExists() {
            // given
            Long userId = 999L;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> validator.validateUserExists(userId))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("다중 사용자 존재 검증")
    class UsersExistValidation {

        @Test
        @DisplayName("모든 사용자가 존재하면 예외가 발생하지 않음")
        void should_notThrowException_when_allUsersExist() {
            // given
            List<Long> userIds = List.of(1L, 2L, 3L);
            List<User> users = List.of(
                    User.builder().id(1L).build(),
                    User.builder().id(2L).build(),
                    User.builder().id(3L).build()
            );
            when(userRepository.findAllById(userIds)).thenReturn(users);

            // when & then
            assertThatCode(() -> validator.validateUsersExist(userIds))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("존재하지 않는 사용자가 있으면 UserNotFoundException 발생")
        void should_throwException_when_someUserNotExists() {
            // given
            List<Long> userIds = List.of(1L, 2L, 999L);
            List<User> existingUsers = List.of(
                    User.builder().id(1L).build(),
                    User.builder().id(2L).build()
            );
            when(userRepository.findAllById(userIds)).thenReturn(existingUsers);

            // when & then
            assertThatThrownBy(() -> validator.validateUsersExist(userIds))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("빈 리스트는 예외가 발생하지 않음")
        void should_notThrowException_when_emptyList() {
            // given
            List<Long> emptyList = List.of();

            // when & then
            assertThatCode(() -> validator.validateUsersExist(emptyList))
                    .doesNotThrowAnyException();
        }
    }
}
