package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.ChatRoomSummary;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.chatroom.GetChatRoomsUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetChatRoomsServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    private GetChatRoomsUseCase getChatRoomsUseCase;

    @BeforeEach
    void setUp() {
        getChatRoomsUseCase = new GetChatRoomsService(
                chatRoomRepository, chatRoomMemberRepository, messageRepository, userRepository);
    }

    @Test
    @DisplayName("사용자의 채팅방 목록 조회 성공 - 마지막 메시지, 안읽은 개수, 상대방 정보 포함")
    void should_returnChatRoomSummaries_when_validUserId() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long chatRoomId = 100L;
        LocalDateTime lastReadAt = LocalDateTime.now().minusHours(1);
        LocalDateTime lastMessageAt = LocalDateTime.now();

        ChatRoom chatRoom = ChatRoom.builder()
                .id(chatRoomId)
                .name("채팅방1")
                .type(ChatRoom.ChatRoomType.DIRECT)
                .build();

        ChatRoomMember myMember = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .lastReadAt(lastReadAt)
                .lastReadMessageId(900L)
                .build();

        ChatRoomMember otherMember = ChatRoomMember.builder()
                .id(501L)
                .chatRoomId(chatRoomId)
                .userId(otherUserId)
                .build();

        User otherUser = User.builder()
                .id(otherUserId)
                .email("other@test.com")
                .nickname("상대방")
                .passwordHash("hash")
                .avatarUrl("https://example.com/avatar.png")
                .build();

        Message lastMessage = Message.builder()
                .id(1000L)
                .chatRoomId(chatRoomId)
                .senderId(otherUserId)
                .content("마지막 메시지입니다")
                .build();

        given(chatRoomRepository.findByUserId(userId)).willReturn(List.of(chatRoom));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(myMember));
        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(List.of(myMember, otherMember));
        given(userRepository.findById(otherUserId)).willReturn(Optional.of(otherUser));
        given(messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId))
                .willReturn(Optional.of(lastMessage));
        given(messageRepository.countUnreadMessagesByLastReadMessageId(chatRoomId, userId, 900L))
                .willReturn(5L);

        // when
        List<ChatRoomSummary> result = getChatRoomsUseCase.getChatRooms(userId);

        // then
        assertThat(result).hasSize(1);
        ChatRoomSummary summary = result.get(0);
        assertThat(summary.id()).isEqualTo(chatRoomId);
        assertThat(summary.name()).isEqualTo("채팅방1");
        assertThat(summary.lastMessage()).isEqualTo("마지막 메시지입니다");
        assertThat(summary.unreadCount()).isEqualTo(5L);
        assertThat(summary.otherUserId()).isEqualTo(otherUserId);
        assertThat(summary.otherUserNickname()).isEqualTo("상대방");
        assertThat(summary.otherUserAvatarUrl()).isEqualTo("https://example.com/avatar.png");
    }

    @Test
    @DisplayName("채팅방이 없을 때 빈 목록 반환")
    void should_returnEmptyList_when_noChatRooms() {
        // given
        Long userId = 1L;
        given(chatRoomRepository.findByUserId(userId)).willReturn(List.of());

        // when
        List<ChatRoomSummary> result = getChatRoomsUseCase.getChatRooms(userId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("메시지가 없는 채팅방도 정상 조회")
    void should_returnSummaryWithoutMessage_when_noMessages() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long chatRoomId = 100L;

        ChatRoom chatRoom = ChatRoom.builder()
                .id(chatRoomId)
                .name("새 채팅방")
                .type(ChatRoom.ChatRoomType.DIRECT)
                .build();

        ChatRoomMember myMember = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .lastReadMessageId(null)
                .build();

        ChatRoomMember otherMember = ChatRoomMember.builder()
                .id(501L)
                .chatRoomId(chatRoomId)
                .userId(otherUserId)
                .build();

        User otherUser = User.builder()
                .id(otherUserId)
                .email("other@test.com")
                .nickname("상대방")
                .passwordHash("hash")
                .build();

        given(chatRoomRepository.findByUserId(userId)).willReturn(List.of(chatRoom));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(myMember));
        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(List.of(myMember, otherMember));
        given(userRepository.findById(otherUserId)).willReturn(Optional.of(otherUser));
        given(messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId))
                .willReturn(Optional.empty());

        // when
        List<ChatRoomSummary> result = getChatRoomsUseCase.getChatRooms(userId);

        // then
        assertThat(result).hasSize(1);
        ChatRoomSummary summary = result.get(0);
        assertThat(summary.lastMessage()).isEmpty();
        assertThat(summary.lastMessageAt()).isNull();
        assertThat(summary.unreadCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("멤버 정보가 없는 경우 unreadCount는 0")
    void should_returnZeroUnreadCount_when_memberNotFound() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;

        ChatRoom chatRoom = ChatRoom.builder()
                .id(chatRoomId)
                .name("채팅방")
                .type(ChatRoom.ChatRoomType.DIRECT)
                .build();

        given(chatRoomRepository.findByUserId(userId)).willReturn(List.of(chatRoom));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.empty()); // 멤버 정보 없음
        given(messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId))
                .willReturn(Optional.empty());

        // when
        List<ChatRoomSummary> result = getChatRoomsUseCase.getChatRooms(userId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).unreadCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("그룹 채팅방은 상대방 정보가 null")
    void should_returnNullOtherUserInfo_when_groupChatRoom() {
        // given
        Long userId = 1L;
        Long chatRoomId = 100L;

        ChatRoom groupChatRoom = ChatRoom.builder()
                .id(chatRoomId)
                .name("그룹 채팅방")
                .type(ChatRoom.ChatRoomType.GROUP)
                .build();

        ChatRoomMember myMember = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .lastReadMessageId(null)
                .build();

        given(chatRoomRepository.findByUserId(userId)).willReturn(List.of(groupChatRoom));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(myMember));
        // 그룹 채팅방은 getOtherUserInfo에서 바로 null 반환하므로 userRepository 호출 안됨
        given(messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId))
                .willReturn(Optional.empty());

        // when
        List<ChatRoomSummary> result = getChatRoomsUseCase.getChatRooms(userId);

        // then
        assertThat(result).hasSize(1);
        ChatRoomSummary summary = result.get(0);
        assertThat(summary.otherUserId()).isNull();
        assertThat(summary.otherUserNickname()).isNull();
        assertThat(summary.otherUserAvatarUrl()).isNull();
    }

    @Test
    @DisplayName("1:1 채팅방에서 상대방 사용자 정보가 없는 경우 null 처리")
    void should_handleNullOtherUser_when_otherUserNotFound() {
        // given
        Long userId = 1L;
        Long otherUserId = 999L; // 삭제된 사용자
        Long chatRoomId = 100L;

        ChatRoom chatRoom = ChatRoom.builder()
                .id(chatRoomId)
                .name("채팅방")
                .type(ChatRoom.ChatRoomType.DIRECT)
                .build();

        ChatRoomMember myMember = ChatRoomMember.builder()
                .id(500L)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .lastReadMessageId(null)
                .build();

        ChatRoomMember otherMember = ChatRoomMember.builder()
                .id(501L)
                .chatRoomId(chatRoomId)
                .userId(otherUserId)
                .build();

        given(chatRoomRepository.findByUserId(userId)).willReturn(List.of(chatRoom));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
                .willReturn(Optional.of(myMember));
        given(chatRoomMemberRepository.findByChatRoomId(chatRoomId))
                .willReturn(List.of(myMember, otherMember));
        given(userRepository.findById(otherUserId))
                .willReturn(Optional.empty()); // 사용자 없음
        given(messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId))
                .willReturn(Optional.empty());

        // when
        List<ChatRoomSummary> result = getChatRoomsUseCase.getChatRooms(userId);

        // then
        assertThat(result).hasSize(1);
        ChatRoomSummary summary = result.get(0);
        assertThat(summary.otherUserId()).isNull();
        assertThat(summary.otherUserNickname()).isNull();
        assertThat(summary.otherUserAvatarUrl()).isNull();
    }
}
