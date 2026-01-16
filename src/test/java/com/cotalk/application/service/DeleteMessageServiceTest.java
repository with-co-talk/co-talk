package com.cotalk.application.service;

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
class DeleteMessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    private DeleteMessageService service;

    @BeforeEach
    void setUp() {
        service = new DeleteMessageService(messageRepository);
    }

    @Test
    @DisplayName("본인이 보낸 메시지 삭제 성공")
    void should_deleteMessage_when_sender() {
        // given
        Long messageId = 100L;
        Long userId = 1L;

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(userId)
                .content("삭제할 메시지")
                .type(Message.MessageType.TEXT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleted(false)
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(messageRepository.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        service.deleteMessage(messageId, userId);

        // then
        assertThat(message.isDeleted()).isTrue();
        assertThat(message.getDeletedAt()).isNotNull();
        verify(messageRepository).save(message);
    }

    @Test
    @DisplayName("다른 사람이 보낸 메시지 삭제 시 예외")
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
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> service.deleteMessage(messageId, userId))
                .isInstanceOf(MessageAccessDeniedException.class)
                .hasMessageContaining("본인이 보낸 메시지만");
    }

    @Test
    @DisplayName("존재하지 않는 메시지 삭제 시 예외")
    void should_throwException_when_messageNotFound() {
        // given
        Long messageId = 999L;
        given(messageRepository.findById(messageId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.deleteMessage(messageId, 1L))
                .isInstanceOf(MessageNotFoundException.class);
    }

    @Test
    @DisplayName("이미 삭제된 메시지 삭제 시 예외")
    void should_throwException_when_alreadyDeleted() {
        // given
        Long messageId = 100L;
        Long userId = 1L;

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(userId)
                .content("원본 메시지")
                .type(Message.MessageType.TEXT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleted(true)
                .deletedAt(LocalDateTime.now())
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> service.deleteMessage(messageId, userId))
                .isInstanceOf(MessageAccessDeniedException.class)
                .hasMessageContaining("이미 삭제된");
    }
}
