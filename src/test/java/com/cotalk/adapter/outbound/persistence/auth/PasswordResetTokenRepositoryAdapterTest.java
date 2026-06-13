package com.cotalk.adapter.outbound.persistence.auth;

import com.cotalk.domain.entity.PasswordResetToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PasswordResetTokenRepositoryAdapter 단위 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetTokenRepositoryAdapter")
class PasswordResetTokenRepositoryAdapterTest {

    @Mock
    private PasswordResetTokenJpaRepository jpaRepository;

    @InjectMocks
    private PasswordResetTokenRepositoryAdapter adapter;

    private PasswordResetToken token;

    @BeforeEach
    void setUp() {
        token = PasswordResetToken.builder()
                .id(1L)
                .userId(100L)
                .token("reset-token-123")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    @Nested
    @DisplayName("save 메서드")
    class SaveMethod {

        @Test
        @DisplayName("토큰을 저장하고 반환한다")
        void should_saveToken_when_tokenProvided() {
            // given
            when(jpaRepository.save(token)).thenReturn(token);

            // when
            PasswordResetToken result = adapter.save(token);

            // then
            assertThat(result).isEqualTo(token);
            verify(jpaRepository).save(token);
        }
    }

    @Nested
    @DisplayName("findByToken 메서드")
    class FindByTokenMethod {

        @Test
        @DisplayName("토큰 값으로 조회한다")
        void should_findToken_when_tokenExists() {
            // given
            String tokenValue = "reset-token-123";
            when(jpaRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));

            // when
            Optional<PasswordResetToken> result = adapter.findByToken(tokenValue);

            // then
            assertThat(result).contains(token);
            verify(jpaRepository).findByToken(tokenValue);
        }

        @Test
        @DisplayName("존재하지 않는 토큰은 빈 Optional 반환")
        void should_returnEmpty_when_tokenNotExists() {
            // given
            String tokenValue = "nonexistent";
            when(jpaRepository.findByToken(tokenValue)).thenReturn(Optional.empty());

            // when
            Optional<PasswordResetToken> result = adapter.findByToken(tokenValue);

            // then
            assertThat(result).isEmpty();
            verify(jpaRepository).findByToken(tokenValue);
        }
    }

    @Nested
    @DisplayName("deleteByUserId 메서드")
    class DeleteByUserIdMethod {

        @Test
        @DisplayName("사용자 ID로 토큰을 삭제한다")
        void should_deleteToken_when_userIdProvided() {
            // given
            Long userId = 100L;

            // when
            adapter.deleteByUserId(userId);

            // then
            verify(jpaRepository).deleteByUserId(userId);
        }
    }

    @Nested
    @DisplayName("deleteExpiredTokens 메서드")
    class DeleteExpiredTokensMethod {

        @Test
        @DisplayName("만료된 토큰들을 삭제한다")
        void should_deleteExpiredTokens() {
            // given
            ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

            // when
            adapter.deleteExpiredTokens();

            // then
            verify(jpaRepository).deleteExpiredTokens(timeCaptor.capture());
            assertThat(timeCaptor.getValue()).isBeforeOrEqualTo(LocalDateTime.now());
        }
    }

    @Nested
    @DisplayName("incrementFailedAttemptsAndGet 메서드")
    class IncrementFailedAttemptsAndGetMethod {

        @Test
        @DisplayName("원자적으로 증가시킨 뒤 갱신된 실패 횟수를 반환한다")
        void should_incrementAtomically_andReturnUpdatedCount() {
            // given
            when(jpaRepository.findFailedAttemptsById(1L)).thenReturn(Optional.of(3));

            // when
            int result = adapter.incrementFailedAttemptsAndGet(1L);

            // then: 원자적 UPDATE 후 최신 값 재조회
            verify(jpaRepository).incrementFailedAttempts(1L);
            verify(jpaRepository).findFailedAttemptsById(1L);
            assertThat(result).isEqualTo(3);
        }

        @Test
        @DisplayName("토큰이 없으면 0을 반환한다")
        void should_returnZero_when_tokenNotFound() {
            // given
            when(jpaRepository.findFailedAttemptsById(1L)).thenReturn(Optional.empty());

            // when
            int result = adapter.incrementFailedAttemptsAndGet(1L);

            // then
            assertThat(result).isZero();
        }
    }
}
