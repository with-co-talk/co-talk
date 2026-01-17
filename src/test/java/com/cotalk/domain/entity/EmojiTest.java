package com.cotalk.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Emoji 열거형")
class EmojiTest {

    @Nested
    @DisplayName("fromString 메서드")
    class FromString {

        @Test
        @DisplayName("이모지 문자로 Emoji를 찾을 수 있다")
        void should_findEmoji_when_givenEmojiCharacter() {
            // when & then
            assertThat(Emoji.fromString("👍")).isEqualTo(Emoji.THUMBS_UP);
            assertThat(Emoji.fromString("👎")).isEqualTo(Emoji.THUMBS_DOWN);
            assertThat(Emoji.fromString("❤️")).isEqualTo(Emoji.HEART);
            assertThat(Emoji.fromString("😂")).isEqualTo(Emoji.LAUGHING);
            assertThat(Emoji.fromString("😮")).isEqualTo(Emoji.SURPRISED);
            assertThat(Emoji.fromString("😢")).isEqualTo(Emoji.SAD);
            assertThat(Emoji.fromString("🔥")).isEqualTo(Emoji.FIRE);
            assertThat(Emoji.fromString("🎉")).isEqualTo(Emoji.PARTY);
            assertThat(Emoji.fromString("👏")).isEqualTo(Emoji.CLAPPING);
            assertThat(Emoji.fromString("✅")).isEqualTo(Emoji.CHECK);
        }

        @Test
        @DisplayName("이모지 이름으로 Emoji를 찾을 수 있다")
        void should_findEmoji_when_givenEmojiName() {
            // when & then
            assertThat(Emoji.fromString("thumbsup")).isEqualTo(Emoji.THUMBS_UP);
            assertThat(Emoji.fromString("thumbsdown")).isEqualTo(Emoji.THUMBS_DOWN);
            assertThat(Emoji.fromString("heart")).isEqualTo(Emoji.HEART);
            assertThat(Emoji.fromString("laughing")).isEqualTo(Emoji.LAUGHING);
            assertThat(Emoji.fromString("surprised")).isEqualTo(Emoji.SURPRISED);
            assertThat(Emoji.fromString("sad")).isEqualTo(Emoji.SAD);
            assertThat(Emoji.fromString("fire")).isEqualTo(Emoji.FIRE);
            assertThat(Emoji.fromString("party")).isEqualTo(Emoji.PARTY);
            assertThat(Emoji.fromString("clapping")).isEqualTo(Emoji.CLAPPING);
            assertThat(Emoji.fromString("check")).isEqualTo(Emoji.CHECK);
        }

        @Test
        @DisplayName("null을 전달하면 null을 반환한다")
        void should_returnNull_when_givenNull() {
            // when
            Emoji result = Emoji.fromString(null);

            // then
            assertThat(result).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("빈 문자열이나 공백을 전달하면 null을 반환한다")
        void should_returnNull_when_givenEmptyOrBlank(String value) {
            // when
            Emoji result = Emoji.fromString(value);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("알 수 없는 문자열을 전달하면 null을 반환한다")
        void should_returnNull_when_givenUnknownValue() {
            // when
            Emoji result = Emoji.fromString("unknown");

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("대소문자를 구분한다 (name은 소문자)")
        void should_beCaseSensitive_forName() {
            // when
            Emoji result = Emoji.fromString("THUMBSUP");

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("isValid 메서드")
    class IsValid {

        @Test
        @DisplayName("유효한 이모지 문자는 true를 반환한다")
        void should_returnTrue_when_validEmojiCharacter() {
            // when & then
            assertThat(Emoji.isValid("👍")).isTrue();
            assertThat(Emoji.isValid("❤️")).isTrue();
            assertThat(Emoji.isValid("🔥")).isTrue();
        }

        @Test
        @DisplayName("유효한 이모지 이름은 true를 반환한다")
        void should_returnTrue_when_validEmojiName() {
            // when & then
            assertThat(Emoji.isValid("thumbsup")).isTrue();
            assertThat(Emoji.isValid("heart")).isTrue();
            assertThat(Emoji.isValid("fire")).isTrue();
        }

        @Test
        @DisplayName("null은 false를 반환한다")
        void should_returnFalse_when_null() {
            // when
            boolean result = Emoji.isValid(null);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("빈 문자열은 false를 반환한다")
        void should_returnFalse_when_empty() {
            // when
            boolean result = Emoji.isValid("");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("알 수 없는 값은 false를 반환한다")
        void should_returnFalse_when_unknown() {
            // when
            boolean result = Emoji.isValid("not_an_emoji");

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getCharacter 메서드")
    class GetCharacter {

        @Test
        @DisplayName("각 이모지의 문자를 반환한다")
        void should_returnCorrectCharacter_forEachEmoji() {
            // then
            assertThat(Emoji.THUMBS_UP.getCharacter()).isEqualTo("👍");
            assertThat(Emoji.THUMBS_DOWN.getCharacter()).isEqualTo("👎");
            assertThat(Emoji.HEART.getCharacter()).isEqualTo("❤️");
            assertThat(Emoji.LAUGHING.getCharacter()).isEqualTo("😂");
            assertThat(Emoji.SURPRISED.getCharacter()).isEqualTo("😮");
            assertThat(Emoji.SAD.getCharacter()).isEqualTo("😢");
            assertThat(Emoji.FIRE.getCharacter()).isEqualTo("🔥");
            assertThat(Emoji.PARTY.getCharacter()).isEqualTo("🎉");
            assertThat(Emoji.CLAPPING.getCharacter()).isEqualTo("👏");
            assertThat(Emoji.CHECK.getCharacter()).isEqualTo("✅");
        }
    }

    @Nested
    @DisplayName("getName 메서드")
    class GetName {

        @Test
        @DisplayName("각 이모지의 이름을 반환한다")
        void should_returnCorrectName_forEachEmoji() {
            // then
            assertThat(Emoji.THUMBS_UP.getName()).isEqualTo("thumbsup");
            assertThat(Emoji.THUMBS_DOWN.getName()).isEqualTo("thumbsdown");
            assertThat(Emoji.HEART.getName()).isEqualTo("heart");
            assertThat(Emoji.LAUGHING.getName()).isEqualTo("laughing");
            assertThat(Emoji.SURPRISED.getName()).isEqualTo("surprised");
            assertThat(Emoji.SAD.getName()).isEqualTo("sad");
            assertThat(Emoji.FIRE.getName()).isEqualTo("fire");
            assertThat(Emoji.PARTY.getName()).isEqualTo("party");
            assertThat(Emoji.CLAPPING.getName()).isEqualTo("clapping");
            assertThat(Emoji.CHECK.getName()).isEqualTo("check");
        }
    }

    @Nested
    @DisplayName("이모지 열거형")
    class EmojiEnum {

        @Test
        @DisplayName("10개의 이모지가 정의되어 있다")
        void should_haveTenEmojis() {
            // then
            assertThat(Emoji.values()).hasSize(10);
        }

        @Test
        @DisplayName("모든 이모지가 정의되어 있다")
        void should_haveAllEmojis() {
            // then
            assertThat(Emoji.values()).containsExactlyInAnyOrder(
                    Emoji.THUMBS_UP,
                    Emoji.THUMBS_DOWN,
                    Emoji.HEART,
                    Emoji.LAUGHING,
                    Emoji.SURPRISED,
                    Emoji.SAD,
                    Emoji.FIRE,
                    Emoji.PARTY,
                    Emoji.CLAPPING,
                    Emoji.CHECK
            );
        }
    }
}
