package com.cotalk.adapter.outbound.persistence;

import com.cotalk.adapter.outbound.persistence.refreshtoken.RefreshTokenRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.mapper.UserMapper;
import com.cotalk.adapter.outbound.persistence.user.UserRepositoryAdapter;
import com.cotalk.domain.entity.RefreshToken;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RefreshTokenRepositoryAdapter 테스트.
 *
 * @author seunggu.lee
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({RefreshTokenRepositoryAdapter.class, UserRepositoryAdapter.class, UserMapper.class, JpaAuditingConfig.class})
@DisplayName("RefreshTokenRepositoryAdapter")
class RefreshTokenRepositoryAdapterTest {

    @Autowired
    private RefreshTokenRepositoryAdapter refreshTokenRepository;

    @Autowired
    private UserRepositoryAdapter userRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .id(1L)
                .email(new Email("user@example.com"))
                .passwordHash("hash")
                .nickname("user")
                .build());
    }

    @Nested
    @DisplayName("저장 시")
    class Save {

        @Test
        @DisplayName("Refresh Token을 저장한다")
        void should_saveRefreshToken_when_tokenProvided() {
            // given
            RefreshToken token = RefreshToken.builder()
                    .id(100L)
                    .userId(user.getId())
                    .token("refresh-token-123")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            // when
            RefreshToken saved = refreshTokenRepository.save(token);

            // then
            assertThat(saved.getId()).isEqualTo(100L);
            assertThat(saved.getUserId()).isEqualTo(user.getId());
            assertThat(saved.getToken()).isEqualTo("refresh-token-123");
            assertThat(saved.isRevoked()).isFalse();
        }
    }

    @Nested
    @DisplayName("조회 시")
    class Find {

        @Test
        @DisplayName("토큰 값으로 조회한다")
        void should_findToken_when_tokenValueProvided() {
            // given
            refreshTokenRepository.save(RefreshToken.builder()
                    .id(100L)
                    .userId(user.getId())
                    .token("refresh-token-123")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build());

            // when
            Optional<RefreshToken> found = refreshTokenRepository.findByToken("refresh-token-123");

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getUserId()).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("존재하지 않는 토큰은 빈 Optional을 반환한다")
        void should_returnEmpty_when_tokenNotExists() {
            // when
            Optional<RefreshToken> found = refreshTokenRepository.findByToken("nonexistent");

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("사용자의 유효한 토큰을 조회한다")
        void should_findValidToken_when_userIdProvided() {
            // given
            refreshTokenRepository.save(RefreshToken.builder()
                    .id(100L)
                    .userId(user.getId())
                    .token("valid-token")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .revoked(false)
                    .build());

            // when
            Optional<RefreshToken> found = refreshTokenRepository.findByUserIdAndRevokedFalse(user.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getToken()).isEqualTo("valid-token");
        }

        @Test
        @DisplayName("폐기된 토큰은 조회되지 않는다")
        void should_notFindToken_when_revoked() {
            // given
            refreshTokenRepository.save(RefreshToken.builder()
                    .id(100L)
                    .userId(user.getId())
                    .token("revoked-token")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .revoked(true)
                    .build());

            // when
            Optional<RefreshToken> found = refreshTokenRepository.findByUserIdAndRevokedFalse(user.getId());

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("폐기 시")
    class Revoke {

        @Test
        @DisplayName("사용자의 모든 토큰을 폐기한다")
        void should_revokeAllTokens_when_userIdProvided() {
            // given
            refreshTokenRepository.save(RefreshToken.builder()
                    .id(100L)
                    .userId(user.getId())
                    .token("token-1")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .revoked(false)
                    .build());
            refreshTokenRepository.save(RefreshToken.builder()
                    .id(101L)
                    .userId(user.getId())
                    .token("token-2")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .revoked(false)
                    .build());

            entityManager.flush();
            entityManager.clear();

            // when
            refreshTokenRepository.revokeAllByUserId(user.getId());

            entityManager.flush();
            entityManager.clear();

            // then
            Optional<RefreshToken> found = refreshTokenRepository.findByUserIdAndRevokedFalse(user.getId());
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("삭제 시")
    class Delete {

        @Test
        @DisplayName("만료된 토큰을 삭제한다")
        void should_deleteExpiredTokens() {
            // given
            refreshTokenRepository.save(RefreshToken.builder()
                    .id(100L)
                    .userId(user.getId())
                    .token("expired-token")
                    .expiresAt(LocalDateTime.now().minusDays(1))
                    .build());
            refreshTokenRepository.save(RefreshToken.builder()
                    .id(101L)
                    .userId(user.getId())
                    .token("valid-token")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build());

            entityManager.flush();
            entityManager.clear();

            // when
            int deleted = refreshTokenRepository.deleteExpiredTokens();

            // then
            assertThat(deleted).isEqualTo(1);
            assertThat(refreshTokenRepository.findByToken("expired-token")).isEmpty();
            assertThat(refreshTokenRepository.findByToken("valid-token")).isPresent();
        }

        @Test
        @DisplayName("사용자의 모든 토큰을 물리적으로 삭제한다")
        void should_deleteAllTokens_when_userIdProvided() {
            // given
            refreshTokenRepository.save(RefreshToken.builder()
                    .id(100L)
                    .userId(user.getId())
                    .token("token-1")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .revoked(false)
                    .build());
            refreshTokenRepository.save(RefreshToken.builder()
                    .id(101L)
                    .userId(user.getId())
                    .token("token-2")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .revoked(true)
                    .build());

            entityManager.flush();
            entityManager.clear();

            // when
            refreshTokenRepository.deleteByUserId(user.getId());

            entityManager.flush();
            entityManager.clear();

            // then: revoke 여부와 무관하게 행 자체가 사라져야 한다
            assertThat(refreshTokenRepository.findByToken("token-1")).isEmpty();
            assertThat(refreshTokenRepository.findByToken("token-2")).isEmpty();
        }

        @Test
        @DisplayName("만료된 토큰이 없으면 0을 반환한다")
        void should_returnZero_when_noExpiredTokens() {
            // given
            refreshTokenRepository.save(RefreshToken.builder()
                    .id(100L)
                    .userId(user.getId())
                    .token("valid-token")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build());

            entityManager.flush();
            entityManager.clear();

            // when
            int deleted = refreshTokenRepository.deleteExpiredTokens();

            // then
            assertThat(deleted).isEqualTo(0);
        }
    }
}
