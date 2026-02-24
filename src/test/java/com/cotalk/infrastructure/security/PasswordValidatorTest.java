package com.cotalk.infrastructure.security;

import com.cotalk.domain.validation.StrongPassword;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PasswordValidator 테스트.
 *
 * <p>domain.validation.StrongPassword 기반 비밀번호 검증을 검증한다.</p>
 *
 * @author seunggu.lee
 */
@DisplayName("PasswordValidator")
class PasswordValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("비밀번호 검증")
    class PasswordValidation {

        @Test
        @DisplayName("유효한 비밀번호는 검증을 통과한다")
        void should_PassValidation_when_ValidPassword() {
            // given
            TestDto dto = new TestDto("ValidPass123!");

            // when
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  "})
        @DisplayName("null, 빈 문자열, 공백만 있는 비밀번호는 검증 실패")
        void should_FailValidation_when_BlankPassword(String password) {
            // given
            TestDto dto = new TestDto(password);

            // when
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"short", "1234567", "Short1"})
        @DisplayName("8자 미만 비밀번호는 검증 실패")
        void should_FailValidation_when_TooShort(String password) {
            // given
            TestDto dto = new TestDto(password);

            // when
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations.iterator().next().getMessage())
                    .contains("8-128자");
        }

        @Test
        @DisplayName("129자 이상 비밀번호는 검증 실패")
        void should_FailValidation_when_TooLong() {
            // given
            String longPassword = "A".repeat(129) + "a1!";
            TestDto dto = new TestDto(longPassword);

            // when
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"password123!", "PASSWORD123!", "Password123", "Password!"})
        @DisplayName("대문자, 소문자, 숫자, 특수문자 중 하나라도 없으면 검증 실패")
        void should_FailValidation_when_MissingRequiredCharacter(String password) {
            // given
            TestDto dto = new TestDto(password);

            // when
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "ValidPass123!",
                "Another1@",
                "Test123#",
                "MyPass1$",
                "Secure1%",
                "Strong1^",
                "GoodPass1-",
                "BestPass1=",
                "TopPass1+",
                "ValidPass123!@#$%^&()-_=+"
        })
        @DisplayName("모든 요구사항을 만족하는 비밀번호는 검증 통과")
        void should_PassValidation_when_AllRequirementsMet(String password) {
            // given
            TestDto dto = new TestDto(password);

            // when
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("128자 정확한 비밀번호는 검증 통과")
        void should_PassValidation_when_Exactly128Characters() {
            // given
            String password = "A".repeat(125) + "a1!";
            TestDto dto = new TestDto(password);

            // when
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("8자 정확한 비밀번호는 검증 통과")
        void should_PassValidation_when_Exactly8Characters() {
            // given
            TestDto dto = new TestDto("Valid1!@");

            // when
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isEmpty();
        }
    }

    /**
     * 테스트용 DTO.
     */
    private record TestDto(
            @StrongPassword
            String password
    ) {
    }
}
