package com.cotalk.application.service.message;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.port.inbound.message.MarkAsReadUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserEventBroker.ReadReceiptEvent;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MarkAsReadServiceTest {

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private UserEventBroker userEventBroker;

    private ChatRoomMemberValidator chatRoomMemberValidator;

    private MarkAsReadUseCase markAsReadUseCase;

    @BeforeEach
    void setUp() {
        chatRoomMemberValidator = new ChatRoomMemberValidator(chatRoomMemberRepository);
        markAsReadUseCase = new MarkAsReadService(chatRoomMemberRepository, chatRoomMemberValidator, userEventBroker);
    }

    @Test
    @DisplayName("읽음 표시 성공")
    void should_markAsRead_when_validRequest() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;
        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(chatRoomMemberRepository.updateLastReadAtIfNewer(eq(chatRoomId), eq(userId), any(LocalDateTime.class)))
                .willReturn(1);
        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(List.of(member));

        // when
        markAsReadUseCase.markAsRead(userId, chatRoomId);

        // then
        verify(chatRoomMemberRepository).updateLastReadAtIfNewer(eq(chatRoomId), eq(userId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("채팅방 멤버가 아니면 예외 발생")
    void should_throwException_when_notMember() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> markAsReadUseCase.markAsRead(userId, chatRoomId))
                .isInstanceOf(ChatRoomAccessDeniedException.class);
    }

    @Test
    @DisplayName("읽음 시간이 현재 시간으로 업데이트됨")
    void should_updateLastReadAtToCurrentTime_when_markAsRead() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;
        LocalDateTime beforeMark = LocalDateTime.now().minusSeconds(1);
        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(chatRoomMemberRepository.updateLastReadAtIfNewer(eq(chatRoomId), eq(userId), any(LocalDateTime.class)))
                .willReturn(1);
        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(List.of(member));

        // when
        markAsReadUseCase.markAsRead(userId, chatRoomId);

        // then
        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(chatRoomMemberRepository).updateLastReadAtIfNewer(eq(chatRoomId), eq(userId), timeCaptor.capture());
        assertThat(timeCaptor.getValue()).isAfter(beforeMark);
    }

    @Test
    @DisplayName("읽음 처리 시 모든 멤버에게 읽음 이벤트 브로드캐스트")
    void should_broadcastReadReceipt_when_markAsRead() {
        // given
        Long readerId = 1L;
        Long otherUserId = 2L;
        Long chatRoomId = 100L;

        ChatRoomMember readerMember = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(readerId)
                .build();
        ChatRoomMember otherMember = ChatRoomMember.builder()
                .id(501L)
                .chatRoomId(chatRoomId)
                .userId(otherUserId)
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, readerId))
                .willReturn(Optional.of(readerMember));
        given(chatRoomMemberRepository.updateLastReadAtIfNewer(eq(chatRoomId), eq(readerId), any(LocalDateTime.class)))
                .willReturn(1);
        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(List.of(readerMember, otherMember));

        // when
        markAsReadUseCase.markAsRead(readerId, chatRoomId);

        // then - 모든 멤버에게 이벤트 전송 확인
        ArgumentCaptor<ReadReceiptEvent> eventCaptor = ArgumentCaptor.forClass(ReadReceiptEvent.class);
        verify(userEventBroker).publishReadReceipt(eq(readerId), eventCaptor.capture());
        verify(userEventBroker).publishReadReceipt(eq(otherUserId), eventCaptor.capture());

        ReadReceiptEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(capturedEvent.userId()).isEqualTo(readerId);
        assertThat(capturedEvent.lastReadAt()).isNotNull();
    }

    @Test
    @DisplayName("읽은 본인에게도 읽음 이벤트를 전송함")
    void should_broadcastToSelf_when_markAsRead() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(chatRoomMemberRepository.updateLastReadAtIfNewer(eq(chatRoomId), eq(userId), any(LocalDateTime.class)))
                .willReturn(1);
        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(List.of(member));

        // when
        markAsReadUseCase.markAsRead(userId, chatRoomId);

        // then
        ArgumentCaptor<ReadReceiptEvent> eventCaptor = ArgumentCaptor.forClass(ReadReceiptEvent.class);
        verify(userEventBroker).publishReadReceipt(eq(userId), eventCaptor.capture());

        ReadReceiptEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(capturedEvent.userId()).isEqualTo(userId);
        assertThat(capturedEvent.lastReadAt()).isNotNull();
    }

    @Test
    @DisplayName("업데이트가 발생하지 않으면 읽음 이벤트를 브로드캐스트하지 않음")
    void should_notBroadcast_when_noUpdateOccurs() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;

        ChatRoomMember member = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(member));
        given(chatRoomMemberRepository.updateLastReadAtIfNewer(eq(chatRoomId), eq(userId), any(LocalDateTime.class)))
                .willReturn(0); // 업데이트 발생하지 않음

        // when
        markAsReadUseCase.markAsRead(userId, chatRoomId);

        // then
        verify(userEventBroker, never()).publishReadReceipt(any(), any());
        verify(chatRoomMemberRepository, never()).findByChatRoomId(any());
    }
}
