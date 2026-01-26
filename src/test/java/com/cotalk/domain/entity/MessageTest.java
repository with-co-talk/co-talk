package com.cotalk.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

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

        @Test
        @DisplayName("이미지 메시지는 파일 URL이 필수이다")
        void should_ThrowException_when_ImageMessageWithoutFileUrl() {
            // given
            Message message = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .type(Message.MessageType.IMAGE)
                    .content("이미지 설명")
                    .fileUrl(null)
                    .build();

            // when & then
            assertThatThrownBy(() -> message.validateContent())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("파일 URL");
        }

        @Test
        @DisplayName("파일 메시지는 파일 URL이 필수이다")
        void should_ThrowException_when_FileMessageWithoutFileUrl() {
            // given
            Message message = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .type(Message.MessageType.FILE)
                    .content("파일 설명")
                    .fileUrl("")
                    .build();

            // when & then
            assertThatThrownBy(() -> message.validateContent())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("파일 URL");
        }

        @Test
        @DisplayName("이미지 메시지는 유효한 파일 URL로 검증에 성공한다")
        void should_NotThrowException_when_ImageMessageWithFileUrl() {
            // given
            Message message = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .type(Message.MessageType.IMAGE)
                    .fileUrl("https://example.com/image.png")
                    .build();

            // when & then (no exception)
            message.validateContent();
        }
    }

    @Nested
    @DisplayName("메시지 타입 확인 시")
    class MessageTypeCheck {

        @Test
        @DisplayName("이미지 메시지인지 확인할 수 있다")
        void should_ReturnTrue_when_MessageIsImage() {
            // given
            Message imageMessage = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .type(Message.MessageType.IMAGE)
                    .fileUrl("https://example.com/image.png")
                    .build();

            Message textMessage = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .type(Message.MessageType.TEXT)
                    .content("텍스트")
                    .build();

            // when & then
            assertThat(imageMessage.isImage()).isTrue();
            assertThat(textMessage.isImage()).isFalse();
        }

        @Test
        @DisplayName("파일 메시지인지 확인할 수 있다")
        void should_ReturnTrue_when_MessageIsFile() {
            // given
            Message fileMessage = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .type(Message.MessageType.FILE)
                    .fileUrl("https://example.com/file.pdf")
                    .build();

            Message textMessage = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .type(Message.MessageType.TEXT)
                    .content("텍스트")
                    .build();

            // when & then
            assertThat(fileMessage.isFile()).isTrue();
            assertThat(textMessage.isFile()).isFalse();
        }

        @Test
        @DisplayName("텍스트 메시지인지 확인할 수 있다")
        void should_ReturnTrue_when_MessageIsText() {
            // given
            Message textMessage = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .type(Message.MessageType.TEXT)
                    .content("텍스트")
                    .build();

            Message imageMessage = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .type(Message.MessageType.IMAGE)
                    .fileUrl("https://example.com/image.png")
                    .build();

            // when & then
            assertThat(textMessage.isText()).isTrue();
            assertThat(imageMessage.isText()).isFalse();
        }
    }

    @Nested
    @DisplayName("메시지 내용 수정 시")
    class UpdateContent {

        @Test
        @DisplayName("텍스트 메시지 내용을 수정할 수 있다")
        void should_UpdateContent_when_ValidTextMessage() {
            // given
            Message message = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .type(Message.MessageType.TEXT)
                    .content("기존 내용")
                    .deleted(false)
                    .build();
            String newContent = "새 내용";

            // when
            message.updateContent(newContent);

            // then
            assertThat(message.getContent()).isEqualTo(newContent);
        }

        @Test
        @DisplayName("삭제된 메시지는 수정할 수 없다")
        void should_ThrowException_when_UpdateDeletedMessage() {
            // given
            Message message = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .type(Message.MessageType.TEXT)
                    .content("기존 내용")
                    .deleted(true)
                    .build();

            // when & then
            assertThatThrownBy(() -> message.updateContent("새 내용"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("삭제된 메시지");
        }

        @Test
        @DisplayName("텍스트가 아닌 메시지는 수정할 수 없다")
        void should_ThrowException_when_UpdateNonTextMessage() {
            // given
            Message imageMessage = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .type(Message.MessageType.IMAGE)
                    .fileUrl("https://example.com/image.png")
                    .deleted(false)
                    .build();

            // when & then
            assertThatThrownBy(() -> imageMessage.updateContent("새 내용"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("텍스트 메시지");
        }

        @Test
        @DisplayName("빈 내용으로 수정할 수 없다")
        void should_ThrowException_when_UpdateWithEmptyContent() {
            // given
            Message message = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .type(Message.MessageType.TEXT)
                    .content("기존 내용")
                    .deleted(false)
                    .build();

            // when & then
            assertThatThrownBy(() -> message.updateContent(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("내용");
        }

        @Test
        @DisplayName("null 내용으로 수정할 수 없다")
        void should_ThrowException_when_UpdateWithNullContent() {
            // given
            Message message = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .type(Message.MessageType.TEXT)
                    .content("기존 내용")
                    .deleted(false)
                    .build();

            // when & then
            assertThatThrownBy(() -> message.updateContent(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("메시지 삭제 시")
    class DeleteMessage {

        @Test
        @DisplayName("메시지를 소프트 삭제할 수 있다")
        void should_DeleteMessage_when_DeleteCalled() {
            // given
            Message message = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .content("메시지 내용")
                    .deleted(false)
                    .deletedAt(null)
                    .build();

            // when
            message.delete();

            // then
            assertThat(message.isDeleted()).isTrue();
            assertThat(message.getDeletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("발신자 확인 시")
    class CheckSender {

        @Test
        @DisplayName("지정된 사용자가 보낸 메시지인지 확인할 수 있다")
        void should_ReturnTrue_when_MessageSentByUser() {
            // given
            Long senderId = 2L;
            Message message = Message.builder()
                    .chatRoomId(1L)
                    .senderId(senderId)
                    .content("메시지 내용")
                    .build();

            // when & then
            assertThat(message.isSentBy(senderId)).isTrue();
            assertThat(message.isSentBy(999L)).isFalse();
        }
    }

    @Nested
    @DisplayName("삭제 상태 확인 시")
    class CheckDeletedStatus {

        @Test
        @DisplayName("삭제된 메시지인지 확인할 수 있다")
        void should_ReturnTrue_when_MessageIsDeleted() {
            // given
            Message deletedMessage = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .content("메시지 내용")
                    .deleted(true)
                    .deletedAt(LocalDateTime.now())
                    .build();

            Message activeMessage = Message.builder()
                    .chatRoomId(1L)
                    .senderId(2L)
                    .content("메시지 내용")
                    .deleted(false)
                    .build();

            // when & then
            assertThat(deletedMessage.isDeleted()).isTrue();
            assertThat(activeMessage.isDeleted()).isFalse();
        }
    }
}
