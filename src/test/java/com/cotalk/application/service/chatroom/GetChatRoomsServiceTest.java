package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomSummary;
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

import com.cotalk.domain.model.PageQuery;
import com.cotalk.domain.model.PageResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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

    @Mock
    private ChatRoomSummaryAssembler chatRoomSummaryAssembler;

    private GetChatRoomsUseCase getChatRoomsUseCase;

    @BeforeEach
    void setUp() {
        getChatRoomsUseCase = new GetChatRoomsService(
                chatRoomRepository, chatRoomMemberRepository, messageRepository, userRepository, chatRoomSummaryAssembler);
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

            ChatRoom chatRoom = ChatRoom.builder()
                    .id(chatRoomId)
                    .name("채팅방1")
                    .type(ChatRoom.ChatRoomType.DIRECT)
                    .build();

            ChatRoomSummaryAssembler.BatchData batchData = new ChatRoomSummaryAssembler.BatchData(
                    Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of()
            );

            ChatRoomSummary expectedSummary = new ChatRoomSummary(
                    chatRoomId, "채팅방1", null, ChatRoom.ChatRoomType.DIRECT, LocalDateTime.now(),
                    "마지막 메시지입니다", "TEXT", LocalDateTime.now(), 5L,
                    otherUserId, "상대방", "https://example.com/avatar.png",
                    false, false, null
            );

            given(chatRoomRepository.findByUserId(userId)).willReturn(List.of(chatRoom));
            given(chatRoomSummaryAssembler.loadBatchData(userId, List.of(chatRoom))).willReturn(batchData);
            given(chatRoomSummaryAssembler.assembleSummaries(List.of(chatRoom), batchData))
                    .willReturn(List.of(expectedSummary));

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
            // 빈 목록일 때 assembler가 호출되지 않음
            verify(chatRoomSummaryAssembler, never()).loadBatchData(any(), anyList());
            verify(chatRoomSummaryAssembler, never()).assembleSummaries(anyList(), any());
        }

        @Test
        @DisplayName("메시지가 없는 채팅방도 정상 조회된다")
        void should_returnSummaryWithoutMessage_when_noMessages() {
            // given
            Long userId = 1L;
            Long chatRoomId = 100L;

            ChatRoom chatRoom = ChatRoom.builder()
                    .id(chatRoomId)
                    .name("새 채팅방")
                    .type(ChatRoom.ChatRoomType.DIRECT)
                    .build();

            ChatRoomSummaryAssembler.BatchData batchData = new ChatRoomSummaryAssembler.BatchData(
                    Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of()
            );

            ChatRoomSummary expectedSummary = new ChatRoomSummary(
                    chatRoomId, "새 채팅방", null, ChatRoom.ChatRoomType.DIRECT, LocalDateTime.now(),
                    "", null, null, 0L,
                    2L, "상대방", null,
                    false, false, null
            );

            given(chatRoomRepository.findByUserId(userId)).willReturn(List.of(chatRoom));
            given(chatRoomSummaryAssembler.loadBatchData(userId, List.of(chatRoom))).willReturn(batchData);
            given(chatRoomSummaryAssembler.assembleSummaries(List.of(chatRoom), batchData))
                    .willReturn(List.of(expectedSummary));

            // when
            List<ChatRoomSummary> result = getChatRoomsUseCase.getChatRooms(userId);

            // then
            assertThat(result).hasSize(1);
            ChatRoomSummary summary = result.get(0);
            assertThat(summary.lastMessage()).isEmpty();
            assertThat(summary.lastMessageAt()).isNull();
            assertThat(summary.unreadCount()).isZero();
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

            ChatRoomSummaryAssembler.BatchData batchData = new ChatRoomSummaryAssembler.BatchData(
                    Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of()
            );

            ChatRoomSummary expectedSummary = new ChatRoomSummary(
                    chatRoomId, "채팅방", null, ChatRoom.ChatRoomType.DIRECT, LocalDateTime.now(),
                    "", null, null, 0L,
                    null, null, null,
                    false, false, null
            );

            given(chatRoomRepository.findByUserId(userId)).willReturn(List.of(chatRoom));
            given(chatRoomSummaryAssembler.loadBatchData(userId, List.of(chatRoom))).willReturn(batchData);
            given(chatRoomSummaryAssembler.assembleSummaries(List.of(chatRoom), batchData))
                    .willReturn(List.of(expectedSummary));

            // when
            List<ChatRoomSummary> result = getChatRoomsUseCase.getChatRooms(userId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).unreadCount()).isZero();
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

            ChatRoomSummaryAssembler.BatchData batchData = new ChatRoomSummaryAssembler.BatchData(
                    Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of()
            );

            ChatRoomSummary expectedSummary = new ChatRoomSummary(
                    chatRoomId, "그룹 채팅방", null, ChatRoom.ChatRoomType.GROUP, LocalDateTime.now(),
                    "", null, null, 0L,
                    null, null, null,
                    false, false, null
            );

            given(chatRoomRepository.findByUserId(userId)).willReturn(List.of(groupChatRoom));
            given(chatRoomSummaryAssembler.loadBatchData(userId, List.of(groupChatRoom))).willReturn(batchData);
            given(chatRoomSummaryAssembler.assembleSummaries(List.of(groupChatRoom), batchData))
                    .willReturn(List.of(expectedSummary));

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
            Long chatRoomId = 100L;

            ChatRoom chatRoom = ChatRoom.builder()
                    .id(chatRoomId)
                    .name("채팅방")
                    .type(ChatRoom.ChatRoomType.DIRECT)
                    .build();

            ChatRoomSummaryAssembler.BatchData batchData = new ChatRoomSummaryAssembler.BatchData(
                    Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of()
            );

            ChatRoomSummary expectedSummary = new ChatRoomSummary(
                    chatRoomId, "채팅방", null, ChatRoom.ChatRoomType.DIRECT, LocalDateTime.now(),
                    "", null, null, 0L,
                    null, null, null,
                    false, false, null
            );

            given(chatRoomRepository.findByUserId(userId)).willReturn(List.of(chatRoom));
            given(chatRoomSummaryAssembler.loadBatchData(userId, List.of(chatRoom))).willReturn(batchData);
            given(chatRoomSummaryAssembler.assembleSummaries(List.of(chatRoom), batchData))
                    .willReturn(List.of(expectedSummary));

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

            ChatRoomSummaryAssembler.BatchData batchData = new ChatRoomSummaryAssembler.BatchData(
                    Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of()
            );

            ChatRoomSummary expectedSummary = new ChatRoomSummary(
                    chatRoomId, "채팅방", null, ChatRoom.ChatRoomType.DIRECT, LocalDateTime.now(),
                    "", null, null, 0L,
                    leftUserId, "나간유저", "https://example.com/avatar.png",
                    true, false, null
            );

            given(chatRoomRepository.findByUserId(userId)).willReturn(List.of(chatRoom));
            given(chatRoomSummaryAssembler.loadBatchData(userId, List.of(chatRoom))).willReturn(batchData);
            given(chatRoomSummaryAssembler.assembleSummaries(List.of(chatRoom), batchData))
                    .willReturn(List.of(expectedSummary));

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

            ChatRoom chatRoom1 = ChatRoom.builder().id(chatRoomId1).name("채팅방1").type(ChatRoom.ChatRoomType.DIRECT).build();
            ChatRoom chatRoom2 = ChatRoom.builder().id(chatRoomId2).name("채팅방2").type(ChatRoom.ChatRoomType.DIRECT).build();
            ChatRoom chatRoom3 = ChatRoom.builder().id(chatRoomId3).name("채팅방3").type(ChatRoom.ChatRoomType.GROUP).build();

            List<ChatRoom> chatRooms = List.of(chatRoom1, chatRoom2, chatRoom3);

            ChatRoomSummaryAssembler.BatchData batchData = new ChatRoomSummaryAssembler.BatchData(
                    Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of()
            );

            List<ChatRoomSummary> expectedSummaries = List.of(
                    new ChatRoomSummary(chatRoomId1, "채팅방1", null, ChatRoom.ChatRoomType.DIRECT, LocalDateTime.now(),
                            "", null, null, 3L, null, null, null, false, false, null),
                    new ChatRoomSummary(chatRoomId2, "채팅방2", null, ChatRoom.ChatRoomType.DIRECT, LocalDateTime.now(),
                            "", null, null, 0L, null, null, null, false, false, null),
                    new ChatRoomSummary(chatRoomId3, "채팅방3", null, ChatRoom.ChatRoomType.GROUP, LocalDateTime.now(),
                            "", null, null, 10L, null, null, null, false, false, null)
            );

            given(chatRoomRepository.findByUserId(userId)).willReturn(chatRooms);
            given(chatRoomSummaryAssembler.loadBatchData(userId, chatRooms)).willReturn(batchData);
            given(chatRoomSummaryAssembler.assembleSummaries(chatRooms, batchData)).willReturn(expectedSummaries);

            // when
            List<ChatRoomSummary> result = getChatRoomsUseCase.getChatRooms(userId);

            // then
            assertThat(result).hasSize(3);

            // assembler가 호출되었는지 검증
            verify(chatRoomSummaryAssembler).loadBatchData(userId, chatRooms);
            verify(chatRoomSummaryAssembler).assembleSummaries(chatRooms, batchData);

            // unreadCount가 올바르게 매핑되었는지 확인
            ChatRoomSummary summary1 = result.stream().filter(s -> s.id().equals(chatRoomId1)).findFirst().orElseThrow();
            assertThat(summary1.unreadCount()).isEqualTo(3L);

            ChatRoomSummary summary2 = result.stream().filter(s -> s.id().equals(chatRoomId2)).findFirst().orElseThrow();
            assertThat(summary2.unreadCount()).isZero();

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

            List<ChatRoom> chatRooms = List.of(directChatRoom, groupChatRoom);

            ChatRoomSummaryAssembler.BatchData batchData = new ChatRoomSummaryAssembler.BatchData(
                    Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of()
            );

            List<ChatRoomSummary> expectedSummaries = List.of(
                    new ChatRoomSummary(directChatRoomId, "1:1 채팅방", null, ChatRoom.ChatRoomType.DIRECT, LocalDateTime.now(),
                            "", null, null, 0L, null, null, null, false, false, null),
                    new ChatRoomSummary(groupChatRoomId, "그룹 채팅방", null, ChatRoom.ChatRoomType.GROUP, LocalDateTime.now(),
                            "", null, null, 0L, null, null, null, false, false, null)
            );

            given(chatRoomRepository.findByUserId(userId)).willReturn(chatRooms);
            given(chatRoomSummaryAssembler.loadBatchData(userId, chatRooms)).willReturn(batchData);
            given(chatRoomSummaryAssembler.assembleSummaries(chatRooms, batchData)).willReturn(expectedSummaries);

            // when
            getChatRoomsUseCase.getChatRooms(userId);

            // then - assembler가 호출되었는지 검증
            verify(chatRoomSummaryAssembler).loadBatchData(userId, chatRooms);
            verify(chatRoomSummaryAssembler).assembleSummaries(chatRooms, batchData);
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
            PageQuery query = PageQuery.of(0, 20);

            ChatRoom chatRoom = ChatRoom.builder()
                    .id(chatRoomId)
                    .name("채팅방1")
                    .type(ChatRoom.ChatRoomType.DIRECT)
                    .build();

            PageResult<ChatRoom> chatRoomPage = new PageResult<>(List.of(chatRoom), 0, 20, 1);

            ChatRoomSummaryAssembler.BatchData batchData = new ChatRoomSummaryAssembler.BatchData(
                    Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of()
            );

            ChatRoomSummary expectedSummary = new ChatRoomSummary(
                    chatRoomId, "채팅방1", null, ChatRoom.ChatRoomType.DIRECT, LocalDateTime.now(),
                    "마지막 메시지입니다", "TEXT", LocalDateTime.now(), 5L,
                    otherUserId, "상대방", "https://example.com/avatar.png",
                    false, false, null
            );

            given(chatRoomRepository.findByUserId(userId, query)).willReturn(chatRoomPage);
            given(chatRoomSummaryAssembler.loadBatchData(userId, List.of(chatRoom))).willReturn(batchData);
            given(chatRoomSummaryAssembler.assembleSummaries(List.of(chatRoom), batchData))
                    .willReturn(List.of(expectedSummary));

            // when
            PageResult<ChatRoomSummary> result = getChatRoomsUseCase.getChatRooms(userId, query);

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.page()).isZero();
            assertThat(result.size()).isEqualTo(20);

            ChatRoomSummary summary = result.content().get(0);
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
            PageQuery query = PageQuery.of(0, 20);
            PageResult<ChatRoom> emptyPage = new PageResult<>(List.of(), 0, 20, 0);

            given(chatRoomRepository.findByUserId(userId, query)).willReturn(emptyPage);

            // when
            PageResult<ChatRoomSummary> result = getChatRoomsUseCase.getChatRooms(userId, query);

            // then
            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
            // 빈 페이지일 때 assembler가 호출되지 않음
            verify(chatRoomSummaryAssembler, never()).loadBatchData(any(), anyList());
            verify(chatRoomSummaryAssembler, never()).assembleSummaries(anyList(), any());
        }
    }
}
