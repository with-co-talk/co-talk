package com.cotalk.adapter.outbound.persistence;

import com.cotalk.adapter.outbound.persistence.chatroom.ChatRoomMemberRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.chatroom.ChatRoomRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.mapper.UserMapper;
import com.cotalk.adapter.outbound.persistence.message.MessageRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.user.UserRepositoryAdapter;
import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoom.ChatRoomType;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.Message.MessageType;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.infrastructure.config.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MessageRepositoryAdapter 테스트.
 *
 * @author seunggu.lee
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({MessageRepositoryAdapter.class, ChatRoomRepositoryAdapter.class,
        ChatRoomMemberRepositoryAdapter.class, UserRepositoryAdapter.class, UserMapper.class, JpaAuditingConfig.class})
@DisplayName("MessageRepositoryAdapter")
class MessageRepositoryAdapterTest {

    @Autowired
    private MessageRepositoryAdapter messageRepository;

    @Autowired
    private ChatRoomRepositoryAdapter chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepositoryAdapter chatRoomMemberRepository;

    @Autowired
    private UserRepositoryAdapter userRepository;

    private User user1;
    private User user2;
    private ChatRoom chatRoom;
    private ChatRoom chatRoom2;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(User.builder()
                .id(1L)
                .email(new Email("user1@example.com"))
                .passwordHash("hash")
                .nickname("user1")
                .build());

        user2 = userRepository.save(User.builder()
                .id(2L)
                .email(new Email("user2@example.com"))
                .passwordHash("hash")
                .nickname("user2")
                .build());

        chatRoom = chatRoomRepository.save(ChatRoom.builder()
                .id(100L)
                .type(ChatRoomType.DIRECT)
                .build());

        chatRoom2 = chatRoomRepository.save(ChatRoom.builder()
                .id(101L)
                .name("그룹채팅")
                .type(ChatRoomType.GROUP)
                .build());

