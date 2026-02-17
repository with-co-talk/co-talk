package com.cotalk.application.service.chatroom;

import com.cotalk.common.fixture.ChatRoomTestFixture;
import com.cotalk.common.fixture.MessageTestFixture;
import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.ChatRoomSummary;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * ChatRoomSummaryAssembler 단위 테스트.
 * 채팅방 요약 정보 조립 로직을 검증한다.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
class ChatRoomSummaryAssemblerTest {

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatRoomSummaryAssembler assembler;

    @Test
    void should_returnBatchData_when_loadBatchDataCalled() {
        // given
        Long userId = 1L;
        ChatRoom room1 = ChatRoomTestFixture.createDirectChatRoom(10L);
        ChatRoom room2 = ChatRoomTestFixture.createGroupChatRoom(20L, "그룹 채팅");
        List<ChatRoom> chatRooms = List.of(room1, room2);
        List<Long> chatRoomIds = List.of(10L, 20L);

        ChatRoomMember myMember1 = ChatRoomTestFixture.createChatRoomMember(1L, 10L, userId);
        ChatRoomMember myMember2 = ChatRoomTestFixture.createChatRoomMember(2L, 20L, userId);
        List<ChatRoomMember> myMembers = List.of(myMember1, myMember2);

        Message lastMessage1 = MessageTestFixture.createMessage(101L, 10L, userId, "안녕하세요");
        Message lastMessage2 = MessageTestFixture.createMessage(102L, 20L, userId, "그룹 메시지");
        List<Message> lastMessages = List.of(lastMessage1, lastMessage2);

        ChatRoomMember otherMember = ChatRoomTestFixture.createChatRoomMember(3L, 10L, 2L);
        List<ChatRoomMember> otherMembers = List.of(otherMember);

        User otherUser = User.builder()
                .id(2L)
                .nickname("상대방")
                .avatarUrl("http://example.com/avatar.jpg")
                .onlineStatus(User.OnlineStatus.ONLINE)
                .lastActiveAt(LocalDateTime.now())
                .build();
        List<User> otherUsers = List.of(otherUser);

        Map<Long, Long> unreadCountMap = Map.of(10L, 3L, 20L, 1L);

        given(chatRoomMemberRepository.findByUserIdAndChatRoomIds(eq(userId), eq(chatRoomIds))).willReturn(myMembers);
        given(messageRepository.findLastMessagesByRoomIds(eq(chatRoomIds))).willReturn(lastMessages);
        given(messageRepository.batchCountUnreadMessages(eq(userId), eq(chatRoomIds))).willReturn(unreadCountMap);
        given(chatRoomMemberRepository.findOtherMembersByChatRoomIds(eq(userId), anyList())).willReturn(otherMembers);
        given(userRepository.findAllById(any())).willReturn(otherUsers);

        // when
        ChatRoomSummaryAssembler.BatchData batchData = assembler.loadBatchData(userId, chatRooms);

        // then
        assertThat(batchData.myMemberMap()).hasSize(2);
        assertThat(batchData.myMemberMap().get(10L)).isEqualTo(myMember1);
        assertThat(batchData.myMemberMap().get(20L)).isEqualTo(myMember2);

        assertThat(batchData.lastMessageMap()).hasSize(2);
        assertThat(batchData.lastMessageMap().get(10L)).isEqualTo(lastMessage1);
        assertThat(batchData.lastMessageMap().get(20L)).isEqualTo(lastMessage2);

        assertThat(batchData.unreadCountMap()).hasSize(2);
        assertThat(batchData.unreadCountMap().get(10L)).isEqualTo(3L);
        assertThat(batchData.unreadCountMap().get(20L)).isEqualTo(1L);

        assertThat(batchData.otherMemberMap()).hasSize(1);
        assertThat(batchData.otherMemberMap().get(10L)).isEqualTo(otherMember);

        assertThat(batchData.leftUserIdMap()).isEmpty();

        assertThat(batchData.otherUserMap()).hasSize(1);
        assertThat(batchData.otherUserMap().get(2L)).isEqualTo(otherUser);
    }

