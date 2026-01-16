package com.cotalk.domain.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserValidator 테스트")
class UserValidatorTest {

    private final UserValidator validator = new UserValidator();

    @Nested
    @DisplayName("이메일 검증")
    class EmailValidation {

        @Test
        @DisplayName("유효한 이메일이면 예외가 발생하지 않음")
        void should_notThrowException_when_validEmail() {
            // given
            String validEmail = "test@example.com";

            // when & then
            assertThatCode(() -> validator.validateEmail(validEmail))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "invalid", "invalid@", "@example.com", "invalid@.com"})
        @DisplayName("잘못된 이메일 형식이면 예외 발생")
        void should_throwException_when_invalidEmail(String invalidEmail) {
            // when & then
            assertThatThrownBy(() -> validator.validateEmail(invalidEmail))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("올바른 이메일 형식이 아닙니다");
        }
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
}
