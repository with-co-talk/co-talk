package com.cotalk.application.service.chat;

import com.cotalk.common.fixture.ChatRoomTestFixture;
import com.cotalk.common.fixture.MessageTestFixture;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserEventBroker.ChatListUpdateEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * {@link PublishChatListUpdateService} 유닛 테스트.
 * 채팅 목록 업데이트 발행 로직을 검증한다.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
class PublishChatListUpdateServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserEventBroker userEventBroker;

    @InjectMocks
    private PublishChatListUpdateService publishChatListUpdateService;

    @Test
    void should_publishUpdateToAllMembers_when_messageIsSent() {
        // given
        Message message = MessageTestFixture.createTextMessage(1L, 100L, 1L);
        List<ChatRoomMember> members = List.of(
                ChatRoomTestFixture.createChatRoomMember(1L, 100L, 1L),
                ChatRoomTestFixture.createChatRoomMember(2L, 100L, 2L),
                ChatRoomTestFixture.createChatRoomMember(3L, 100L, 3L)
        );
        String senderNickname = "발신자";

        Map<Long, Long> unreadCountMap = Map.of(
                1L, 0L,
                2L, 5L,
                3L, 3L
        );
        given(messageRepository.batchCountUnreadMessagesForAllMembers(100L))
                .willReturn(unreadCountMap);

        // when
        publishChatListUpdateService.publishChatListUpdate(message, members, senderNickname);

        // then
        verify(messageRepository).batchCountUnreadMessagesForAllMembers(100L);
        verify(userEventBroker, times(3)).publishChatListUpdate(anyLong(), any(ChatListUpdateEvent.class));
    }

    @Test
    void should_publishWithCorrectUnreadCount_when_memberHasUnreadMessages() {
        // given
        Message message = MessageTestFixture.createTextMessage(1L, 100L, 1L);
        List<ChatRoomMember> members = List.of(
                ChatRoomTestFixture.createChatRoomMember(2L, 100L, 2L)
        );
        String senderNickname = "발신자";

        Map<Long, Long> unreadCountMap = Map.of(2L, 5L);
        given(messageRepository.batchCountUnreadMessagesForAllMembers(100L))
                .willReturn(unreadCountMap);

        // when
        publishChatListUpdateService.publishChatListUpdate(message, members, senderNickname);

        // then
        ArgumentCaptor<ChatListUpdateEvent> captor = ArgumentCaptor.forClass(ChatListUpdateEvent.class);
        verify(userEventBroker).publishChatListUpdate(eq(2L), captor.capture());

        ChatListUpdateEvent event = captor.getValue();
        assertThat(event.unreadCount()).isEqualTo(5);
        assertThat(event.eventType()).isEqualTo("NEW_MESSAGE");
        assertThat(event.roomId()).isEqualTo(100L);
        assertThat(event.lastMessage()).isEqualTo(message.getContent());
        assertThat(event.senderId()).isEqualTo(1L);
        assertThat(event.senderNickname()).isEqualTo(senderNickname);
    }

    @Test
    void should_publishWithZeroUnreadCount_when_memberHasNoUnreadMessages() {
        // given
        Message message = MessageTestFixture.createTextMessage(1L, 100L, 1L);
        List<ChatRoomMember> members = List.of(
                ChatRoomTestFixture.createChatRoomMember(1L, 100L, 1L)
        );
        String senderNickname = "발신자";

        Map<Long, Long> unreadCountMap = Map.of(1L, 0L);
        given(messageRepository.batchCountUnreadMessagesForAllMembers(100L))
                .willReturn(unreadCountMap);

        // when
        publishChatListUpdateService.publishChatListUpdate(message, members, senderNickname);

        // then
        ArgumentCaptor<ChatListUpdateEvent> captor = ArgumentCaptor.forClass(ChatListUpdateEvent.class);
        verify(userEventBroker).publishChatListUpdate(eq(1L), captor.capture());

        ChatListUpdateEvent event = captor.getValue();
        assertThat(event.unreadCount()).isEqualTo(0);
    }

    @Test
    void should_useBatchQueryOnce_when_multipleMembers() {
        // given
        Message message = MessageTestFixture.createTextMessage(1L, 100L, 1L);
        List<ChatRoomMember> members = List.of(
                ChatRoomTestFixture.createChatRoomMember(1L, 100L, 1L),
                ChatRoomTestFixture.createChatRoomMember(2L, 100L, 2L),
                ChatRoomTestFixture.createChatRoomMember(3L, 100L, 3L)
        );
        String senderNickname = "발신자";

        Map<Long, Long> unreadCountMap = Map.of(1L, 0L, 2L, 2L, 3L, 3L);
        given(messageRepository.batchCountUnreadMessagesForAllMembers(100L))
                .willReturn(unreadCountMap);

        // when
        publishChatListUpdateService.publishChatListUpdate(message, members, senderNickname);

        // then
        verify(messageRepository, times(1)).batchCountUnreadMessagesForAllMembers(100L);
    }
}