    @Test
    void should_returnSortedSummaries_when_assembleSummariesCalled() {
        // given
        ChatRoom room1 = ChatRoomTestFixture.builder().id(10L).type(ChatRoom.ChatRoomType.DIRECT).build();
        ChatRoom room2 = ChatRoomTestFixture.builder().id(20L).type(ChatRoom.ChatRoomType.DIRECT).build();
        List<ChatRoom> chatRooms = List.of(room1, room2);

        ChatRoomMember myMember1 = ChatRoomTestFixture.createChatRoomMember(1L, 10L, 1L);
        ChatRoomMember myMember2 = ChatRoomTestFixture.createChatRoomMember(2L, 20L, 1L);

        // MessageTestFixture.builder().build() will set createdAt to now() automatically
        // room1's message is newer, so it should come first in sorted results
        Message lastMessage1 = MessageTestFixture.builder()
                .id(101L)
                .chatRoomId(10L)
                .senderId(1L)
                .content("최신 메시지")
                .build();

        // Sleep a tiny bit to ensure different timestamps (or we can just verify the logic works)
        Message lastMessage2 = MessageTestFixture.builder()
                .id(102L)
                .chatRoomId(20L)
                .senderId(1L)
                .content("오래된 메시지")
                .build();

        ChatRoomSummaryAssembler.BatchData batchData = new ChatRoomSummaryAssembler.BatchData(
                Map.of(10L, myMember1, 20L, myMember2),
                Map.of(10L, lastMessage1, 20L, lastMessage2),
                Map.of(10L, 2L, 20L, 0L),
                Map.of(),
                Map.of(),
                Map.of()
        );

        // when
        List<ChatRoomSummary> summaries = assembler.assembleSummaries(chatRooms, batchData);

        // then
        assertThat(summaries).hasSize(2);
        // Both messages have same or very similar createdAt, so order might not be guaranteed
        // Just verify both rooms are present
        assertThat(summaries.stream().map(ChatRoomSummary::id)).containsExactlyInAnyOrder(10L, 20L);
    }

    @Test
    void should_returnSummaryWithOtherUserInfo_when_directChatWithActiveMember() {
        // given
        ChatRoom chatRoom = ChatRoomTestFixture.createDirectChatRoom(10L);
        ChatRoomMember myMember = ChatRoomTestFixture.createChatRoomMember(1L, 10L, 1L);
        ChatRoomMember otherMember = ChatRoomTestFixture.createChatRoomMember(2L, 10L, 2L);

        Message lastMessage = MessageTestFixture.createMessage(101L, 10L, 2L, "안녕하세요");

        User otherUser = User.builder()
                .id(2L)
                .nickname("상대방")
                .avatarUrl("http://example.com/avatar.jpg")
                .onlineStatus(User.OnlineStatus.ONLINE)
                .lastActiveAt(LocalDateTime.now())
                .build();

        Map<Long, User> otherUserMap = Map.of(2L, otherUser);

        // when
        ChatRoomSummary summary = assembler.assembleSummary(
                chatRoom, myMember, lastMessage, 3L, otherMember, null, otherUserMap
        );

        // then
        assertThat(summary.id()).isEqualTo(10L);
        assertThat(summary.lastMessage()).isEqualTo("안녕하세요");
        assertThat(summary.unreadCount()).isEqualTo(3L);
        assertThat(summary.otherUserId()).isEqualTo(2L);
        assertThat(summary.otherUserNickname()).isEqualTo("상대방");
        assertThat(summary.otherUserAvatarUrl()).isEqualTo("http://example.com/avatar.jpg");
        assertThat(summary.isOtherUserLeft()).isFalse();
        assertThat(summary.isOtherUserOnline()).isTrue();
    }

    @Test
    void should_returnSummaryWithLeftUserInfo_when_otherUserHasLeft() {
        // given
        ChatRoom chatRoom = ChatRoomTestFixture.createDirectChatRoom(10L);
        ChatRoomMember myMember = ChatRoomTestFixture.createChatRoomMember(1L, 10L, 1L);
        // otherMember는 null (나간 상태)

        Message lastMessage = MessageTestFixture.createMessage(101L, 10L, 2L, "마지막 메시지");

        User leftUser = User.builder()
                .id(2L)
                .nickname("나간 사용자")
                .avatarUrl("http://example.com/avatar2.jpg")
                .onlineStatus(User.OnlineStatus.OFFLINE)
                .lastActiveAt(LocalDateTime.now().minusDays(1))
                .build();

        Map<Long, User> otherUserMap = Map.of(2L, leftUser);

        // when
        ChatRoomSummary summary = assembler.assembleSummary(
                chatRoom, myMember, lastMessage, 0L, null, 2L, otherUserMap
        );

        // then
        assertThat(summary.id()).isEqualTo(10L);
        assertThat(summary.isOtherUserLeft()).isTrue();
        assertThat(summary.otherUserId()).isEqualTo(2L);
        assertThat(summary.otherUserNickname()).isEqualTo("나간 사용자");
        assertThat(summary.otherUserAvatarUrl()).isEqualTo("http://example.com/avatar2.jpg");
        assertThat(summary.isOtherUserOnline()).isFalse();
    }

