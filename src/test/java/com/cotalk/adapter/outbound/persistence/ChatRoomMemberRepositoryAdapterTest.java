package com.cotalk.adapter.outbound.persistence;

import com.cotalk.adapter.outbound.persistence.chatroom.ChatRoomMemberRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.chatroom.ChatRoomRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.user.UserRepositoryAdapter;
import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoom.ChatRoomType;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.ChatRoomMember.MemberRole;
import com.cotalk.domain.entity.User;
import com.cotalk.infrastructure.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
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
 * ChatRoomMemberRepositoryAdapter 테스트.
 *
 * @author seunggu.lee
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({ChatRoomMemberRepositoryAdapter.class, ChatRoomRepositoryAdapter.class,
        UserRepositoryAdapter.class, JpaAuditingConfig.class})
@DisplayName("ChatRoomMemberRepositoryAdapter")
class ChatRoomMemberRepositoryAdapterTest {

    @Autowired
    private ChatRoomMemberRepositoryAdapter chatRoomMemberRepository;

    @Autowired
    private ChatRoomRepositoryAdapter chatRoomRepository;

    @Autowired
    private UserRepositoryAdapter userRepository;

    @Autowired
    private EntityManager entityManager;

    private User user1;
    private User user2;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(User.builder()
                .id(1L)
                .email("user1@example.com")
                .passwordHash("hash")
                .nickname("user1")
                .build());

        user2 = userRepository.save(User.builder()
                .id(2L)
                .email("user2@example.com")
                .passwordHash("hash")
                .nickname("user2")
                .build());

