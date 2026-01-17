package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.exception.MessageAccessDeniedException;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.port.outbound.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UpdateMessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    private UpdateMessageService service;

    @BeforeEach
    void setUp() {
        service = new UpdateMessageService(messageRepository);
    }

    @Test
    @DisplayName("본인이 보낸 메시지 수정 성공")
    void should_updateMessage_when_sender() {
        // given
        Long messageId = 100L;
        Long userId = 1L;
        String newContent = "수정된 메시지";

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(userId)
                .content("원본 메시지")
                .type(Message.MessageType.TEXT)
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(messageRepository.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        Message updated = service.updateMessage(messageId, userId, newContent);

        // then
        assertThat(updated.getContent()).isEqualTo(newContent);
        verify(messageRepository).save(message);
    }

    @Test
    @DisplayName("다른 사람이 보낸 메시지 수정 시 예외")
    void should_throwException_when_notSender() {
        // given
        Long messageId = 100L;
        Long userId = 1L;
        Long otherUserId = 2L;

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(otherUserId) // 다른 사용자
                .content("원본 메시지")
                .type(Message.MessageType.TEXT)
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> service.updateMessage(messageId, userId, "새 내용"))
                .isInstanceOf(MessageAccessDeniedException.class)
                .hasMessageContaining("본인이 보낸 메시지만");
    }

    @Test
    @DisplayName("존재하지 않는 메시지 수정 시 예외")
    void should_throwException_when_messageNotFound() {
        // given
        Long messageId = 999L;
        given(messageRepository.findById(messageId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.updateMessage(messageId, 1L, "새 내용"))
                .isInstanceOf(MessageNotFoundException.class);
    }

    @Test
    @DisplayName("이미 삭제된 메시지 수정 시 예외")
    void should_throwException_when_messageDeleted() {
        // given
        Long messageId = 100L;
        Long userId = 1L;

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(userId)
                .content("원본 메시지")
                .type(Message.MessageType.TEXT)
                .deleted(true)
                .deletedAt(LocalDateTime.now())
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> service.updateMessage(messageId, userId, "새 내용"))
                .isInstanceOf(MessageAccessDeniedException.class)
                .hasMessageContaining("이미 삭제된");
    }

    @Test
    @DisplayName("이미지/파일 메시지 수정 시 예외")
    void should_throwException_when_notTextMessage() {
        // given
        Long messageId = 100L;
        Long userId = 1L;

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(userId)
                .content("이미지")
                .type(Message.MessageType.IMAGE)
                .fileUrl("http://example.com/image.jpg")
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> service.updateMessage(messageId, userId, "새 내용"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("텍스트 메시지만");
    }
}