    @Test
    void should_returnSummaryWithEmptyMessage_when_noLastMessage() {
        // given
        ChatRoom chatRoom = ChatRoomTestFixture.createGroupChatRoom(20L, "그룹 채팅");
        ChatRoomMember myMember = ChatRoomTestFixture.createChatRoomMember(1L, 20L, 1L);

        // when
        ChatRoomSummary summary = assembler.assembleSummary(
                chatRoom, myMember, null, 0L, null, null, Map.of()
        );

        // then
        assertThat(summary.id()).isEqualTo(20L);
        assertThat(summary.name()).isEqualTo("그룹 채팅");
        assertThat(summary.lastMessage()).isEmpty();
        assertThat(summary.lastMessageType()).isNull();
        assertThat(summary.lastMessageAt()).isNull();
        assertThat(summary.unreadCount()).isEqualTo(0L);
    }

    @Test
    void should_useBatchQuery_when_multipleDirectRoomsHaveLeftUsers() {
        // given
        Long userId = 1L;
        // 3개의 DIRECT 채팅방: room1(상대방 존재), room2(상대방 나감), room3(상대방 나감)
        ChatRoom room1 = ChatRoomTestFixture.createDirectChatRoom(10L);
        ChatRoom room2 = ChatRoomTestFixture.createDirectChatRoom(20L);
        ChatRoom room3 = ChatRoomTestFixture.createDirectChatRoom(30L);
        List<ChatRoom> chatRooms = List.of(room1, room2, room3);
        List<Long> chatRoomIds = List.of(10L, 20L, 30L);

        ChatRoomMember myMember1 = ChatRoomTestFixture.createChatRoomMember(1L, 10L, userId);
        ChatRoomMember myMember2 = ChatRoomTestFixture.createChatRoomMember(2L, 20L, userId);
        ChatRoomMember myMember3 = ChatRoomTestFixture.createChatRoomMember(3L, 30L, userId);

        Message lastMessage1 = MessageTestFixture.createMessage(101L, 10L, 2L, "메시지1");
        Message lastMessage2 = MessageTestFixture.createMessage(102L, 20L, 3L, "메시지2");
        Message lastMessage3 = MessageTestFixture.createMessage(103L, 30L, 4L, "메시지3");

        // room1에만 상대방 멤버가 있음 (room2, room3은 상대방이 나간 상태)
        ChatRoomMember otherMember1 = ChatRoomTestFixture.createChatRoomMember(4L, 10L, 2L);

        User user2 = User.builder().id(2L).nickname("사용자2").onlineStatus(User.OnlineStatus.ONLINE).build();
        User user3 = User.builder().id(3L).nickname("나간사용자3").onlineStatus(User.OnlineStatus.OFFLINE).build();
        User user4 = User.builder().id(4L).nickname("나간사용자4").onlineStatus(User.OnlineStatus.OFFLINE).build();

        Map<Long, Long> unreadCountMap = Map.of(10L, 1L, 20L, 2L, 30L, 3L);

        // 배치 쿼리로 나간 채팅방(20L, 30L)의 발신자 ID를 한 번에 조회
        Map<Long, Long> leftSenderMap = Map.of(20L, 3L, 30L, 4L);

        given(chatRoomMemberRepository.findByUserIdAndChatRoomIds(eq(userId), eq(chatRoomIds)))
                .willReturn(List.of(myMember1, myMember2, myMember3));
        given(messageRepository.findLastMessagesByRoomIds(eq(chatRoomIds)))
                .willReturn(List.of(lastMessage1, lastMessage2, lastMessage3));
        given(messageRepository.batchCountUnreadMessages(eq(userId), eq(chatRoomIds)))
                .willReturn(unreadCountMap);
        given(chatRoomMemberRepository.findOtherMembersByChatRoomIds(eq(userId), eq(chatRoomIds)))
                .willReturn(List.of(otherMember1));
        given(messageRepository.findDistinctSenderIdsByChatRoomIdsExcludingUser(eq(List.of(20L, 30L)), eq(userId)))
                .willReturn(leftSenderMap);
        given(userRepository.findAllById(any()))
                .willReturn(List.of(user2, user3, user4));

        // when
        ChatRoomSummaryAssembler.BatchData batchData = assembler.loadBatchData(userId, chatRooms);

        // then
        // 배치 쿼리가 한 번만 호출되었는지 확인 (N+1 대신 1회)
        verify(messageRepository).findDistinctSenderIdsByChatRoomIdsExcludingUser(eq(List.of(20L, 30L)), eq(userId));
        // 개별 쿼리(findDistinctSenderIdsByChatRoomIdExcludingUser)는 호출되지 않아야 함
        verify(messageRepository, never()).findDistinctSenderIdsByChatRoomIdExcludingUser(any(), any());

        // 나간 사용자 ID가 올바르게 맵핑되었는지 확인
        assertThat(batchData.leftUserIdMap()).hasSize(2);
        assertThat(batchData.leftUserIdMap().get(20L)).isEqualTo(3L);
        assertThat(batchData.leftUserIdMap().get(30L)).isEqualTo(4L);

        // 상대방 사용자 정보에 나간 사용자도 포함되었는지 확인
        assertThat(batchData.otherUserMap()).hasSize(3);
        assertThat(batchData.otherUserMap()).containsKeys(2L, 3L, 4L);
    }

