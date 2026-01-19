package com.cotalk.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HtmlSanitizer 테스트.
 *
 * @author seunggu.lee
 */
@DisplayName("HtmlSanitizer")
class HtmlSanitizerTest {

    private final HtmlSanitizer htmlSanitizer = new HtmlSanitizer();

    @Nested
    @DisplayName("sanitize 메서드")
    class SanitizeMethod {

        @Test
        @DisplayName("null 입력 시 빈 문자열 반환")
        void should_ReturnEmptyString_when_NullInput() {
            // when
            String result = htmlSanitizer.sanitize(null);

            // then
            assertThat(result).isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  "})
        @DisplayName("빈 문자열이나 공백만 있는 경우 그대로 반환")
        void should_ReturnAsIs_when_BlankInput(String input) {
            // when
            String result = htmlSanitizer.sanitize(input);

            // then
            assertThat(result).isEqualTo(input == null ? "" : input);
        }

        @Test
        @DisplayName("HTML 특수문자를 이스케이프 처리한다")
        void should_EscapeHtmlSpecialCharacters() {
            // given
            String input = "<script>alert('XSS')</script>";

            // when
            String result = htmlSanitizer.sanitize(input);

            // then
            assertThat(result).isEqualTo("&lt;script&gt;alert(&#x27;XSS&#x27;)&lt;/script&gt;");
        }

        @Test
        @DisplayName("앰퍼샌드 문자를 이스케이프 처리한다")
        void should_EscapeAmpersand() {
            // given
            String input = "A & B";

            // when
            String result = htmlSanitizer.sanitize(input);

            // then
            assertThat(result).isEqualTo("A &amp; B");
        }

        @Test
        @DisplayName("큰따옴표를 이스케이프 처리한다")
        void should_EscapeDoubleQuote() {
            // given
            String input = "He said \"Hello\"";

            // when
            String result = htmlSanitizer.sanitize(input);

            // then
            assertThat(result).isEqualTo("He said &quot;Hello&quot;");
        }

        @Test
        @DisplayName("작은따옴표를 이스케이프 처리한다")
        void should_EscapeSingleQuote() {
            // given
            String input = "It's a test";

            // when
            String result = htmlSanitizer.sanitize(input);

            // then
            assertThat(result).isEqualTo("It&#x27;s a test");
        }

        @Test
        @DisplayName("일반 텍스트는 그대로 유지한다")
        void should_KeepNormalTextAsIs() {
            // given
            String input = "Hello World 123";

            // when
            String result = htmlSanitizer.sanitize(input);

            // then
            assertThat(result).isEqualTo("Hello World 123");
        }

        @Test
        @DisplayName("모든 HTML 특수문자를 동시에 이스케이프 처리한다")
        void should_EscapeAllHtmlSpecialCharacters() {
            // given
            String input = "<div class=\"test\">It's & more</div>";

            // when
            String result = htmlSanitizer.sanitize(input);

            // then
            assertThat(result).isEqualTo("&lt;div class=&quot;test&quot;&gt;It&#x27;s &amp; more&lt;/div&gt;");
        }

        @Test
        @DisplayName("XSS 공격 시도 문자열을 이스케이프 처리한다")
        void should_EscapeXssAttackString() {
            // given
            String input = "<img src=x onerror=alert('XSS')>";

            // when
            String result = htmlSanitizer.sanitize(input);

            // then
            assertThat(result).isEqualTo("&lt;img src=x onerror=alert(&#x27;XSS&#x27;)&gt;");
        }
    }

    @Nested
    @DisplayName("escape 정적 메서드")
    class EscapeStaticMethod {

        @Test
        @DisplayName("null 입력 시 빈 문자열 반환")
        void should_ReturnEmptyString_when_NullInput() {
            // when
            String result = HtmlSanitizer.escape(null);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("HTML 특수문자를 이스케이프 처리한다")
        void should_EscapeHtmlSpecialCharacters() {
            // given
            String input = "<script>alert('XSS')</script>";

            // when
            String result = HtmlSanitizer.escape(input);

            // then
            assertThat(result).isEqualTo("&lt;script&gt;alert(&#x27;XSS&#x27;)&lt;/script&gt;");
        }

        @Test
        @DisplayName("sanitize 메서드와 동일한 결과를 반환한다")
        void should_ReturnSameResultAsSanitize() {
            // given
            String input = "<div>Test & More</div>";
            HtmlSanitizer instance = new HtmlSanitizer();

            // when
            String instanceResult = instance.sanitize(input);
            String staticResult = HtmlSanitizer.escape(input);

            // then
            assertThat(staticResult).isEqualTo(instanceResult);
        }
    }
}
