package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.exception.ResourceAccessDeniedException;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

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

    @Mock
    private ChatMessageBroker chatMessageBroker;

    @Mock
    private TimeProvider timeProvider;

    private UpdateMessageService service;

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void setUp() {
        service = new UpdateMessageService(messageRepository, chatMessageBroker, timeProvider);
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
        given(timeProvider.now()).willReturn(FIXED_NOW);

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
                .isInstanceOf(ResourceAccessDeniedException.class)
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
                .isInstanceOf(ResourceAccessDeniedException.class)
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

    @Test
    @DisplayName("파일 메시지 수정 시 예외")
    void should_throwException_when_fileMessage() {
        // given
        Long messageId = 100L;
        Long userId = 1L;

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(userId)
                .content("파일")
                .type(Message.MessageType.FILE)
                .fileUrl("http://example.com/file.pdf")
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> service.updateMessage(messageId, userId, "새 내용"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("텍스트 메시지만");
    }

    @Test
    @DisplayName("빈 메시지 내용으로 수정 시 예외")
    void should_throwException_when_emptyContent() {
        // given
        Long messageId = 100L;
        Long userId = 1L;

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(userId)
                .content("원본 메시지")
                .type(Message.MessageType.TEXT)
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> service.updateMessage(messageId, userId, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("내용");
    }

    @Test
    @DisplayName("null 메시지 내용으로 수정 시 예외")
    void should_throwException_when_nullContent() {
        // given
        Long messageId = 100L;
        Long userId = 1L;

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(userId)
                .content("원본 메시지")
                .type(Message.MessageType.TEXT)
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> service.updateMessage(messageId, userId, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("5분 초과된 메시지 수정 시 예외")
    void should_throwException_when_messageOlderThan5Minutes() {
        // given
        Long messageId = 100L;
        Long userId = 1L;

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(userId)
                .content("원본 메시지")
                .type(Message.MessageType.TEXT)
                .build();

        // BaseEntity의 createdAt은 빌더에 없으므로 ReflectionTestUtils 사용
        ReflectionTestUtils.setField(message, "createdAt", FIXED_NOW.minusMinutes(6));

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when & then
        assertThatThrownBy(() -> service.updateMessage(messageId, userId, "새 내용"))
                .isInstanceOf(ResourceAccessDeniedException.class)
                .hasMessageContaining("5분");
    }
}
