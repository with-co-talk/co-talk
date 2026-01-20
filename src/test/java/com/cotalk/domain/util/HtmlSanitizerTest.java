package com.cotalk.domain.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HtmlSanitizer 테스트.
 *
 * @author seunggu.lee
 */
@DisplayName("HtmlSanitizer")
class HtmlSanitizerTest {

    @Nested
    @DisplayName("escape 메서드")
    class EscapeTest {

        @Test
        @DisplayName("should_returnEscapedString_when_containsHtmlCharacters")
        void should_returnEscapedString_when_containsHtmlCharacters() {
            // given
            String input = "<script>alert('xss')</script>";

            // when
            String result = HtmlSanitizer.escape(input);

            // then
            assertThat(result).isEqualTo("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;");
        }

        @Test
        @DisplayName("should_returnNull_when_inputIsNull")
        void should_returnNull_when_inputIsNull() {
            // when
            String result = HtmlSanitizer.escape(null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should_escapeAmpersand_when_containsAmpersand")
        void should_escapeAmpersand_when_containsAmpersand() {
            // given
            String input = "Tom & Jerry";

            // when
            String result = HtmlSanitizer.escape(input);

            // then
            assertThat(result).isEqualTo("Tom &amp; Jerry");
        }

        @Test
        @DisplayName("should_escapeQuotes_when_containsQuotes")
        void should_escapeQuotes_when_containsQuotes() {
            // given
            String input = "He said \"Hello\"";

            // when
            String result = HtmlSanitizer.escape(input);

            // then
            assertThat(result).isEqualTo("He said &quot;Hello&quot;");
        }

        @Test
        @DisplayName("should_returnSameString_when_noHtmlCharacters")
        void should_returnSameString_when_noHtmlCharacters() {
            // given
            String input = "Hello World";

            // when
            String result = HtmlSanitizer.escape(input);

            // then
            assertThat(result).isEqualTo("Hello World");
        }
    }

    @Nested
    @DisplayName("escapeOrDefault 메서드")
    class EscapeOrDefaultTest {

        @Test
        @DisplayName("should_returnDefaultValue_when_inputIsNull")
        void should_returnDefaultValue_when_inputIsNull() {
            // when
            String result = HtmlSanitizer.escapeOrDefault(null, "default");

            // then
            assertThat(result).isEqualTo("default");
        }

        @Test
        @DisplayName("should_returnDefaultValue_when_inputIsBlank")
        void should_returnDefaultValue_when_inputIsBlank() {
            // when
            String result = HtmlSanitizer.escapeOrDefault("   ", "default");

            // then
            assertThat(result).isEqualTo("default");
        }

        @Test
        @DisplayName("should_returnEscapedString_when_inputIsValid")
        void should_returnEscapedString_when_inputIsValid() {
            // given
            String input = "<b>bold</b>";

            // when
            String result = HtmlSanitizer.escapeOrDefault(input, "default");

            // then
            assertThat(result).isEqualTo("&lt;b&gt;bold&lt;/b&gt;");
        }
    }

    @Nested
    @DisplayName("unescape 메서드")
    class UnescapeTest {

        @Test
        @DisplayName("should_returnUnescapedString_when_containsEscapedCharacters")
        void should_returnUnescapedString_when_containsEscapedCharacters() {
            // given
            String input = "&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;";

            // when
            String result = HtmlSanitizer.unescape(input);

            // then
            assertThat(result).isEqualTo("<script>alert('xss')</script>");
        }

        @Test
        @DisplayName("should_returnNull_when_inputIsNull")
        void should_returnNull_when_inputIsNull() {
            // when
            String result = HtmlSanitizer.unescape(null);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("containsHtmlTags 메서드")
    class ContainsHtmlTagsTest {

        @Test
        @DisplayName("should_returnTrue_when_containsScriptTag")
        void should_returnTrue_when_containsScriptTag() {
            // given
            String input = "<script>alert('xss')</script>";

            // when
            boolean result = HtmlSanitizer.containsHtmlTags(input);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should_returnTrue_when_containsIframeTag")
        void should_returnTrue_when_containsIframeTag() {
            // given
            String input = "<iframe src='evil.com'></iframe>";

            // when
            boolean result = HtmlSanitizer.containsHtmlTags(input);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should_returnTrue_when_containsJavascriptProtocol")
        void should_returnTrue_when_containsJavascriptProtocol() {
            // given
            String input = "javascript:alert('xss')";

            // when
            boolean result = HtmlSanitizer.containsHtmlTags(input);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should_returnTrue_when_containsEventHandler")
        void should_returnTrue_when_containsEventHandler() {
            // given
            String input = "<img onerror=alert('xss')>";

            // when
            boolean result = HtmlSanitizer.containsHtmlTags(input);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should_returnFalse_when_noHtmlTags")
        void should_returnFalse_when_noHtmlTags() {
            // given
            String input = "This is a normal message";

            // when
            boolean result = HtmlSanitizer.containsHtmlTags(input);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should_returnFalse_when_inputIsNull")
        void should_returnFalse_when_inputIsNull() {
            // when
            boolean result = HtmlSanitizer.containsHtmlTags(null);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should_returnFalse_when_inputIsBlank")
        void should_returnFalse_when_inputIsBlank() {
            // when
            boolean result = HtmlSanitizer.containsHtmlTags("   ");

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("stripAllTags 메서드")
    class StripAllTagsTest {

        @Test
        @DisplayName("should_removeAllTags_when_containsHtmlTags")
        void should_removeAllTags_when_containsHtmlTags() {
            // given
            String input = "<p>Hello <b>World</b></p>";

            // when
            String result = HtmlSanitizer.stripAllTags(input);

            // then
            assertThat(result).isEqualTo("Hello World");
        }

        @Test
        @DisplayName("should_returnNull_when_inputIsNull")
        void should_returnNull_when_inputIsNull() {
            // when
            String result = HtmlSanitizer.stripAllTags(null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should_returnSameString_when_noTags")
        void should_returnSameString_when_noTags() {
            // given
            String input = "Hello World";

            // when
            String result = HtmlSanitizer.stripAllTags(input);

            // then
            assertThat(result).isEqualTo("Hello World");
        }
    }
}
