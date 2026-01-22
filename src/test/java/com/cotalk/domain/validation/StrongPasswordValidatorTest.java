package com.cotalk.domain.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StrongPasswordValidator 테스트.
 *
 * @author seunggu.lee
 */
@DisplayName("StrongPasswordValidator")
class StrongPasswordValidatorTest {

    private StrongPasswordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new StrongPasswordValidator();
    }

    @Nested
    @DisplayName("유효한 비밀번호")
    class ValidPasswordTest {

        @ParameterizedTest
        @ValueSource(strings = {
                "Password1!",
                "MySecure@123",
                "Test1234$",
                "Abcd1234&",
                "P@ssw0rd!",
                "StrongPass1%"
        })
        @DisplayName("should_returnTrue_when_passwordMeetsAllRequirements")
        void should_returnTrue_when_passwordMeetsAllRequirements(String password) {
            // when
            boolean result = validator.isValid(password, null);

            // then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("유효하지 않은 비밀번호")
    class InvalidPasswordTest {

        @Test
        @DisplayName("should_returnFalse_when_passwordIsNull")
        void should_returnFalse_when_passwordIsNull() {
            // when
            boolean result = validator.isValid(null, null);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should_returnFalse_when_passwordIsBlank")
        void should_returnFalse_when_passwordIsBlank() {
            // when
            boolean result = validator.isValid("   ", null);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should_returnFalse_when_passwordTooShort")
        void should_returnFalse_when_passwordTooShort() {
            // given
            String password = "Pass1!";  // 6자 - 8자 미만

            // when
            boolean result = validator.isValid(password, null);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should_returnFalse_when_noUppercase")
        void should_returnFalse_when_noUppercase() {
            // given
            String password = "password1!";  // 대문자 없음

            // when
            boolean result = validator.isValid(password, null);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should_returnFalse_when_noLowercase")
        void should_returnFalse_when_noLowercase() {
            // given
            String password = "PASSWORD1!";  // 소문자 없음

            // when
            boolean result = validator.isValid(password, null);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should_returnFalse_when_noDigit")
        void should_returnFalse_when_noDigit() {
            // given
            String password = "Password!";  // 숫자 없음

            // when
            boolean result = validator.isValid(password, null);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should_returnFalse_when_noSpecialCharacter")
        void should_returnFalse_when_noSpecialCharacter() {
            // given
            String password = "Password1";  // 특수문자 없음

            // when
            boolean result = validator.isValid(password, null);

            // then
            assertThat(result).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "12345678",         // 숫자만
                "abcdefgh",         // 소문자만
                "ABCDEFGH",         // 대문자만
                "!@#$%^&*",         // 특수문자만
                "Simple",           // 너무 단순
                "password",         // 소문자만
                "PASSWORD",         // 대문자만
        })
        @DisplayName("should_returnFalse_when_passwordDoesNotMeetRequirements")
        void should_returnFalse_when_passwordDoesNotMeetRequirements(String password) {
            // when
            boolean result = validator.isValid(password, null);

            // then
            assertThat(result).isFalse();
        }
    }
}