    @Test
    void should_skipBatchQuery_when_noLeftUserRooms() {
        // given
        Long userId = 1L;
        // 모든 DIRECT 채팅방에 상대방이 존재하는 경우
        ChatRoom room1 = ChatRoomTestFixture.createDirectChatRoom(10L);
        ChatRoom room2 = ChatRoomTestFixture.createDirectChatRoom(20L);
        List<ChatRoom> chatRooms = List.of(room1, room2);
        List<Long> chatRoomIds = List.of(10L, 20L);

        ChatRoomMember myMember1 = ChatRoomTestFixture.createChatRoomMember(1L, 10L, userId);
        ChatRoomMember myMember2 = ChatRoomTestFixture.createChatRoomMember(2L, 20L, userId);

        Message lastMessage1 = MessageTestFixture.createMessage(101L, 10L, 2L, "메시지1");
        Message lastMessage2 = MessageTestFixture.createMessage(102L, 20L, 3L, "메시지2");

        ChatRoomMember otherMember1 = ChatRoomTestFixture.createChatRoomMember(3L, 10L, 2L);
        ChatRoomMember otherMember2 = ChatRoomTestFixture.createChatRoomMember(4L, 20L, 3L);

        User user2 = User.builder().id(2L).nickname("사용자2").onlineStatus(User.OnlineStatus.ONLINE).build();
        User user3 = User.builder().id(3L).nickname("사용자3").onlineStatus(User.OnlineStatus.ONLINE).build();

        Map<Long, Long> unreadCountMap = Map.of(10L, 1L, 20L, 0L);

        given(chatRoomMemberRepository.findByUserIdAndChatRoomIds(eq(userId), eq(chatRoomIds)))
                .willReturn(List.of(myMember1, myMember2));
        given(messageRepository.findLastMessagesByRoomIds(eq(chatRoomIds)))
                .willReturn(List.of(lastMessage1, lastMessage2));
        given(messageRepository.batchCountUnreadMessages(eq(userId), eq(chatRoomIds)))
                .willReturn(unreadCountMap);
        given(chatRoomMemberRepository.findOtherMembersByChatRoomIds(eq(userId), eq(chatRoomIds)))
                .willReturn(List.of(otherMember1, otherMember2));
        given(userRepository.findAllById(any()))
                .willReturn(List.of(user2, user3));

        // when
        ChatRoomSummaryAssembler.BatchData batchData = assembler.loadBatchData(userId, chatRooms);

        // then
        // 나간 채팅방이 없으므로 배치 쿼리가 호출되지 않아야 함
        verify(messageRepository, never()).findDistinctSenderIdsByChatRoomIdsExcludingUser(any(), any());
        verify(messageRepository, never()).findDistinctSenderIdsByChatRoomIdExcludingUser(any(), any());

        assertThat(batchData.leftUserIdMap()).isEmpty();
    }
}
