package com.cotalk.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Message 엔티티")
class MessageTest {

    @Nested
    @DisplayName("생성 시")
    class Creation {

        @Test
        @DisplayName("채팅방 ID, 발신자 ID, 내용으로 메시지를 생성할 수 있다")
        void should_CreateMessage_when_ValidInputsProvided() {
            // given
            Long chatRoomId = 1L;
            Long senderId = 2L;
            String content = "안녕하세요!";

            // when
            Message message = Message.builder()
                    .chatRoomId(chatRoomId)
                    .senderId(senderId)
                    .content(content)
                    .build();

            // then
            assertThat(message.getChatRoomId()).isEqualTo(chatRoomId);
            assertThat(message.getSenderId()).isEqualTo(senderId);
            assertThat(message.getContent()).isEqualTo(content);
        }

        @Test
        @DisplayName("기본 메시지 타입은 TEXT이다")
        void should_HaveTextType_when_Created() {
            // given & when
            Message message = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .content("테스트")
                    .build();

            // then
            assertThat(message.getType()).isEqualTo(Message.MessageType.TEXT);
        }
    }

    @Nested
    @DisplayName("내용 검증 시")
    class ValidateContent {

        @Test
        @DisplayName("빈 내용은 검증에 실패한다")
        void should_ThrowException_when_EmptyContent() {
            // given
            Message message = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .content("")
                    .build();

            // when & then
            assertThatThrownBy(() -> message.validateContent())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("내용");
        }

        @Test
        @DisplayName("null 내용은 검증에 실패한다")
        void should_ThrowException_when_NullContent() {
            // given
            Message message = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .content(null)
                    .build();

            // when & then
            assertThatThrownBy(() -> message.validateContent())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("유효한 내용은 검증에 성공한다")
        void should_NotThrowException_when_ValidContent() {
            // given
            Message message = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .content("유효한 메시지")
                    .build();

            // when & then (no exception)
            message.validateContent();
        }
    }
}