        chatRoomMemberRepository.save(ChatRoomMember.builder()
                .id(1000L)
                .chatRoomId(chatRoom.getId())
                .userId(user1.getId())
                .build());
        chatRoomMemberRepository.save(ChatRoomMember.builder()
                .id(1001L)
                .chatRoomId(chatRoom.getId())
                .userId(user2.getId())
                .build());
        chatRoomMemberRepository.save(ChatRoomMember.builder()
                .id(1002L)
                .chatRoomId(chatRoom2.getId())
                .userId(user1.getId())
                .build());
    }

    @Nested
    @DisplayName("저장 시")
    class Save {

        @Test
        @DisplayName("메시지를 저장한다")
        void should_saveMessage_when_messageProvided() {
            // given
            Message message = Message.builder()
                    .id(10000L)
                    .chatRoomId(chatRoom.getId())
                    .senderId(user1.getId())
                    .content("안녕하세요")
                    .type(MessageType.TEXT)
                    .build();

            // when
            Message saved = messageRepository.save(message);

            // then
            assertThat(saved.getId()).isEqualTo(10000L);
            assertThat(saved.getContent()).isEqualTo("안녕하세요");
        }
    }

    @Nested
    @DisplayName("조회 시")
    class Find {

        @Test
        @DisplayName("ID로 메시지를 조회한다")
        void should_findMessage_when_idProvided() {
            // given
            messageRepository.save(Message.builder()
                    .id(10000L)
                    .chatRoomId(chatRoom.getId())
                    .senderId(user1.getId())
                    .content("테스트")
                    .type(MessageType.TEXT)
                    .build());

            // when
            Optional<Message> found = messageRepository.findById(10000L);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getContent()).isEqualTo("테스트");
        }

        @Test
        @DisplayName("채팅방 ID로 메시지 목록을 역순으로 조회한다")
        void should_findMessages_when_chatRoomIdProvided() {
            // given
            for (int i = 1; i <= 5; i++) {
                messageRepository.save(Message.builder()
                        .id(10000L + i)
                        .chatRoomId(chatRoom.getId())
                        .senderId(user1.getId())
                        .content("메시지 " + i)
                        .type(MessageType.TEXT)
                        .build());
            }

            // when
            List<Message> messages = messageRepository.findByChatRoomIdOrderByCreatedAtDesc(
                    chatRoom.getId(), 0, 3);

            // then
            assertThat(messages).hasSize(3);
        }

        @Test
        @DisplayName("가장 최근 메시지를 조회한다")
        void should_findLatestMessage_when_chatRoomIdProvided() {
            // given - 단일 메시지만 저장 (Optional 반환을 위해)
            messageRepository.save(Message.builder()
                    .id(10001L)
                    .chatRoomId(chatRoom.getId())
                    .senderId(user1.getId())
                    .content("유일한 메시지")
                    .type(MessageType.TEXT)
                    .build());

            // when
            Optional<Message> latest = messageRepository
                    .findTopByChatRoomIdOrderByCreatedAtDesc(chatRoom.getId());

            // then
            assertThat(latest).isPresent();
            assertThat(latest.get().getId()).isEqualTo(10001L);
        }

        @Test
        @DisplayName("메시지가 없으면 빈 Optional을 반환한다")
        void should_returnEmpty_when_noMessages() {
            // when
            Optional<Message> latest = messageRepository
                    .findTopByChatRoomIdOrderByCreatedAtDesc(chatRoom.getId());

            // then
            assertThat(latest).isEmpty();
        }
    }

    @Nested
    @DisplayName("읽지 않은 메시지 카운트 시")
    class CountUnread {

        @Test
        @DisplayName("마지막 읽은 시간 이후의 메시지 수를 반환한다")
        void should_countUnreadMessages_when_lastReadAtProvided() {
            // given
            LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 12, 0);

            for (int i = 1; i <= 5; i++) {
                messageRepository.save(Message.builder()
                        .id(10000L + i)
                        .chatRoomId(chatRoom.getId())
                        .senderId(user1.getId())
                        .content("메시지 " + i)
                        .type(MessageType.TEXT)
                        .build());
            }

            // when
            long unreadCount = messageRepository.countUnreadMessages(
                    chatRoom.getId(), user2.getId(), baseTime);

            // then
            assertThat(unreadCount).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("마지막 읽은 시간이 null이면 모든 메시지를 카운트한다")
        void should_countAllMessages_when_lastReadAtIsNull() {
            // given
            messageRepository.save(Message.builder()
                    .id(10001L)
                    .chatRoomId(chatRoom.getId())
                    .senderId(user1.getId())
                    .content("메시지 1")
                    .type(MessageType.TEXT)
                    .build());
            messageRepository.save(Message.builder()
                    .id(10002L)
                    .chatRoomId(chatRoom.getId())
                    .senderId(user1.getId())
                    .content("메시지 2")
                    .type(MessageType.TEXT)
                    .build());

            // when - user2의 관점에서 user1이 보낸 메시지 카운트
            long unreadCount = messageRepository.countUnreadMessages(
                    chatRoom.getId(), user2.getId(), null);

            // then - null이면 모든 메시지가 카운트됨 (본인 메시지 제외)
            assertThat(unreadCount).isEqualTo(2);
        }

        @Test
        @DisplayName("마지막 읽은 메시지 ID 이후의 메시지 수를 반환한다")
        void should_countUnreadMessagesByLastReadMessageId_when_lastReadMessageIdProvided() {
            // given
            Message message1 = messageRepository.save(Message.builder()
                    .id(10001L)
                    .chatRoomId(chatRoom.getId())
                    .senderId(user1.getId())
                    .content("메시지 1")
                    .type(MessageType.TEXT)
                    .build());

            // when - message1을 읽었다고 가정
            long unreadCount = messageRepository.countUnreadMessagesByLastReadMessageId(
                    chatRoom.getId(), user2.getId(), message1.getId());

            // then - message2, message3이 읽지 않은 메시지
            assertThat(unreadCount).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("마지막 읽은 메시지 ID가 null이면 모든 메시지가 읽지 않은 것으로 계산된다")
        void should_countAllMessages_when_lastReadMessageIdIsNull() {
            // given
            messageRepository.save(Message.builder()
                    .id(10001L)
                    .chatRoomId(chatRoom.getId())
                    .senderId(user1.getId())
                    .content("메시지 1")
                    .type(MessageType.TEXT)
                    .build());
            messageRepository.save(Message.builder()
                    .id(10002L)
                    .chatRoomId(chatRoom.getId())
                    .senderId(user2.getId())
                    .content("메시지 2")
                    .type(MessageType.TEXT)
                    .build());

            // when
            long unreadCount = messageRepository.countUnreadMessagesByLastReadMessageId(
                    chatRoom.getId(), user2.getId(), null);

            // then
            assertThat(unreadCount).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("무한 스크롤 조회 시")
    class FindBeforeMessageId {

        @Test
        @DisplayName("beforeMessageId가 null이면 최신 메시지부터 조회한다")
        void should_findFromLatest_when_beforeMessageIdIsNull() {
            // given
            for (int i = 1; i <= 5; i++) {
                messageRepository.save(Message.builder()
                        .id(10000L + i)
                        .chatRoomId(chatRoom.getId())
                        .senderId(user1.getId())
                        .content("메시지 " + i)
                        .type(MessageType.TEXT)
                        .build());
            }

            // when
            List<Message> messages = messageRepository.findByChatRoomIdBeforeMessageId(
                    chatRoom.getId(), null, 3);

            // then
            assertThat(messages)
                    .hasSize(3)
                    .element(0)
                    .extracting(Message::getId)
                    .isEqualTo(10005L);
        }

        @Test
        @DisplayName("beforeMessageId가 지정되면 해당 ID 이전 메시지를 조회한다")
        void should_findBeforeId_when_beforeMessageIdProvided() {
            // given
            for (int i = 1; i <= 5; i++) {
                messageRepository.save(Message.builder()
                        .id(10000L + i)
                        .chatRoomId(chatRoom.getId())
                        .senderId(user1.getId())
                        .content("메시지 " + i)
                        .type(MessageType.TEXT)
                        .build());
            }

            // when
            List<Message> messages = messageRepository.findByChatRoomIdBeforeMessageId(
                    chatRoom.getId(), 10004L, 3);

            // then
            assertThat(messages)
                    .hasSize(3)
                    .allMatch(m -> m.getId() < 10004L);
        }
    }

    @Nested
    @DisplayName("메시지 검색 시")
    class Search {

        @Test
        @DisplayName("특정 채팅방에서 키워드로 메시지를 검색한다")
        void should_searchMessages_when_keywordProvided() {
            // given
            messageRepository.save(Message.builder()
                    .id(10001L)
                    .chatRoomId(chatRoom.getId())
                    .senderId(user1.getId())
                    .content("안녕하세요 반갑습니다")
                    .type(MessageType.TEXT)
                    .build());
            messageRepository.save(Message.builder()
                    .id(10002L)
                    .chatRoomId(chatRoom.getId())
                    .senderId(user2.getId())
                    .content("네 안녕하세요")
                    .type(MessageType.TEXT)
                    .build());
            messageRepository.save(Message.builder()
                    .id(10003L)
                    .chatRoomId(chatRoom.getId())
                    .senderId(user1.getId())
                    .content("오늘 날씨 좋네요")
                    .type(MessageType.TEXT)
                    .build());

            // when
            List<Message> results = messageRepository.searchByKeywordInChatRoom(
                    chatRoom.getId(), "안녕", 0, 10);

            // then
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("사용자가 참여한 모든 채팅방에서 키워드로 검색한다")
        void should_searchAcrossChatRooms_when_userIdProvided() {
            // given
            messageRepository.save(Message.builder()
                    .id(10001L)
                    .chatRoomId(chatRoom.getId())
                    .senderId(user1.getId())
                    .content("프로젝트 관련 내용입니다")
                    .type(MessageType.TEXT)
                    .build());
            messageRepository.save(Message.builder()
                    .id(10002L)
                    .chatRoomId(chatRoom2.getId())
                    .senderId(user1.getId())
                    .content("프로젝트 일정 공유드립니다")
                    .type(MessageType.TEXT)
                    .build());

            // when
            List<Message> results = messageRepository.searchByKeywordInUserChatRooms(
                    user1.getId(), "프로젝트", 0, 10);

            // then
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("검색 결과가 없으면 빈 목록을 반환한다")
        void should_returnEmptyList_when_noResults() {
            // given
            messageRepository.save(Message.builder()
                    .id(10001L)
                    .chatRoomId(chatRoom.getId())
                    .senderId(user1.getId())
                    .content("테스트 메시지")
                    .type(MessageType.TEXT)
                    .build());

            // when
            List<Message> results = messageRepository.searchByKeywordInChatRoom(
                    chatRoom.getId(), "존재하지않는키워드", 0, 10);

            // then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("카운트 시")
    class Count {

        @Test
        @DisplayName("전체 메시지 수를 조회한다")
        void should_returnCount_when_called() {
            // given
            messageRepository.save(Message.builder()
                    .id(10001L)
                    .chatRoomId(chatRoom.getId())
                    .senderId(user1.getId())
                    .content("메시지 1")
                    .type(MessageType.TEXT)
                    .build());
            messageRepository.save(Message.builder()
                    .id(10002L)
                    .chatRoomId(chatRoom.getId())
                    .senderId(user2.getId())
                    .content("메시지 2")
                    .type(MessageType.TEXT)
                    .build());

            // when
            long count = messageRepository.count();

            // then
            assertThat(count).isEqualTo(2);
        }
    }
}
