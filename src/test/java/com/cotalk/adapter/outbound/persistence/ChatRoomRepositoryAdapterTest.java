package com.cotalk.adapter.outbound.persistence;

import com.cotalk.adapter.outbound.persistence.chatroom.ChatRoomMemberRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.chatroom.ChatRoomRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.mapper.UserMapper;
import com.cotalk.adapter.outbound.persistence.user.UserRepositoryAdapter;
import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoom.ChatRoomType;
import com.cotalk.domain.entity.ChatRoomMember;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChatRoomRepositoryAdapter 테스트.
 *
 * @author seunggu.lee
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({ChatRoomRepositoryAdapter.class, ChatRoomMemberRepositoryAdapter.class,
        UserRepositoryAdapter.class, UserMapper.class, JpaAuditingConfig.class})
@DisplayName("ChatRoomRepositoryAdapter")
class ChatRoomRepositoryAdapterTest {

    @Autowired
    private ChatRoomRepositoryAdapter chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepositoryAdapter chatRoomMemberRepository;

    @Autowired
    private UserRepositoryAdapter userRepository;

    private User user1;
    private User user2;
    private User user3;

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

        user3 = userRepository.save(User.builder()
                .id(3L)
                .email(new Email("user3@example.com"))
                .passwordHash("hash")
                .nickname("user3")
                .build());
    }

    @Nested
    @DisplayName("저장 시")
    class Save {

        @Test
        @DisplayName("채팅방을 저장하면 저장된 채팅방을 반환한다")
        void should_returnSavedChatRoom_when_chatRoomSaved() {
            // given
            ChatRoom chatRoom = ChatRoom.builder()
                    .id(100L)
                    .type(ChatRoomType.DIRECT)
                    .build();

            // when
            ChatRoom saved = chatRoomRepository.save(chatRoom);

            // then
            assertThat(saved.getId()).isEqualTo(100L);
            assertThat(saved.getType()).isEqualTo(ChatRoomType.DIRECT);
        }

        @Test
        @DisplayName("그룹 채팅방을 이름과 함께 저장한다")
        void should_saveGroupChatRoom_when_nameProvided() {
            // given
            ChatRoom chatRoom = ChatRoom.builder()
                    .id(101L)
                    .name("테스트 그룹")
                    .type(ChatRoomType.GROUP)
                    .build();

            // when
            ChatRoom saved = chatRoomRepository.save(chatRoom);

            // then
            assertThat(saved.getName()).isEqualTo("테스트 그룹");
            assertThat(saved.getType()).isEqualTo(ChatRoomType.GROUP);
        }
    }

    @Nested
    @DisplayName("조회 시")
    class Find {

        @Test
        @DisplayName("ID로 채팅방을 조회할 수 있다")
        void should_findChatRoom_when_idProvided() {
            // given
            chatRoomRepository.save(ChatRoom.builder()
                    .id(100L)
                    .type(ChatRoomType.DIRECT)
                    .build());

            // when
            Optional<ChatRoom> found = chatRoomRepository.findById(100L);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("존재하지 않는 ID 조회 시 빈 Optional을 반환한다")
        void should_returnEmpty_when_chatRoomNotFound() {
            // when
            Optional<ChatRoom> found = chatRoomRepository.findById(999L);

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("사용자 ID로 참여 중인 채팅방 목록을 조회한다")
        void should_findChatRooms_when_userIdProvided() {
            // given
            ChatRoom room1 = chatRoomRepository.save(ChatRoom.builder()
                    .id(100L)
                    .type(ChatRoomType.DIRECT)
                    .build());
            ChatRoom room2 = chatRoomRepository.save(ChatRoom.builder()
                    .id(101L)
                    .name("그룹채팅")
                    .type(ChatRoomType.GROUP)
                    .build());

            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .id(1000L)
                    .chatRoomId(room1.getId())
                    .userId(user1.getId())
                    .build());
            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .id(1001L)
                    .chatRoomId(room2.getId())
                    .userId(user1.getId())
                    .build());
            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .id(1002L)
                    .chatRoomId(room1.getId())
                    .userId(user2.getId())
                    .build());

            // when
            List<ChatRoom> rooms = chatRoomRepository.findByUserId(user1.getId());

            // then
            assertThat(rooms).hasSize(2);
        }

        @Test
        @DisplayName("참여 중인 채팅방이 없으면 빈 목록을 반환한다")
        void should_returnEmptyList_when_noParticipation() {
            // when
            List<ChatRoom> rooms = chatRoomRepository.findByUserId(user3.getId());

            // then
            assertThat(rooms).isEmpty();
        }
    }

    @Nested
    @DisplayName("1:1 채팅방 조회 시")
    class FindDirectChatRoom {

        @Test
        @DisplayName("두 사용자 간 1:1 채팅방을 조회한다")
        void should_findDirectChatRoom_when_userIdsProvided() {
            // given
            ChatRoom directRoom = chatRoomRepository.save(ChatRoom.builder()
                    .id(100L)
                    .type(ChatRoomType.DIRECT)
                    .build());

            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .id(1000L)
                    .chatRoomId(directRoom.getId())
                    .userId(user1.getId())
                    .build());
            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .id(1001L)
                    .chatRoomId(directRoom.getId())
                    .userId(user2.getId())
                    .build());

            // when
            Optional<ChatRoom> found = chatRoomRepository.findDirectChatRoomByUserIds(
                    user1.getId(), user2.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("순서를 바꿔도 같은 채팅방을 조회한다")
        void should_findSameChatRoom_when_userIdsReversed() {
            // given
            ChatRoom directRoom = chatRoomRepository.save(ChatRoom.builder()
                    .id(100L)
                    .type(ChatRoomType.DIRECT)
                    .build());

            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .id(1000L)
                    .chatRoomId(directRoom.getId())
                    .userId(user1.getId())
                    .build());
            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .id(1001L)
                    .chatRoomId(directRoom.getId())
                    .userId(user2.getId())
                    .build());

            // when
            Optional<ChatRoom> found1 = chatRoomRepository.findDirectChatRoomByUserIds(
                    user1.getId(), user2.getId());
            Optional<ChatRoom> found2 = chatRoomRepository.findDirectChatRoomByUserIds(
                    user2.getId(), user1.getId());

            // then
            assertThat(found1).isPresent();
            assertThat(found2).isPresent();
            assertThat(found1.get().getId()).isEqualTo(found2.get().getId());
        }

        @Test
        @DisplayName("1:1 채팅방이 없으면 빈 Optional을 반환한다")
        void should_returnEmpty_when_noDirectChatRoom() {
            // when
            Optional<ChatRoom> found = chatRoomRepository.findDirectChatRoomByUserIds(
                    user1.getId(), user3.getId());

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("삭제 시")
    class Delete {

        @Test
        @DisplayName("채팅방을 삭제한다")
        void should_deleteChatRoom_when_chatRoomProvided() {
            // given
            ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.builder()
                    .id(100L)
                    .type(ChatRoomType.DIRECT)
                    .build());

            // when
            chatRoomRepository.delete(chatRoom);

            // then
            assertThat(chatRoomRepository.findById(100L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("카운트 시")
    class Count {

        @Test
        @DisplayName("전체 채팅방 수를 조회한다")
        void should_returnCount_when_called() {
            // given
            chatRoomRepository.save(ChatRoom.builder()
                    .id(100L)
                    .type(ChatRoomType.DIRECT)
                    .build());
            chatRoomRepository.save(ChatRoom.builder()
                    .id(101L)
                    .name("그룹")
                    .type(ChatRoomType.GROUP)
                    .build());

            // when
            long count = chatRoomRepository.count();

            // then
            assertThat(count).isEqualTo(2);
        }
    }
}
