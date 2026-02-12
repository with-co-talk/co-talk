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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
 * GetChatRoomsService 단위 테스트.
 * 배치 쿼리를 사용하여 N+1 문제를 해결한 새로운 구현을 테스트한다.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetChatRoomsService 배치 쿼리")
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

    @Nested
    @DisplayName("채팅방 목록 조회 시")
    class GetChatRoomsTest {

        @Test
        @DisplayName("마지막 메시지, 안읽은 개수, 상대방 정보를 포함하여 반환한다")
        void should_returnChatRoomSummaries_when_validUserId() {
            // given
            Long userId = 1L;
            Long otherUserId = 2L;
            Long chatRoomId = 100L;
            LocalDateTime lastReadAt = LocalDateTime.now().minusHours(1);

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

            // 배치 쿼리 모킹
            given(chatRoomRepository.findByUserId(userId)).willReturn(List.of(chatRoom));
            given(chatRoomMemberRepository.findByUserIdAndChatRoomIds(userId, List.of(chatRoomId)))
                    .willReturn(List.of(myMember));
            given(messageRepository.findLastMessagesByRoomIds(List.of(chatRoomId)))
                    .willReturn(List.of(lastMessage));
            given(messageRepository.batchCountUnreadMessages(userId, List.of(chatRoomId)))
                    .willReturn(Map.of(chatRoomId, 5L));
            given(chatRoomMemberRepository.findOtherMembersByChatRoomIds(userId, List.of(chatRoomId)))
                    .willReturn(List.of(otherMember));
            given(userRepository.findAllById(any())).willReturn(List.of(otherUser));

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
        @DisplayName("채팅방이 없을 때 빈 목록을 반환한다")
        void should_returnEmptyList_when_noChatRooms() {
            // given
            Long userId = 1L;
            given(chatRoomRepository.findByUserId(userId)).willReturn(List.of());

            // when
            List<ChatRoomSummary> result = getChatRoomsUseCase.getChatRooms(userId);

            // then
            assertThat(result).isEmpty();
            // 빈 목록일 때 다른 배치 쿼리가 호출되지 않음
            verify(chatRoomMemberRepository, never()).findByUserIdAndChatRoomIds(any(), anyList());
            verify(messageRepository, never()).findLastMessagesByRoomIds(anyList());
        }

        @Test
        @DisplayName("메시지가 없는 채팅방도 정상 조회된다")
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

            // 배치 쿼리 모킹 - 빈 결과
            given(chatRoomRepository.findByUserId(userId)).willReturn(List.of(chatRoom));
            given(chatRoomMemberRepository.findByUserIdAndChatRoomIds(userId, List.of(chatRoomId)))
                    .willReturn(List.of(myMember));
            given(messageRepository.findLastMessagesByRoomIds(List.of(chatRoomId)))
                    .willReturn(List.of()); // 메시지 없음
            given(messageRepository.batchCountUnreadMessages(userId, List.of(chatRoomId)))
                    .willReturn(Map.of()); // 읽지 않은 메시지 없음
            given(chatRoomMemberRepository.findOtherMembersByChatRoomIds(userId, List.of(chatRoomId)))
                    .willReturn(List.of(otherMember));
            given(userRepository.findAllById(any())).willReturn(List.of(otherUser));

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
        @DisplayName("멤버 정보가 없는 경우 unreadCount는 0이다")
        void should_returnZeroUnreadCount_when_memberNotFound() {
            // given
            Long userId = 1L;
            Long chatRoomId = 100L;

            ChatRoom chatRoom = ChatRoom.builder()
                    .id(chatRoomId)
                    .name("채팅방")
                    .type(ChatRoom.ChatRoomType.DIRECT)
                    .build();

            // 배치 쿼리 모킹 - 멤버 없음
            given(chatRoomRepository.findByUserId(userId)).willReturn(List.of(chatRoom));
            given(chatRoomMemberRepository.findByUserIdAndChatRoomIds(userId, List.of(chatRoomId)))
                    .willReturn(List.of()); // 멤버 없음
            given(messageRepository.findLastMessagesByRoomIds(List.of(chatRoomId)))
                    .willReturn(List.of());
            given(messageRepository.batchCountUnreadMessages(userId, List.of(chatRoomId)))
                    .willReturn(Map.of());
            given(chatRoomMemberRepository.findOtherMembersByChatRoomIds(userId, List.of(chatRoomId)))
                    .willReturn(List.of());
            given(userRepository.findAllById(any())).willReturn(List.of());

            // when
            List<ChatRoomSummary> result = getChatRoomsUseCase.getChatRooms(userId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).unreadCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("그룹 채팅방은 상대방 정보가 null이다")
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

            // 배치 쿼리 모킹
            given(chatRoomRepository.findByUserId(userId)).willReturn(List.of(groupChatRoom));
            given(chatRoomMemberRepository.findByUserIdAndChatRoomIds(userId, List.of(chatRoomId)))
                    .willReturn(List.of(myMember));
            given(messageRepository.findLastMessagesByRoomIds(List.of(chatRoomId)))
                    .willReturn(List.of());
            given(messageRepository.batchCountUnreadMessages(userId, List.of(chatRoomId)))
                    .willReturn(Map.of());
            // 그룹 채팅방은 DIRECT 타입이 아니므로 빈 리스트로 호출됨
            given(chatRoomMemberRepository.findOtherMembersByChatRoomIds(eq(userId), anyList()))
                    .willReturn(List.of());
            given(userRepository.findAllById(any())).willReturn(List.of());

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
        @DisplayName("1:1 채팅방에서 상대방 사용자 정보가 없는 경우 null 처리된다")
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

            // 배치 쿼리 모킹
            given(chatRoomRepository.findByUserId(userId)).willReturn(List.of(chatRoom));
            given(chatRoomMemberRepository.findByUserIdAndChatRoomIds(userId, List.of(chatRoomId)))
                    .willReturn(List.of(myMember));
            given(messageRepository.findLastMessagesByRoomIds(List.of(chatRoomId)))
                    .willReturn(List.of());
            given(messageRepository.batchCountUnreadMessages(userId, List.of(chatRoomId)))
                    .willReturn(Map.of());
            given(chatRoomMemberRepository.findOtherMembersByChatRoomIds(userId, List.of(chatRoomId)))
                    .willReturn(List.of(otherMember));
            given(userRepository.findAllById(any())).willReturn(List.of()); // 사용자 없음

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
        @DisplayName("1:1 채팅방에서 상대방이 나간 경우 메시지 기록에서 상대방 ID를 복구한다")
        void should_recoverOtherUserId_when_otherUserLeftButHasMessages() {
            // given
            Long userId = 1L;
            Long leftUserId = 2L; // 나간 사용자
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

            User leftUser = User.builder()
                    .id(leftUserId)
                    .email("left@test.com")
                    .nickname("나간유저")
                    .passwordHash("hash")
                    .avatarUrl("https://example.com/avatar.png")
                    .build();

            // 배치 쿼리 모킹
            given(chatRoomRepository.findByUserId(userId)).willReturn(List.of(chatRoom));
            given(chatRoomMemberRepository.findByUserIdAndChatRoomIds(userId, List.of(chatRoomId)))
                    .willReturn(List.of(myMember));
            given(messageRepository.findLastMessagesByRoomIds(List.of(chatRoomId)))
                    .willReturn(List.of());
            given(messageRepository.batchCountUnreadMessages(userId, List.of(chatRoomId)))
                    .willReturn(Map.of());
            // 상대방 멤버가 없음 (나갔기 때문)
            given(chatRoomMemberRepository.findOtherMembersByChatRoomIds(userId, List.of(chatRoomId)))
                    .willReturn(List.of());
            // 메시지 기록에서 상대방 ID 복구
            given(messageRepository.findDistinctSenderIdsByChatRoomIdExcludingUser(chatRoomId, userId))
                    .willReturn(List.of(leftUserId));
            given(userRepository.findAllById(any())).willReturn(List.of(leftUser));

            // when
            List<ChatRoomSummary> result = getChatRoomsUseCase.getChatRooms(userId);

            // then
            assertThat(result).hasSize(1);
            ChatRoomSummary summary = result.get(0);
            assertThat(summary.isOtherUserLeft()).isTrue();
            assertThat(summary.otherUserId()).isEqualTo(leftUserId);
            assertThat(summary.otherUserNickname()).isEqualTo("나간유저");
            assertThat(summary.otherUserAvatarUrl()).isEqualTo("https://example.com/avatar.png");
        }
    }

    @Nested
    @DisplayName("배치 쿼리 동작 시")
    class BatchQueryBehaviorTest {

        @Test
        @DisplayName("여러 채팅방 조회 시 배치 쿼리가 사용된다")
        void should_useBatchQueries_when_multipleChatRooms() {
            // given
            Long userId = 1L;
            Long chatRoomId1 = 100L;
            Long chatRoomId2 = 101L;
            Long chatRoomId3 = 102L;

            List<Long> chatRoomIds = List.of(chatRoomId1, chatRoomId2, chatRoomId3);

            ChatRoom chatRoom1 = ChatRoom.builder().id(chatRoomId1).name("채팅방1").type(ChatRoom.ChatRoomType.DIRECT).build();
            ChatRoom chatRoom2 = ChatRoom.builder().id(chatRoomId2).name("채팅방2").type(ChatRoom.ChatRoomType.DIRECT).build();
            ChatRoom chatRoom3 = ChatRoom.builder().id(chatRoomId3).name("채팅방3").type(ChatRoom.ChatRoomType.GROUP).build();

            // 배치 쿼리 모킹
            given(chatRoomRepository.findByUserId(userId)).willReturn(List.of(chatRoom1, chatRoom2, chatRoom3));
            given(chatRoomMemberRepository.findByUserIdAndChatRoomIds(eq(userId), eq(chatRoomIds)))
                    .willReturn(List.of());
            given(messageRepository.findLastMessagesByRoomIds(eq(chatRoomIds)))
                    .willReturn(List.of());
            given(messageRepository.batchCountUnreadMessages(eq(userId), eq(chatRoomIds)))
                    .willReturn(Map.of(chatRoomId1, 3L, chatRoomId2, 0L, chatRoomId3, 10L));
            given(chatRoomMemberRepository.findOtherMembersByChatRoomIds(eq(userId), anyList()))
                    .willReturn(List.of());
            given(userRepository.findAllById(any())).willReturn(List.of());

            // when
            List<ChatRoomSummary> result = getChatRoomsUseCase.getChatRooms(userId);

            // then
            assertThat(result).hasSize(3);

            // 배치 쿼리가 사용되었는지 검증
            verify(chatRoomMemberRepository).findByUserIdAndChatRoomIds(userId, chatRoomIds);
            verify(messageRepository).findLastMessagesByRoomIds(chatRoomIds);
            verify(messageRepository).batchCountUnreadMessages(userId, chatRoomIds);

            // unreadCount가 올바르게 매핑되었는지 확인
            ChatRoomSummary summary1 = result.stream().filter(s -> s.id().equals(chatRoomId1)).findFirst().orElseThrow();
            assertThat(summary1.unreadCount()).isEqualTo(3L);

            ChatRoomSummary summary2 = result.stream().filter(s -> s.id().equals(chatRoomId2)).findFirst().orElseThrow();
            assertThat(summary2.unreadCount()).isEqualTo(0L);

            ChatRoomSummary summary3 = result.stream().filter(s -> s.id().equals(chatRoomId3)).findFirst().orElseThrow();
            assertThat(summary3.unreadCount()).isEqualTo(10L);
        }

        @Test
        @DisplayName("DIRECT 채팅방만 상대방 멤버 조회에 포함된다")
        void should_queryOtherMembersOnlyForDirectRooms() {
            // given
            Long userId = 1L;
            Long directChatRoomId = 100L;
            Long groupChatRoomId = 101L;

            ChatRoom directChatRoom = ChatRoom.builder()
                    .id(directChatRoomId)
                    .name("1:1 채팅방")
                    .type(ChatRoom.ChatRoomType.DIRECT)
                    .build();

            ChatRoom groupChatRoom = ChatRoom.builder()
                    .id(groupChatRoomId)
                    .name("그룹 채팅방")
                    .type(ChatRoom.ChatRoomType.GROUP)
                    .build();

            // 배치 쿼리 모킹
            given(chatRoomRepository.findByUserId(userId)).willReturn(List.of(directChatRoom, groupChatRoom));
            given(chatRoomMemberRepository.findByUserIdAndChatRoomIds(eq(userId), anyList()))
                    .willReturn(List.of());
            given(messageRepository.findLastMessagesByRoomIds(anyList()))
                    .willReturn(List.of());
            given(messageRepository.batchCountUnreadMessages(eq(userId), anyList()))
                    .willReturn(Map.of());
            // DIRECT 채팅방 ID만 전달되어야 함
            given(chatRoomMemberRepository.findOtherMembersByChatRoomIds(eq(userId), eq(List.of(directChatRoomId))))
                    .willReturn(List.of());
            given(userRepository.findAllById(any())).willReturn(List.of());

            // when
            getChatRoomsUseCase.getChatRooms(userId);

            // then - DIRECT 채팅방 ID만 전달되었는지 검증
            verify(chatRoomMemberRepository).findOtherMembersByChatRoomIds(userId, List.of(directChatRoomId));
        }
    }

    @Nested
    @DisplayName("페이지네이션된 채팅방 목록 조회 시")
    class GetPagedChatRoomsTest {

        @Test
        @DisplayName("DB 레벨 페이지네이션으로 채팅방 목록을 조회한다")
        void should_returnPagedChatRoomSummaries_when_pageableProvided() {
            // given
            Long userId = 1L;
            Long otherUserId = 2L;
            Long chatRoomId = 100L;
            Pageable pageable = PageRequest.of(0, 20);

            ChatRoom chatRoom = ChatRoom.builder()
                    .id(chatRoomId)
                    .name("채팅방1")
                    .type(ChatRoom.ChatRoomType.DIRECT)
                    .build();

            Page<ChatRoom> chatRoomPage = new PageImpl<>(List.of(chatRoom), pageable, 1);

            ChatRoomMember myMember = ChatRoomMember.builder()
                    .id(500L)
                    .chatRoomId(chatRoomId)
                    .userId(userId)
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

            // 배치 쿼리 모킹
            given(chatRoomRepository.findByUserId(userId, pageable)).willReturn(chatRoomPage);
            given(chatRoomMemberRepository.findByUserIdAndChatRoomIds(userId, List.of(chatRoomId)))
                    .willReturn(List.of(myMember));
            given(messageRepository.findLastMessagesByRoomIds(List.of(chatRoomId)))
                    .willReturn(List.of(lastMessage));
            given(messageRepository.batchCountUnreadMessages(userId, List.of(chatRoomId)))
                    .willReturn(Map.of(chatRoomId, 5L));
            given(chatRoomMemberRepository.findOtherMembersByChatRoomIds(userId, List.of(chatRoomId)))
                    .willReturn(List.of(otherMember));
            given(userRepository.findAllById(any())).willReturn(List.of(otherUser));

            // when
            Page<ChatRoomSummary> result = getChatRoomsUseCase.getChatRooms(userId, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getNumber()).isEqualTo(0);
            assertThat(result.getSize()).isEqualTo(20);

            ChatRoomSummary summary = result.getContent().get(0);
            assertThat(summary.id()).isEqualTo(chatRoomId);
            assertThat(summary.name()).isEqualTo("채팅방1");
            assertThat(summary.lastMessage()).isEqualTo("마지막 메시지입니다");
            assertThat(summary.unreadCount()).isEqualTo(5L);
            assertThat(summary.otherUserId()).isEqualTo(otherUserId);
            assertThat(summary.otherUserNickname()).isEqualTo("상대방");
        }

        @Test
        @DisplayName("채팅방이 없을 때 빈 페이지를 반환한다")
        void should_returnEmptyPage_when_noChatRooms() {
            // given
            Long userId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            Page<ChatRoom> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(chatRoomRepository.findByUserId(userId, pageable)).willReturn(emptyPage);

            // when
            Page<ChatRoomSummary> result = getChatRoomsUseCase.getChatRooms(userId, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
            // 빈 페이지일 때 다른 배치 쿼리가 호출되지 않음
            verify(chatRoomMemberRepository, never()).findByUserIdAndChatRoomIds(any(), anyList());
            verify(messageRepository, never()).findLastMessagesByRoomIds(anyList());
        }
    }
}
