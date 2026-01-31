package com.cotalk.application.service.profile;

import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;
import com.cotalk.domain.port.outbound.ProfileHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

/**
 * 프로필 이력 조회 서비스 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetProfileHistoryService 테스트")
class GetProfileHistoryServiceTest {

    @Mock
    private ProfileHistoryRepository profileHistoryRepository;

    @InjectMocks
    private GetProfileHistoryService getProfileHistoryService;

    @Nested
    @DisplayName("프로필 이력 조회")
    class GetProfileHistory {

        @Test
        @DisplayName("본인 조회 시 모든 이력 반환")
        void should_returnAllHistories_when_viewerIsOwner() {
            // given
            Long userId = 1L;
            Long viewerId = 1L;

            List<ProfileHistory> histories = List.of(
                    ProfileHistory.builder()
                            .id(1L)
                            .userId(userId)
                            .type(ProfileHistoryType.AVATAR)
                            .url("https://example.com/avatar1.png")
                            .isPrivate(true)
                            .isCurrent(true)
                            .build(),
                    ProfileHistory.builder()
                            .id(2L)
                            .userId(userId)
                            .type(ProfileHistoryType.AVATAR)
                            .url("https://example.com/avatar2.png")
                            .isPrivate(false)
                            .isCurrent(false)
                            .build()
            );

            given(profileHistoryRepository.findByUserIdOrderByCreatedAtDesc(anyLong()))
                    .willReturn(histories);

            // when
            List<ProfileHistory> result = getProfileHistoryService.getProfileHistory(
                    userId, null, viewerId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrderElementsOf(histories);
        }

        @Test
        @DisplayName("타인 조회 시 비공개 이력 필터링")
        void should_filterPrivateHistories_when_viewerIsNotOwner() {
            // given
            Long userId = 1L;
            Long viewerId = 2L;

            List<ProfileHistory> histories = List.of(
                    ProfileHistory.builder()
                            .id(1L)
                            .userId(userId)
                            .type(ProfileHistoryType.AVATAR)
                            .url("https://example.com/avatar1.png")
                            .isPrivate(true)
                            .isCurrent(true)
                            .build(),
                    ProfileHistory.builder()
                            .id(2L)
                            .userId(userId)
                            .type(ProfileHistoryType.AVATAR)
                            .url("https://example.com/avatar2.png")
                            .isPrivate(false)
                            .isCurrent(false)
                            .build()
            );

            given(profileHistoryRepository.findByUserIdOrderByCreatedAtDesc(anyLong()))
                    .willReturn(histories);

            // when
            List<ProfileHistory> result = getProfileHistoryService.getProfileHistory(
                    userId, null, viewerId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(2L);
            assertThat(result.get(0).isPrivate()).isFalse();
        }

        @Test
        @DisplayName("타입으로 필터링")
        void should_filterByType_when_typeIsProvided() {
            // given
            Long userId = 1L;
            Long viewerId = 1L;
            ProfileHistoryType type = ProfileHistoryType.AVATAR;

            List<ProfileHistory> histories = List.of(
                    ProfileHistory.builder()
                            .id(1L)
                            .userId(userId)
                            .type(ProfileHistoryType.AVATAR)
                            .url("https://example.com/avatar1.png")
                            .isPrivate(false)
                            .isCurrent(true)
                            .build()
            );

            given(profileHistoryRepository.findByUserIdAndTypeOrderByCreatedAtDesc(anyLong(), any()))
                    .willReturn(histories);

            // when
            List<ProfileHistory> result = getProfileHistoryService.getProfileHistory(
                    userId, type, viewerId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(ProfileHistoryType.AVATAR);
        }

        @Test
        @DisplayName("이력이 없을 때 빈 리스트 반환")
        void should_returnEmptyList_when_noHistoriesExist() {
            // given
            Long userId = 1L;
            Long viewerId = 1L;

            given(profileHistoryRepository.findByUserIdOrderByCreatedAtDesc(anyLong()))
                    .willReturn(List.of());

            // when
            List<ProfileHistory> result = getProfileHistoryService.getProfileHistory(
                    userId, null, viewerId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("모든 이력이 비공개일 때 타인은 빈 리스트 반환")
        void should_returnEmptyList_when_allArePrivateAndViewerIsNotOwner() {
            // given
            Long userId = 1L;
            Long viewerId = 2L;

            List<ProfileHistory> histories = List.of(
                    ProfileHistory.builder()
                            .id(1L)
                            .userId(userId)
                            .type(ProfileHistoryType.AVATAR)
                            .url("https://example.com/avatar1.png")
                            .isPrivate(true)
                            .isCurrent(true)
                            .build(),
                    ProfileHistory.builder()
                            .id(2L)
                            .userId(userId)
                            .type(ProfileHistoryType.AVATAR)
                            .url("https://example.com/avatar2.png")
                            .isPrivate(true)
                            .isCurrent(false)
                            .build()
            );

            given(profileHistoryRepository.findByUserIdOrderByCreatedAtDesc(anyLong()))
                    .willReturn(histories);

            // when
            List<ProfileHistory> result = getProfileHistoryService.getProfileHistory(
                    userId, null, viewerId);

            // then
            assertThat(result).isEmpty();
        }
    }
}
