package com.cotalk.adapter.inbound.rest.dto.message;

import com.cotalk.domain.entity.Message;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Message DTO 단위 테스트.
 *
 * @author seunggu.lee
 */
@DisplayName("Message DTO")
class MessageDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("SendMessageRequest")
    class SendMessageRequestTest {

        @Test
        @DisplayName("유효한 요청을 생성할 수 있다")
        void should_createRequest_when_validArguments() {
            // when
            SendMessageRequest request = new SendMessageRequest(1L, 100L, "Hello");

            // then
            assertThat(request.senderId()).isEqualTo(1L);
            assertThat(request.chatRoomId()).isEqualTo(100L);
            assertThat(request.content()).isEqualTo("Hello");
        }

        @Test
        @DisplayName("팩토리 메서드로 요청을 생성할 수 있다")
        void should_createRequest_when_usingFactoryMethod() {
            // when
            SendMessageRequest request = SendMessageRequest.of(1L, 100L, "Hello");

            // then
            assertThat(request.senderId()).isEqualTo(1L);
            assertThat(request.chatRoomId()).isEqualTo(100L);
            assertThat(request.content()).isEqualTo("Hello");
        }

        @Test
        @DisplayName("senderId가 null이면 유효성 검사 실패")
        void should_failValidation_when_senderIdIsNull() {
            // given
            SendMessageRequest request = new SendMessageRequest(null, 100L, "Hello");

            // when
            Set<ConstraintViolation<SendMessageRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("발신자 ID는 필수");
        }

        @Test
        @DisplayName("chatRoomId가 null이면 유효성 검사 실패")
        void should_failValidation_when_chatRoomIdIsNull() {
            // given
            SendMessageRequest request = new SendMessageRequest(1L, null, "Hello");

            // when
            Set<ConstraintViolation<SendMessageRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("채팅방 ID는 필수");
        }

        @Test
        @DisplayName("content가 빈 문자열이면 유효성 검사 실패")
        void should_failValidation_when_contentIsBlank() {
            // given
            SendMessageRequest request = new SendMessageRequest(1L, 100L, "   ");

            // when
            Set<ConstraintViolation<SendMessageRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("메시지 내용은 필수");
        }
    }

    @Nested
    @DisplayName("UpdateMessageRequest")
    class UpdateMessageRequestTest {

        @Test
        @DisplayName("유효한 요청을 생성할 수 있다")
        void should_createRequest_when_validArguments() {
            // when
            UpdateMessageRequest request = new UpdateMessageRequest(1L, "Updated content");

            // then
            assertThat(request.userId()).isEqualTo(1L);
            assertThat(request.content()).isEqualTo("Updated content");
        }

        @Test
        @DisplayName("팩토리 메서드로 요청을 생성할 수 있다")
        void should_createRequest_when_usingFactoryMethod() {
            // when
            UpdateMessageRequest request = UpdateMessageRequest.of(1L, "Updated");

            // then
            assertThat(request.userId()).isEqualTo(1L);
            assertThat(request.content()).isEqualTo("Updated");
        }

        @Test
        @DisplayName("userId가 null이면 유효성 검사 실패")
        void should_failValidation_when_userIdIsNull() {
            // given
            UpdateMessageRequest request = new UpdateMessageRequest(null, "content");

            // when
            Set<ConstraintViolation<UpdateMessageRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("사용자 ID는 필수");
        }
    }

    @Nested
    @DisplayName("ReplyMessageRequest")
    class ReplyMessageRequestTest {

        @Test
        @DisplayName("유효한 요청을 생성할 수 있다")
        void should_createRequest_when_validArguments() {
            // when
            ReplyMessageRequest request = new ReplyMessageRequest(1L, "Reply content");

            // then
            assertThat(request.senderId()).isEqualTo(1L);
            assertThat(request.content()).isEqualTo("Reply content");
        }

        @Test
        @DisplayName("팩토리 메서드로 요청을 생성할 수 있다")
        void should_createRequest_when_usingFactoryMethod() {
            // when
            ReplyMessageRequest request = ReplyMessageRequest.of(1L, "Reply");

            // then
            assertThat(request.senderId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("senderId가 null이면 유효성 검사 실패")
        void should_failValidation_when_senderIdIsNull() {
            // given
            ReplyMessageRequest request = new ReplyMessageRequest(null, "Reply");

            // when
            Set<ConstraintViolation<ReplyMessageRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).hasSize(1);
        }
    }

    @Nested
    @DisplayName("ForwardMessageRequest")
    class ForwardMessageRequestTest {

        @Test
        @DisplayName("유효한 요청을 생성할 수 있다")
        void should_createRequest_when_validArguments() {
            // when
            ForwardMessageRequest request = new ForwardMessageRequest(1L, 200L);

            // then
            assertThat(request.senderId()).isEqualTo(1L);
            assertThat(request.targetChatRoomId()).isEqualTo(200L);
        }

        @Test
        @DisplayName("팩토리 메서드로 요청을 생성할 수 있다")
        void should_createRequest_when_usingFactoryMethod() {
            // when
            ForwardMessageRequest request = ForwardMessageRequest.of(1L, 200L);

            // then
            assertThat(request.senderId()).isEqualTo(1L);
            assertThat(request.targetChatRoomId()).isEqualTo(200L);
        }

        @Test
        @DisplayName("targetChatRoomId가 null이면 유효성 검사 실패")
        void should_failValidation_when_targetChatRoomIdIsNull() {
            // given
            ForwardMessageRequest request = new ForwardMessageRequest(1L, null);

            // when
            Set<ConstraintViolation<ForwardMessageRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("대상 채팅방 ID는 필수");
        }
    }

    @Nested
    @DisplayName("AddReactionRequest")
    class AddReactionRequestTest {

        @Test
        @DisplayName("유효한 요청을 생성할 수 있다")
        void should_createRequest_when_validArguments() {
            // when
            AddReactionRequest request = new AddReactionRequest("👍");

            // then
            assertThat(request.emoji()).isEqualTo("👍");
        }

        @Test
        @DisplayName("팩토리 메서드로 요청을 생성할 수 있다")
        void should_createRequest_when_usingFactoryMethod() {
            // when
            AddReactionRequest request = AddReactionRequest.of("❤️");

            // then
            assertThat(request.emoji()).isEqualTo("❤️");
        }

        @Test
        @DisplayName("emoji가 빈 문자열이면 유효성 검사 실패")
        void should_failValidation_when_emojiIsBlank() {
            // given
            AddReactionRequest request = new AddReactionRequest("");

            // when
            Set<ConstraintViolation<AddReactionRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("이모지는 필수");
        }
    }

    @Nested
    @DisplayName("MessageDto")
    class MessageDtoNestedTest {

        @Test
        @DisplayName("Message 엔티티로부터 DTO를 생성할 수 있다")
        void should_createDto_when_fromMessage() {
            // given
            Message message = Message.builder()
                    .id(1L)
                    .senderId(10L)
                    .chatRoomId(100L)
                    .content("Test message")
                    .type(Message.MessageType.TEXT)
                    .build();

            // when
            MessageDto dto = MessageDto.from(message);

            // then
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.senderId()).isEqualTo(10L);
            assertThat(dto.content()).isEqualTo("Test message");
            assertThat(dto.type()).isEqualTo("TEXT");
        }

        @Test
        @DisplayName("파일 메시지에서 DTO를 생성할 수 있다")
        void should_createDto_when_fromFileMessage() {
            // given
            Message message = Message.builder()
                    .id(2L)
                    .senderId(10L)
                    .chatRoomId(100L)
                    .content("File message")
                    .type(Message.MessageType.FILE)
                    .fileUrl("https://example.com/file.pdf")
                    .fileName("document.pdf")
                    .fileSize(1024L)
                    .fileContentType("application/pdf")
                    .build();

            // when
            MessageDto dto = MessageDto.from(message);

            // then
            assertThat(dto.type()).isEqualTo("FILE");
            assertThat(dto.fileUrl()).isEqualTo("https://example.com/file.pdf");
            assertThat(dto.fileName()).isEqualTo("document.pdf");
            assertThat(dto.fileSize()).isEqualTo(1024L);
            assertThat(dto.contentType()).isEqualTo("application/pdf");
        }

        @Test
        @DisplayName("이미지 메시지에서 DTO를 생성할 수 있다")
        void should_createDto_when_fromImageMessage() {
            // given
            Message message = Message.builder()
                    .id(3L)
                    .senderId(10L)
                    .chatRoomId(100L)
                    .content("Image message")
                    .type(Message.MessageType.IMAGE)
                    .fileUrl("https://example.com/image.jpg")
                    .thumbnailUrl("https://example.com/image_thumb.jpg")
                    .build();

            // when
            MessageDto dto = MessageDto.from(message);

            // then
            assertThat(dto.type()).isEqualTo("IMAGE");
            assertThat(dto.thumbnailUrl()).isEqualTo("https://example.com/image_thumb.jpg");
        }

        @Test
        @DisplayName("답장 메시지에서 DTO를 생성할 수 있다")
        void should_createDto_when_fromReplyMessage() {
            // given
            Message message = Message.builder()
                    .id(4L)
                    .senderId(10L)
                    .chatRoomId(100L)
                    .content("Reply message")
                    .type(Message.MessageType.TEXT)
                    .replyToMessageId(1L)
                    .build();

            // when
            MessageDto dto = MessageDto.from(message);

            // then
            assertThat(dto.replyToMessageId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("전달 메시지에서 DTO를 생성할 수 있다")
        void should_createDto_when_fromForwardedMessage() {
            // given
            Message message = Message.builder()
                    .id(5L)
                    .senderId(10L)
                    .chatRoomId(100L)
                    .content("Forwarded message")
                    .type(Message.MessageType.TEXT)
                    .forwardedFromMessageId(2L)
                    .build();

            // when
            MessageDto dto = MessageDto.from(message);

            // then
            assertThat(dto.forwardedFromMessageId()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("SendMessageResponse")
    class SendMessageResponseTest {

        @Test
        @DisplayName("Message 엔티티로부터 응답 DTO를 생성할 수 있다")
        void should_createResponse_when_fromMessage() {
            // given
            Message message = Message.builder()
                    .id(1L)
                    .senderId(10L)
                    .chatRoomId(100L)
                    .content("Test message")
                    .type(Message.MessageType.TEXT)
                    .build();

            // when
            SendMessageResponse response = SendMessageResponse.from(message);

            // then
            assertThat(response.messageId()).isEqualTo(1L);
            assertThat(response.content()).isEqualTo("Test message");
            assertThat(response.type()).isEqualTo("TEXT");
        }
    }

    @Nested
    @DisplayName("MessageHistoryResponse")
    class MessageHistoryResponseTest {

        @Test
        @DisplayName("팩토리 메서드로 응답을 생성할 수 있다")
        void should_createResponse_when_usingFactoryMethod() {
            // given
            List<MessageDto> messages = List.of(
                    new MessageDto(1L, 10L, "테스트유저", null, "Message 1", "TEXT", LocalDateTime.now(), null, null, null, null, null, null, null, 0),
                    new MessageDto(2L, 10L, "테스트유저", null, "Message 2", "TEXT", LocalDateTime.now(), null, null, null, null, null, null, null, 0)
            );

            // when
            MessageHistoryResponse response = MessageHistoryResponse.of(messages, 2L, true);

            // then
            assertThat(response.messages()).hasSize(2);
            assertThat(response.nextCursor()).isEqualTo(2L);
            assertThat(response.hasMore()).isTrue();
        }

        @Test
        @DisplayName("빈 메시지 목록으로 응답을 생성할 수 있다")
        void should_createResponse_when_emptyMessages() {
            // when
            MessageHistoryResponse response = MessageHistoryResponse.of(List.of(), null, false);

            // then
            assertThat(response.messages()).isEmpty();
            assertThat(response.nextCursor()).isNull();
            assertThat(response.hasMore()).isFalse();
        }
    }
}