        chatRoom = chatRoomRepository.save(ChatRoom.builder()
                .id(100L)
                .type(ChatRoomType.DIRECT)
                .build());
    }

    @Nested
    @DisplayName("저장 시")
    class Save {

        @Test
        @DisplayName("채팅방 멤버를 저장한다")
        void should_saveMember_when_memberProvided() {
            // given
            ChatRoomMember member = ChatRoomMember.builder()
                    .id(1000L)
                    .chatRoomId(chatRoom.getId())
                    .userId(user1.getId())
                    .role(MemberRole.ADMIN)
                    .build();

            // when
            ChatRoomMember saved = chatRoomMemberRepository.save(member);

            // then
            assertThat(saved.getId()).isEqualTo(1000L);
            assertThat(saved.getChatRoomId()).isEqualTo(chatRoom.getId());
            assertThat(saved.getUserId()).isEqualTo(user1.getId());
            assertThat(saved.getRole()).isEqualTo(MemberRole.ADMIN);
        }

        @Test
        @DisplayName("여러 채팅방 멤버를 일괄 저장한다")
        void should_saveAllMembers_when_membersProvided() {
            // given
            List<ChatRoomMember> members = List.of(
                    ChatRoomMember.builder()
                            .id(1000L)
                            .chatRoomId(chatRoom.getId())
                            .userId(user1.getId())
                            .role(MemberRole.ADMIN)
                            .build(),
                    ChatRoomMember.builder()
                            .id(1001L)
                            .chatRoomId(chatRoom.getId())
                            .userId(user2.getId())
                            .role(MemberRole.MEMBER)
                            .build()
            );

            // when
            List<ChatRoomMember> saved = chatRoomMemberRepository.saveAll(members);

            // then
            assertThat(saved).hasSize(2);
        }
    }

    @Nested
    @DisplayName("조회 시")
    class Find {

        @Test
        @DisplayName("채팅방 ID와 사용자 ID로 멤버를 조회한다")
        void should_findMember_when_chatRoomIdAndUserIdProvided() {
            // given
            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .id(1000L)
                    .chatRoomId(chatRoom.getId())
                    .userId(user1.getId())
                    .build());

            // when
            Optional<ChatRoomMember> found = chatRoomMemberRepository
                    .findByChatRoomIdAndUserId(chatRoom.getId(), user1.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getUserId()).isEqualTo(user1.getId());
        }

        @Test
        @DisplayName("채팅방 ID로 모든 멤버를 조회한다")
        void should_findAllMembers_when_chatRoomIdProvided() {
            // given
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

            // when
            List<ChatRoomMember> members = chatRoomMemberRepository
                    .findByChatRoomId(chatRoom.getId());

            // then
            assertThat(members).hasSize(2);
        }

        @Test
        @DisplayName("사용자 ID로 참여 중인 채팅방 멤버 목록을 조회한다")
        void should_findMemberships_when_userIdProvided() {
            // given
            ChatRoom room2 = chatRoomRepository.save(ChatRoom.builder()
                    .id(101L)
                    .name("그룹")
                    .type(ChatRoomType.GROUP)
                    .build());

            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .id(1000L)
                    .chatRoomId(chatRoom.getId())
                    .userId(user1.getId())
                    .build());
            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .id(1001L)
                    .chatRoomId(room2.getId())
                    .userId(user1.getId())
                    .build());

            // when
            List<ChatRoomMember> memberships = chatRoomMemberRepository
                    .findByUserId(user1.getId());

            // then
            assertThat(memberships).hasSize(2);
        }
    }

    @Nested
    @DisplayName("존재 여부 확인 시")
    class Exists {

        @Test
        @DisplayName("채팅방 멤버가 존재하면 true를 반환한다")
        void should_returnTrue_when_memberExists() {
            // given
            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .id(1000L)
                    .chatRoomId(chatRoom.getId())
                    .userId(user1.getId())
                    .build());

            // when & then
            assertThat(chatRoomMemberRepository.existsByChatRoomIdAndUserId(
                    chatRoom.getId(), user1.getId())).isTrue();
        }

        @Test
        @DisplayName("채팅방 멤버가 존재하지 않으면 false를 반환한다")
        void should_returnFalse_when_memberNotExists() {
            // when & then
            assertThat(chatRoomMemberRepository.existsByChatRoomIdAndUserId(
                    chatRoom.getId(), user2.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("삭제 시")
    class Delete {

        @Test
        @DisplayName("채팅방 멤버를 삭제한다")
        void should_deleteMember_when_memberProvided() {
            // given
            ChatRoomMember member = chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .id(1000L)
                    .chatRoomId(chatRoom.getId())
                    .userId(user1.getId())
                    .build());

            // when
            chatRoomMemberRepository.delete(member);

            // then
            assertThat(chatRoomMemberRepository.findByChatRoomIdAndUserId(
                    chatRoom.getId(), user1.getId())).isEmpty();
        }

        @Test
        @DisplayName("사용자 ID로 모든 멤버 정보를 삭제한다")
        void should_deleteAllMemberships_when_userIdProvided() {
            // given
            ChatRoom room2 = chatRoomRepository.save(ChatRoom.builder()
                    .id(101L)
                    .name("그룹")
                    .type(ChatRoomType.GROUP)
                    .build());

            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .id(1000L)
                    .chatRoomId(chatRoom.getId())
                    .userId(user1.getId())
                    .build());
            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .id(1001L)
                    .chatRoomId(room2.getId())
                    .userId(user1.getId())
                    .build());

            // when
            chatRoomMemberRepository.deleteByUserId(user1.getId());

            // then
            assertThat(chatRoomMemberRepository.findByUserId(user1.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("마지막 읽은 시간 업데이트 시")
    class UpdateLastReadAt {

        @Test
        @DisplayName("기존 시간보다 새로운 시간이면 업데이트한다")
        void should_updateLastReadAt_when_newerTime() {
            // given
            LocalDateTime oldTime = LocalDateTime.of(2024, 1, 1, 10, 0);
            LocalDateTime newTime = LocalDateTime.of(2024, 1, 1, 12, 0);

            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .id(1000L)
                    .chatRoomId(chatRoom.getId())
                    .userId(user1.getId())
                    .lastReadAt(oldTime)
                    .build());
            entityManager.flush();
            entityManager.clear();

            // when
            int updated = chatRoomMemberRepository.updateLastReadAtIfNewer(
                    chatRoom.getId(), user1.getId(), newTime);
            entityManager.flush();
            entityManager.clear();

            // then
            assertThat(updated).isEqualTo(1);
            Optional<ChatRoomMember> found = chatRoomMemberRepository
                    .findByChatRoomIdAndUserId(chatRoom.getId(), user1.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getLastReadAt()).isEqualTo(newTime);
        }

        @Test
        @DisplayName("기존 시간보다 이전 시간이면 업데이트하지 않는다")
        void should_notUpdate_when_olderTime() {
            // given
            LocalDateTime existingTime = LocalDateTime.of(2024, 1, 1, 12, 0);
            LocalDateTime olderTime = LocalDateTime.of(2024, 1, 1, 10, 0);

            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .id(1000L)
                    .chatRoomId(chatRoom.getId())
                    .userId(user1.getId())
                    .lastReadAt(existingTime)
                    .build());

            // when
            int updated = chatRoomMemberRepository.updateLastReadAtIfNewer(
                    chatRoom.getId(), user1.getId(), olderTime);

            // then
            assertThat(updated).isEqualTo(0);
            Optional<ChatRoomMember> found = chatRoomMemberRepository
                    .findByChatRoomIdAndUserId(chatRoom.getId(), user1.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getLastReadAt()).isEqualTo(existingTime);
        }

        @Test
        @DisplayName("기존 시간이 null이면 새 시간으로 업데이트한다")
        void should_updateLastReadAt_when_existingTimeIsNull() {
            // given
            LocalDateTime newTime = LocalDateTime.of(2024, 1, 1, 12, 0);

            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .id(1000L)
                    .chatRoomId(chatRoom.getId())
                    .userId(user1.getId())
                    .lastReadAt(null)
                    .build());
            entityManager.flush();
            entityManager.clear();

            // when
            int updated = chatRoomMemberRepository.updateLastReadAtIfNewer(
                    chatRoom.getId(), user1.getId(), newTime);
            entityManager.flush();
            entityManager.clear();

            // then
            assertThat(updated).isEqualTo(1);
            Optional<ChatRoomMember> found = chatRoomMemberRepository
                    .findByChatRoomIdAndUserId(chatRoom.getId(), user1.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getLastReadAt()).isEqualTo(newTime);
        }
    }
}
