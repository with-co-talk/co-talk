package com.cotalk.adapter.outbound.persistence;

import com.cotalk.adapter.outbound.persistence.friend.FriendRequestRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.mapper.UserMapper;
import com.cotalk.adapter.outbound.persistence.user.UserRepositoryAdapter;
import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.entity.FriendRequest.RequestStatus;
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
 * FriendRequestRepositoryAdapter 테스트.
 *
 * @author seunggu.lee
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({FriendRequestRepositoryAdapter.class, UserRepositoryAdapter.class, UserMapper.class, JpaAuditingConfig.class})
@DisplayName("FriendRequestRepositoryAdapter")
class FriendRequestRepositoryAdapterTest {

    @Autowired
    private FriendRequestRepositoryAdapter friendRequestRepository;

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
        @DisplayName("친구 요청을 저장한다")
        void should_saveFriendRequest_when_requestProvided() {
            // given
            FriendRequest request = FriendRequest.builder()
                    .id(100L)
                    .requesterId(user1.getId())
                    .receiverId(user2.getId())
                    .status(RequestStatus.PENDING)
                    .build();

            // when
            FriendRequest saved = friendRequestRepository.save(request);

            // then
            assertThat(saved.getId()).isEqualTo(100L);
            assertThat(saved.getRequesterId()).isEqualTo(user1.getId());
            assertThat(saved.getReceiverId()).isEqualTo(user2.getId());
            assertThat(saved.getStatus()).isEqualTo(RequestStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("조회 시")
    class Find {

        @Test
        @DisplayName("ID로 친구 요청을 조회한다")
        void should_findRequest_when_idProvided() {
            // given
            friendRequestRepository.save(FriendRequest.builder()
                    .id(100L)
                    .requesterId(user1.getId())
                    .receiverId(user2.getId())
                    .status(RequestStatus.PENDING)
                    .build());

            // when
            Optional<FriendRequest> found = friendRequestRepository.findById(100L);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getRequesterId()).isEqualTo(user1.getId());
        }

        @Test
        @DisplayName("수신자 ID로 대기 중인 친구 요청을 조회한다")
        void should_findPendingRequests_when_receiverIdProvided() {
            // given
            friendRequestRepository.save(FriendRequest.builder()
                    .id(100L)
                    .requesterId(user1.getId())
                    .receiverId(user2.getId())
                    .status(RequestStatus.PENDING)
                    .build());
            friendRequestRepository.save(FriendRequest.builder()
                    .id(101L)
                    .requesterId(user3.getId())
                    .receiverId(user2.getId())
                    .status(RequestStatus.PENDING)
                    .build());

            // when
            List<FriendRequest> pending = friendRequestRepository.findPendingByReceiverId(user2.getId());

            // then
            assertThat(pending).hasSize(2);
            assertThat(pending).allMatch(r -> r.getStatus() == RequestStatus.PENDING);
        }

        @Test
        @DisplayName("대기 중인 요청이 없으면 빈 목록을 반환한다")
        void should_returnEmptyList_when_noPendingRequests() {
            // when
            List<FriendRequest> pending = friendRequestRepository.findPendingByReceiverId(user2.getId());

            // then
            assertThat(pending).isEmpty();
        }
    }

    @Nested
    @DisplayName("존재 여부 확인 시")
    class Exists {

        @Test
        @DisplayName("친구 요청이 존재하면 true를 반환한다")
        void should_returnTrue_when_requestExists() {
            // given
            friendRequestRepository.save(FriendRequest.builder()
                    .id(100L)
                    .requesterId(user1.getId())
                    .receiverId(user2.getId())
                    .status(RequestStatus.PENDING)
                    .build());

            // when & then
            assertThat(friendRequestRepository.existsByRequesterIdAndReceiverId(
                    user1.getId(), user2.getId())).isTrue();
        }

        @Test
        @DisplayName("친구 요청이 존재하지 않으면 false를 반환한다")
        void should_returnFalse_when_requestNotExists() {
            // when & then
            assertThat(friendRequestRepository.existsByRequesterIdAndReceiverId(
                    user1.getId(), user2.getId())).isFalse();
        }

        @Test
        @DisplayName("특정 상태의 친구 요청이 존재하면 true를 반환한다")
        void should_returnTrue_when_requestWithStatusExists() {
            // given
            friendRequestRepository.save(FriendRequest.builder()
                    .id(100L)
                    .requesterId(user1.getId())
                    .receiverId(user2.getId())
                    .status(RequestStatus.PENDING)
                    .build());

            // when & then
            assertThat(friendRequestRepository.existsByRequesterIdAndReceiverIdAndStatus(
                    user1.getId(), user2.getId(), RequestStatus.PENDING)).isTrue();
            assertThat(friendRequestRepository.existsByRequesterIdAndReceiverIdAndStatus(
                    user1.getId(), user2.getId(), RequestStatus.ACCEPTED)).isFalse();
        }
    }

    @Nested
    @DisplayName("삭제 시")
    class Delete {

        @Test
        @DisplayName("친구 요청을 삭제한다")
        void should_deleteRequest_when_requestProvided() {
            // given
            FriendRequest request = friendRequestRepository.save(FriendRequest.builder()
                    .id(100L)
                    .requesterId(user1.getId())
                    .receiverId(user2.getId())
                    .status(RequestStatus.PENDING)
                    .build());

            // when
            friendRequestRepository.delete(request);

            // then
            assertThat(friendRequestRepository.findById(100L)).isEmpty();
        }

        @Test
        @DisplayName("사용자 ID로 모든 친구 요청을 삭제한다")
        void should_deleteAllRequests_when_userIdProvided() {
            // given
            // user1이 보낸 요청
            friendRequestRepository.save(FriendRequest.builder()
                    .id(100L)
                    .requesterId(user1.getId())
                    .receiverId(user2.getId())
                    .status(RequestStatus.PENDING)
                    .build());
            // user1이 받은 요청
            friendRequestRepository.save(FriendRequest.builder()
                    .id(101L)
                    .requesterId(user3.getId())
                    .receiverId(user1.getId())
                    .status(RequestStatus.PENDING)
                    .build());

            // when
            friendRequestRepository.deleteByUserId(user1.getId());

            // then
            assertThat(friendRequestRepository.findById(100L)).isEmpty();
            assertThat(friendRequestRepository.findById(101L)).isEmpty();
        }
    }
}
