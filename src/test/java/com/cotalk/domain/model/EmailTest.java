package com.cotalk.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Email 값 객체 테스트.
 *
 * @author seunggu.lee
 */
@DisplayName("Email")
class EmailTest {

    @Nested
    @DisplayName("생성 시")
    class Creation {

        @Test
        @DisplayName("유효한 이메일로 Email을 생성할 수 있다")
        void should_createEmail_when_validEmail() {
            // given
            String validEmail = "user@example.com";

            // when
            Email email = new Email(validEmail);

            // then
            assertThat(email.value()).isEqualTo(validEmail);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "simple@example.com",
                "user+tag@example.com",
                "user.name@example.com",
                "user_name@example.com",
                "user@sub.domain.com",
                "USER@EXAMPLE.COM"
        })
        @DisplayName("다양한 유효 이메일 형식을 허용한다")
        void should_createEmail_when_variousValidFormats(String validEmail) {
            // when
            Email email = new Email(validEmail);

            // then
            assertThat(email.value()).isEqualTo(validEmail);
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("null이면 예외가 발생한다")
        void should_throwException_when_emailIsNull(String nullEmail) {
            // when & then
            assertThatThrownBy(() -> new Email(nullEmail))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("올바른 이메일 형식이 아닙니다.");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "",
                "   ",
                "not-an-email",
                "@example.com",
                "user@",
                "user@.com",
                "user@example",
                "user @example.com",
                "user@@example.com"
        })
        @DisplayName("유효하지 않은 이메일 형식이면 예외가 발생한다")
        void should_throwException_when_invalidEmail(String invalidEmail) {
            // when & then
            assertThatThrownBy(() -> new Email(invalidEmail))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("올바른 이메일 형식이 아닙니다.");
        }
    }

    @Nested
    @DisplayName("동등성")
    class Equality {

        @Test
        @DisplayName("같은 값의 Email은 동등하다")
        void should_beEqual_when_sameValue() {
            // given
            Email email1 = new Email("user@example.com");
            Email email2 = new Email("user@example.com");

            // when & then
            assertThat(email1).isEqualTo(email2);
            assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
        }

        @Test
        @DisplayName("다른 값의 Email은 동등하지 않다")
        void should_notBeEqual_when_differentValue() {
            // given
            Email email1 = new Email("user1@example.com");
            Email email2 = new Email("user2@example.com");

            // when & then
            assertThat(email1).isNotEqualTo(email2);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringMethod {

        @Test
        @DisplayName("toString은 이메일 값을 반환한다")
        void should_returnValue_when_toStringCalled() {
            // given
            Email email = new Email("user@example.com");

            // when & then
            assertThat(email.toString()).isEqualTo("user@example.com");
        }
    }
}
