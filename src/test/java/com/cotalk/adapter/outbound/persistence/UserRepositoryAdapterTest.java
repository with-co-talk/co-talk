package com.cotalk.adapter.outbound.persistence;

import com.cotalk.adapter.outbound.persistence.user.UserRepositoryAdapter;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.entity.User.OAuthProvider;
import com.cotalk.domain.entity.User.UserStatus;
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
 * UserRepositoryAdapter 테스트.
 *
 * @author seunggu.lee
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({UserRepositoryAdapter.class, JpaAuditingConfig.class})
@DisplayName("UserRepositoryAdapter")
class UserRepositoryAdapterTest {

    @Autowired
    private UserRepositoryAdapter userRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(User.builder()
                .id(1L)
                .email("user1@example.com")
                .passwordHash("hash1")
                .nickname("user1")
                .status(UserStatus.ACTIVE)
                .build());

        user2 = userRepository.save(User.builder()
                .id(2L)
                .email("user2@example.com")
                .passwordHash("hash2")
                .nickname("user2")
                .status(UserStatus.ACTIVE)
                .build());
    }

    @Nested
    @DisplayName("저장 시")
    class Save {

        @Test
        @DisplayName("사용자를 저장한다")
        void should_saveUser_when_userProvided() {
            // given
            User newUser = User.builder()
                    .id(100L)
                    .email("new@example.com")
                    .passwordHash("hash")
                    .nickname("newuser")
                    .build();

            // when
            User saved = userRepository.save(newUser);

            // then
            assertThat(saved.getId()).isEqualTo(100L);
            assertThat(saved.getEmail()).isEqualTo("new@example.com");
            assertThat(saved.getNickname()).isEqualTo("newuser");
        }

        @Test
        @DisplayName("OAuth 사용자를 저장한다")
        void should_saveOAuthUser_when_oauthProvided() {
            // given
            User oauthUser = User.builder()
                    .id(101L)
                    .email("oauth@example.com")
                    .nickname("oauthuser")
                    .oauthProvider(OAuthProvider.GOOGLE)
                    .oauthId("google-123")
                    .build();

            // when
            User saved = userRepository.save(oauthUser);

            // then
            assertThat(saved.getOauthProvider()).isEqualTo(OAuthProvider.GOOGLE);
            assertThat(saved.getOauthId()).isEqualTo("google-123");
        }
    }

    @Nested
    @DisplayName("조회 시")
    class Find {

        @Test
        @DisplayName("ID로 사용자를 조회한다")
        void should_findUser_when_idProvided() {
            // when
            Optional<User> found = userRepository.findById(user1.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("user1@example.com");
        }

        @Test
        @DisplayName("이메일로 사용자를 조회한다")
        void should_findUser_when_emailProvided() {
            // when
            Optional<User> found = userRepository.findByEmail("user1@example.com");

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(user1.getId());
        }

        @Test
        @DisplayName("OAuth 제공자와 OAuth ID로 사용자를 조회한다")
        void should_findUser_when_oauthProviderAndIdProvided() {
            // given
            userRepository.save(User.builder()
                    .id(100L)
                    .email("oauth@example.com")
                    .nickname("oauthuser")
                    .oauthProvider(OAuthProvider.KAKAO)
                    .oauthId("kakao-123")
                    .build());

            // when
            Optional<User> found = userRepository.findByOAuthProviderAndOAuthId(
                    OAuthProvider.KAKAO, "kakao-123");

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("oauth@example.com");
        }

        @Test
        @DisplayName("닉네임으로 사용자를 검색한다")
        void should_findUsers_when_nicknameContaining() {
            // when
            List<User> found = userRepository.findByNicknameContaining("user");

            // then
            assertThat(found).hasSize(2);
        }

        @Test
        @DisplayName("모든 사용자를 조회한다")
        void should_findAllUsers() {
            // when
            List<User> users = userRepository.findAll();

            // then
            assertThat(users).hasSize(2);
        }

        @Test
        @DisplayName("상태로 사용자를 조회한다")
        void should_findUsers_when_statusProvided() {
            // given
            userRepository.save(User.builder()
                    .id(100L)
                    .email("inactive@example.com")
                    .passwordHash("hash")
                    .nickname("inactive")
                    .status(UserStatus.INACTIVE)
                    .build());

            // when
            List<User> activeUsers = userRepository.findByStatus(UserStatus.ACTIVE);

            // then
            assertThat(activeUsers).hasSize(2);
        }

        @Test
        @DisplayName("여러 ID로 사용자를 조회한다")
        void should_findUsers_when_multipleIdsProvided() {
            // when
            List<User> users = userRepository.findAllById(List.of(user1.getId(), user2.getId()));

            // then
            assertThat(users).hasSize(2);
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 빈 Optional을 반환한다")
        void should_returnEmpty_when_userNotExists() {
            // when
            Optional<User> found = userRepository.findById(999L);

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("존재 여부 확인 시")
    class Exists {

        @Test
        @DisplayName("이메일이 존재하면 true를 반환한다")
        void should_returnTrue_when_emailExists() {
            // when & then
            assertThat(userRepository.existsByEmail("user1@example.com")).isTrue();
        }

        @Test
        @DisplayName("이메일이 존재하지 않으면 false를 반환한다")
        void should_returnFalse_when_emailNotExists() {
            // when & then
            assertThat(userRepository.existsByEmail("notexist@example.com")).isFalse();
        }

        @Test
        @DisplayName("닉네임이 존재하면 true를 반환한다")
        void should_returnTrue_when_nicknameExists() {
            // when & then
            assertThat(userRepository.existsByNickname("user1")).isTrue();
        }

        @Test
        @DisplayName("닉네임이 존재하지 않으면 false를 반환한다")
        void should_returnFalse_when_nicknameNotExists() {
            // when & then
            assertThat(userRepository.existsByNickname("notexist")).isFalse();
        }
    }

    @Nested
    @DisplayName("개수 조회 시")
    class Count {

        @Test
        @DisplayName("전체 사용자 수를 반환한다")
        void should_countAllUsers() {
            // when
            long count = userRepository.count();

            // then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("상태별 사용자 수를 반환한다")
        void should_countUsersByStatus() {
            // given
            userRepository.save(User.builder()
                    .id(100L)
                    .email("inactive@example.com")
                    .passwordHash("hash")
                    .nickname("inactive")
                    .status(UserStatus.INACTIVE)
                    .build());

            // when
            long activeCount = userRepository.countByStatus(UserStatus.ACTIVE);
            long inactiveCount = userRepository.countByStatus(UserStatus.INACTIVE);

            // then
            assertThat(activeCount).isEqualTo(2);
            assertThat(inactiveCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("삭제 시")
    class Delete {

        @Test
        @DisplayName("사용자를 삭제한다")
        void should_deleteUser_when_userProvided() {
            // when
            userRepository.delete(user1);

            // then
            assertThat(userRepository.findById(user1.getId())).isEmpty();
        }
    }
}
