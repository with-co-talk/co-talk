package com.cotalk.domain.validator;

import com.cotalk.domain.entity.Emoji;
import com.cotalk.domain.exception.InvalidEmojiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MessageValidator 테스트")
class MessageValidatorTest {

    private final MessageValidator validator = new MessageValidator();

    @Nested
    @DisplayName("메시지 내용 검증")
    class ContentValidation {

        @Test
        @DisplayName("유효한 메시지 내용이면 예외가 발생하지 않음")
        void should_notThrowException_when_validContent() {
            // given
            String validContent = "안녕하세요";

            // when & then
            assertThatCode(() -> validator.validateContent(validContent))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  "})
        @DisplayName("빈 메시지 내용이면 예외 발생")
        void should_throwException_when_emptyContent(String emptyContent) {
            // when & then
            assertThatThrownBy(() -> validator.validateContent(emptyContent))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("메시지 내용은 비어있을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("이모지 검증")
    class EmojiValidation {

        @Test
        @DisplayName("유효한 이모지면 Emoji enum을 반환함")
        void should_returnEmoji_when_validEmoji() {
            // given
            String validEmoji = "👍";

            // when
            Emoji result = validator.validateAndParseEmoji(validEmoji);

            // then
            assertThatCode(() -> validator.validateAndParseEmoji(validEmoji))
                    .doesNotThrowAnyException();
            assertThat(result).isEqualTo(Emoji.THUMBS_UP);
        }

        @Test
        @DisplayName("이모지 이름으로도 변환 가능")
        void should_returnEmoji_when_validEmojiName() {
            // given
            String emojiName = "thumbsup";

            // when
            Emoji result = validator.validateAndParseEmoji(emojiName);

            // then
            assertThat(result).isEqualTo(Emoji.THUMBS_UP);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  "})
        @DisplayName("빈 이모지면 예외 발생")
        void should_throwException_when_emptyEmoji(String emptyEmoji) {
            // when & then
            assertThatThrownBy(() -> validator.validateAndParseEmoji(emptyEmoji))
                    .isInstanceOf(InvalidEmojiException.class)
                    .hasMessageContaining("유효하지 않은 이모지");
        }

        @Test
        @DisplayName("유효하지 않은 이모지 형식이면 예외 발생")
        void should_throwException_when_invalidEmoji() {
            // given
            String invalidEmoji = "invalid-emoji";

            // when & then
            assertThatThrownBy(() -> validator.validateAndParseEmoji(invalidEmoji))
                    .isInstanceOf(InvalidEmojiException.class)
                    .hasMessageContaining("유효하지 않은 이모지");
        }
    }
}
