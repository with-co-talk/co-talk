package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.exception.ResourceAccessDeniedException;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.FileStorage;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.junit.jupiter.api.AfterEach;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DeleteMessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatMessageBroker chatMessageBroker;

    @Mock
    private TimeProvider timeProvider;

    @Mock
    private FileStorage fileStorage;

    private DeleteMessageService service;

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void setUp() {
        service = new DeleteMessageService(messageRepository, chatMessageBroker, timeProvider, fileStorage);
    }

    @AfterEach
    void tearDown() {
        // 트랜잭션 동기화를 직접 켠 테스트가 누수되지 않도록 정리한다.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
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
                .deleted(false)
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(messageRepository.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when
        service.deleteMessage(messageId, userId);

        // then
        assertThat(message.isDeleted()).isTrue();
        assertThat(message.getDeletedAt()).isEqualTo(FIXED_NOW);
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
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> service.deleteMessage(messageId, userId))
                .isInstanceOf(ResourceAccessDeniedException.class)
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
                .deleted(true)
                .deletedAt(LocalDateTime.now())
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> service.deleteMessage(messageId, userId))
                .isInstanceOf(ResourceAccessDeniedException.class)
                .hasMessageContaining("이미 삭제된");
    }

    @Test
    @DisplayName("작성 후 5분이 지나도 본인 메시지는 삭제할 수 있다")
    void should_deleteMessage_when_olderThan5Minutes() {
        // given - 본인 삭제(소프트 삭제)는 시간 제한 없이 허용한다.
        Long messageId = 100L;
        Long userId = 1L;

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(userId)
                .content("삭제할 메시지")
                .type(Message.MessageType.TEXT)
                .deleted(false)
                .build();

        // BaseEntity의 createdAt은 빌더에 없으므로 ReflectionTestUtils 사용
        ReflectionTestUtils.setField(message, "createdAt", FIXED_NOW.minusMinutes(6));

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(messageRepository.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when
        service.deleteMessage(messageId, userId);

        // then
        assertThat(message.isDeleted()).isTrue();
        assertThat(message.getDeletedAt()).isEqualTo(FIXED_NOW);
        verify(messageRepository).save(message);
    }

    @Test
    @DisplayName("파일 메시지 삭제 시 스토리지 원본을 정리한다")
    void should_cleanupStorage_when_deletingFileMessage() {
        // given
        Long messageId = 100L;
        Long userId = 1L;

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(userId)
                .content("file.pdf")
                .type(Message.MessageType.FILE)
                .fileUrl("http://localhost:9000/cotalk/uploads/1/abcd-1234.pdf")
                .fileName("file.pdf")
                .deleted(false)
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(messageRepository.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when
        service.deleteMessage(messageId, userId);

        // then - URL이 가리키는 스토리지 객체 키(uploads/...)를 삭제해야 한다.
        verify(fileStorage).delete("uploads/1/abcd-1234.pdf");
    }

    @Test
    @DisplayName("파일 메시지 삭제 시 썸네일이 있으면 함께 정리한다")
    void should_cleanupThumbnail_when_deletingImageMessageWithThumbnail() {
        // given
        Long messageId = 100L;
        Long userId = 1L;

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(userId)
                .content("image.png")
                .type(Message.MessageType.IMAGE)
                .fileUrl("http://localhost:9000/cotalk/uploads/1/image-key.png")
                .thumbnailUrl("http://localhost:9000/cotalk/uploads/1/thumb-key.png")
                .deleted(false)
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(messageRepository.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when
        service.deleteMessage(messageId, userId);

        // then
        verify(fileStorage).delete("uploads/1/image-key.png");
        verify(fileStorage).delete("uploads/1/thumb-key.png");
    }

    @Test
    @DisplayName("텍스트 메시지 삭제 시에는 스토리지를 호출하지 않는다")
    void should_notTouchStorage_when_deletingTextMessage() {
        // given
        Long messageId = 100L;
        Long userId = 1L;

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(userId)
                .content("텍스트 메시지")
                .type(Message.MessageType.TEXT)
                .deleted(false)
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(messageRepository.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when
        service.deleteMessage(messageId, userId);

        // then
        verify(fileStorage, never()).delete(any());
    }

    @Test
    @DisplayName("스토리지 정리에 실패해도 메시지 삭제 자체는 성공한다")
    void should_succeedDelete_when_storageCleanupFails() {
        // given
        Long messageId = 100L;
        Long userId = 1L;

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(userId)
                .content("file.pdf")
                .type(Message.MessageType.FILE)
                .fileUrl("http://localhost:9000/cotalk/uploads/1/key.pdf")
                .deleted(false)
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(messageRepository.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));
        given(timeProvider.now()).willReturn(FIXED_NOW);
        org.mockito.BDDMockito.willThrow(new RuntimeException("storage down"))
                .given(fileStorage).delete(any());

        // when & then - 예외가 전파되지 않고 삭제가 완료된다.
        service.deleteMessage(messageId, userId);

        assertThat(message.isDeleted()).isTrue();
        verify(messageRepository).save(message);
    }

    // === 트랜잭션 경계 검증 ===
    // 프로덕션은 @Transactional이라 항상 트랜잭션 동기화가 활성(isSynchronizationActive()==true)이고,
    // 스토리지 정리는 afterCommit 콜백으로 미뤄진다. 아래 테스트는 동기화를 직접 켜서
    // "정리가 커밋 시점까지 지연되는지"와 "afterCommit 실행 시 실제로 정리되는지(롤백 시 미정리)"를
    // 실제 프로덕션 경로로 검증한다.

    @Test
    @DisplayName("트랜잭션 활성 시 스토리지 정리는 커밋 전에는 실행되지 않고 afterCommit에서 실행된다")
    void should_deferStorageCleanup_until_afterCommit_when_transactionActive() {
        // given - 프로덕션과 동일하게 트랜잭션 동기화를 활성화한다.
        TransactionSynchronizationManager.initSynchronization();

        Long messageId = 100L;
        Long userId = 1L;

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(userId)
                .content("file.pdf")
                .type(Message.MessageType.FILE)
                .fileUrl("http://localhost:9000/cotalk/uploads/1/deferred-key.pdf")
                .fileName("file.pdf")
                .deleted(false)
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(messageRepository.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when - 메서드 실행만으로는 (= 커밋 전) 스토리지 정리가 일어나면 안 된다.
        service.deleteMessage(messageId, userId);

        // then - 콜백만 등록되고 아직 삭제는 호출되지 않았다(즉시 else 브랜치를 타지 않았음을 보장).
        verifyNoInteractions(fileStorage);
        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        assertThat(syncs).hasSize(1);

        // when - 실제 커밋 시점을 모사해 afterCommit을 트리거하면 정리가 수행된다(프로덕션 경로).
        syncs.forEach(TransactionSynchronization::afterCommit);

        // then
        verify(fileStorage).delete("uploads/1/deferred-key.pdf");
    }

    @Test
    @DisplayName("트랜잭션이 롤백되어 afterCommit이 호출되지 않으면 스토리지는 정리되지 않는다")
    void should_notCleanupStorage_when_transactionRolledBack() {
        // given - 트랜잭션 동기화를 활성화한다(롤백 시 afterCommit 미호출 상황을 모사).
        TransactionSynchronizationManager.initSynchronization();

        Long messageId = 100L;
        Long userId = 1L;

        Message message = Message.builder()
                .id(messageId)
                .chatRoomId(10L)
                .senderId(userId)
                .content("file.pdf")
                .type(Message.MessageType.FILE)
                .fileUrl("http://localhost:9000/cotalk/uploads/1/rollback-key.pdf")
                .fileName("file.pdf")
                .deleted(false)
                .build();

        given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(messageRepository.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));
        given(timeProvider.now()).willReturn(FIXED_NOW);

        // when - 삭제는 수행하되 afterCommit을 트리거하지 않는다(= 롤백되어 커밋이 일어나지 않은 경우).
        service.deleteMessage(messageId, userId);

        // then - 커밋이 없으므로 스토리지 정리는 절대 일어나지 않아야 한다(고아 미생성 보장).
        verifyNoInteractions(fileStorage);
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
    }
}
