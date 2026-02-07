package com.cotalk.adapter.outbound.persistence;

import com.cotalk.adapter.outbound.persistence.friend.FriendRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.mapper.UserMapper;
import com.cotalk.adapter.outbound.persistence.user.UserRepositoryAdapter;
import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.entity.Friend.FriendStatus;
import com.cotalk.domain.entity.User;
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
 * FriendRepositoryAdapter 테스트.
 *
 * @author seunggu.lee
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({FriendRepositoryAdapter.class, UserRepositoryAdapter.class, UserMapper.class, JpaAuditingConfig.class})
@DisplayName("FriendRepositoryAdapter")
class FriendRepositoryAdapterTest {

    @Autowired
    private FriendRepositoryAdapter friendRepository;

    @Autowired
    private UserRepositoryAdapter userRepository;

    private User user1;
    private User user2;
    private User user3;

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

        user3 = userRepository.save(User.builder()
                .id(3L)
                .email("user3@example.com")
                .passwordHash("hash")
                .nickname("user3")
                .build());
    }

    @Nested
    @DisplayName("저장 시")
    class Save {

        @Test
        @DisplayName("친구 관계를 저장한다")
        void should_saveFriend_when_friendProvided() {
            // given
            Friend friend = Friend.builder()
                    .id(100L)
                    .userId(user1.getId())
                    .friendId(user2.getId())
                    .status(FriendStatus.ACCEPTED)
                    .build();

            // when
            Friend saved = friendRepository.save(friend);

            // then
            assertThat(saved.getId()).isEqualTo(100L);
            assertThat(saved.getUserId()).isEqualTo(user1.getId());
            assertThat(saved.getFriendId()).isEqualTo(user2.getId());
            assertThat(saved.getStatus()).isEqualTo(FriendStatus.ACCEPTED);
        }
    }

    @Nested
    @DisplayName("조회 시")
    class Find {

        @Test
        @DisplayName("ID로 친구 관계를 조회한다")
        void should_findFriend_when_idProvided() {
            // given
            friendRepository.save(Friend.builder()
                    .id(100L)
                    .userId(user1.getId())
                    .friendId(user2.getId())
                    .status(FriendStatus.ACCEPTED)
                    .build());

            // when
            Optional<Friend> found = friendRepository.findById(100L);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getUserId()).isEqualTo(user1.getId());
        }

        @Test
        @DisplayName("사용자 ID와 친구 ID로 친구 관계를 조회한다")
        void should_findFriend_when_userIdAndFriendIdProvided() {
            // given
            friendRepository.save(Friend.builder()
                    .id(100L)
                    .userId(user1.getId())
                    .friendId(user2.getId())
                    .status(FriendStatus.ACCEPTED)
                    .build());

            // when
            Optional<Friend> found = friendRepository.findByUserIdAndFriendId(
                    user1.getId(), user2.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("수락된 친구 목록만 조회한다")
        void should_findAcceptedFriends_when_userIdProvided() {
            // given
            friendRepository.save(Friend.builder()
                    .id(100L)
                    .userId(user1.getId())
                    .friendId(user2.getId())
                    .status(FriendStatus.ACCEPTED)
                    .build());
            friendRepository.save(Friend.builder()
                    .id(101L)
                    .userId(user1.getId())
                    .friendId(user3.getId())
                    .status(FriendStatus.PENDING)
                    .build());

            // when
            List<Friend> friends = friendRepository.findAcceptedFriendsByUserId(user1.getId());

            // then
            assertThat(friends).hasSize(1);
            assertThat(friends.get(0).getFriendId()).isEqualTo(user2.getId());
        }

        @Test
        @DisplayName("친구가 없으면 빈 목록을 반환한다")
        void should_returnEmptyList_when_noFriends() {
            // when
            List<Friend> friends = friendRepository.findAcceptedFriendsByUserId(user1.getId());

            // then
            assertThat(friends).isEmpty();
        }

        @Test
        @DisplayName("친구 User 정보와 함께 조회한다")
        void should_findFriendsWithUserData_when_userIdProvided() {
            // given
            friendRepository.save(Friend.builder()
                    .id(100L)
                    .userId(user1.getId())
                    .friendId(user2.getId())
                    .status(FriendStatus.ACCEPTED)
                    .build());
            friendRepository.save(Friend.builder()
                    .id(101L)
                    .userId(user1.getId())
                    .friendId(user3.getId())
                    .status(FriendStatus.ACCEPTED)
                    .build());

            // when
            List<User> friendUsers = friendRepository.findAcceptedFriendsWithUserData(user1.getId());

            // then
            assertThat(friendUsers).hasSize(2);
            assertThat(friendUsers).extracting(User::getNickname)
                    .containsExactlyInAnyOrder("user2", "user3");
        }
    }

    @Nested
    @DisplayName("존재 여부 확인 시")
    class Exists {

        @Test
        @DisplayName("친구 관계가 존재하면 true를 반환한다")
        void should_returnTrue_when_friendExists() {
            // given
            friendRepository.save(Friend.builder()
                    .id(100L)
                    .userId(user1.getId())
                    .friendId(user2.getId())
                    .status(FriendStatus.ACCEPTED)
                    .build());

            // when & then
            assertThat(friendRepository.existsByUserIdAndFriendId(
                    user1.getId(), user2.getId())).isTrue();
        }

        @Test
        @DisplayName("친구 관계가 존재하지 않으면 false를 반환한다")
        void should_returnFalse_when_friendNotExists() {
            // when & then
            assertThat(friendRepository.existsByUserIdAndFriendId(
                    user1.getId(), user2.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("삭제 시")
    class Delete {

        @Test
        @DisplayName("친구 관계를 삭제한다")
        void should_deleteFriend_when_friendProvided() {
            // given
            Friend friend = friendRepository.save(Friend.builder()
                    .id(100L)
                    .userId(user1.getId())
                    .friendId(user2.getId())
                    .status(FriendStatus.ACCEPTED)
                    .build());

            // when
            friendRepository.delete(friend);

            // then
            assertThat(friendRepository.findById(100L)).isEmpty();
        }

        @Test
        @DisplayName("사용자 ID로 모든 친구 관계를 삭제한다")
        void should_deleteAllFriendships_when_userIdProvided() {
            // given
            // user1 -> user2 친구
            friendRepository.save(Friend.builder()
                    .id(100L)
                    .userId(user1.getId())
                    .friendId(user2.getId())
                    .status(FriendStatus.ACCEPTED)
                    .build());
            // user2 -> user1 친구 (양방향)
            friendRepository.save(Friend.builder()
                    .id(101L)
                    .userId(user2.getId())
                    .friendId(user1.getId())
                    .status(FriendStatus.ACCEPTED)
                    .build());
            // user1 -> user3 친구
            friendRepository.save(Friend.builder()
                    .id(102L)
                    .userId(user1.getId())
                    .friendId(user3.getId())
                    .status(FriendStatus.ACCEPTED)
                    .build());

            // when
            friendRepository.deleteByUserId(user1.getId());

            // then
            assertThat(friendRepository.findAcceptedFriendsByUserId(user1.getId())).isEmpty();
            // user2 -> user1 관계도 삭제되어야 함
            assertThat(friendRepository.findByUserIdAndFriendId(user2.getId(), user1.getId())).isEmpty();
        }
    }
}
