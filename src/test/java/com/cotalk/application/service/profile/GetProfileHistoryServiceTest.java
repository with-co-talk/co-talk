package com.cotalk.application.service.profile;

import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.outbound.ProfileHistoryRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

    @Mock
    private UserRepository userRepository;

    private GetProfileHistoryService getProfileHistoryService;

    @BeforeEach
    void setUp() {
        getProfileHistoryService = new GetProfileHistoryService(profileHistoryRepository, userRepository);
    }

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

    @Nested
    @DisplayName("현재 프로필 자동 이력 생성")
    class AutoCreateCurrentHistory {

        @Test
        @DisplayName("아바타 이력이 없고 User.avatarUrl이 있으면 자동으로 이력 생성")
        void should_createAvatarHistory_when_noHistoryButUserHasAvatarUrl() {
            // given
            Long userId = 1L;
            Long viewerId = 1L;
            String avatarUrl = "https://example.com/current-avatar.png";

            User user = User.builder()
                    .id(userId)
                    .email("test@test.com")
                    .nickname("테스트")
                    .passwordHash("hash")
                    .avatarUrl(avatarUrl)
                    .build();

            ProfileHistory createdHistory = ProfileHistory.builder()
                    .id(100L)
                    .userId(userId)
                    .type(ProfileHistoryType.AVATAR)
                    .url(avatarUrl)
                    .isPrivate(false)
                    .isCurrent(true)
                    .build();

            given(profileHistoryRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, ProfileHistoryType.AVATAR))
                    .willReturn(new ArrayList<>());
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(profileHistoryRepository.save(any(ProfileHistory.class))).willReturn(createdHistory);

            // when
            List<ProfileHistory> result = getProfileHistoryService.getProfileHistory(
                    userId, ProfileHistoryType.AVATAR, viewerId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUrl()).isEqualTo(avatarUrl);
            assertThat(result.get(0).isCurrent()).isTrue();
            verify(profileHistoryRepository).save(any(ProfileHistory.class));
        }

        @Test
        @DisplayName("배경화면 이력이 없고 User.backgroundUrl이 있으면 자동으로 이력 생성")
        void should_createBackgroundHistory_when_noHistoryButUserHasBackgroundUrl() {
            // given
            Long userId = 1L;
            Long viewerId = 1L;
            String backgroundUrl = "https://example.com/current-background.png";

            User user = User.builder()
                    .id(userId)
                    .email("test@test.com")
                    .nickname("테스트")
                    .passwordHash("hash")
                    .backgroundUrl(backgroundUrl)
                    .build();

            ProfileHistory createdHistory = ProfileHistory.builder()
                    .id(100L)
                    .userId(userId)
                    .type(ProfileHistoryType.BACKGROUND)
                    .url(backgroundUrl)
                    .isPrivate(false)
                    .isCurrent(true)
                    .build();

            given(profileHistoryRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, ProfileHistoryType.BACKGROUND))
                    .willReturn(new ArrayList<>());
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(profileHistoryRepository.save(any(ProfileHistory.class))).willReturn(createdHistory);

            // when
            List<ProfileHistory> result = getProfileHistoryService.getProfileHistory(
                    userId, ProfileHistoryType.BACKGROUND, viewerId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUrl()).isEqualTo(backgroundUrl);
            assertThat(result.get(0).isCurrent()).isTrue();
            verify(profileHistoryRepository).save(any(ProfileHistory.class));
        }

        @Test
        @DisplayName("상태메시지 이력이 없고 User.statusMessage가 있으면 자동으로 이력 생성")
        void should_createStatusMessageHistory_when_noHistoryButUserHasStatusMessage() {
            // given
            Long userId = 1L;
            Long viewerId = 1L;
            String statusMessage = "현재 상태메시지";

            User user = User.builder()
                    .id(userId)
                    .email("test@test.com")
                    .nickname("테스트")
                    .passwordHash("hash")
                    .statusMessage(statusMessage)
                    .build();

            ProfileHistory createdHistory = ProfileHistory.builder()
                    .id(100L)
                    .userId(userId)
                    .type(ProfileHistoryType.STATUS_MESSAGE)
                    .content(statusMessage)
                    .isPrivate(false)
                    .isCurrent(true)
                    .build();

            given(profileHistoryRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, ProfileHistoryType.STATUS_MESSAGE))
                    .willReturn(new ArrayList<>());
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(profileHistoryRepository.save(any(ProfileHistory.class))).willReturn(createdHistory);

            // when
            List<ProfileHistory> result = getProfileHistoryService.getProfileHistory(
                    userId, ProfileHistoryType.STATUS_MESSAGE, viewerId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContent()).isEqualTo(statusMessage);
            assertThat(result.get(0).isCurrent()).isTrue();
            verify(profileHistoryRepository).save(any(ProfileHistory.class));
        }

        @Test
        @DisplayName("이미 이력이 있고 User 값과 동일하면 새 이력 생성 안 함")
        void should_notCreateHistory_when_historyAlreadyExists() {
            // given
            Long userId = 1L;
            Long viewerId = 1L;
            String avatarUrl = "https://example.com/existing-avatar.png";

            List<ProfileHistory> existingHistories = new ArrayList<>(List.of(
                    ProfileHistory.builder()
                            .id(1L)
                            .userId(userId)
                            .type(ProfileHistoryType.AVATAR)
                            .url(avatarUrl)
                            .isPrivate(false)
                            .isCurrent(true)
                            .build()
            ));

            // User의 avatarUrl이 기존 이력과 동일
            User user = User.builder()
                    .id(userId)
                    .email("test@test.com")
                    .nickname("테스트")
                    .passwordHash("hash")
                    .avatarUrl(avatarUrl)
                    .build();

            given(profileHistoryRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, ProfileHistoryType.AVATAR))
                    .willReturn(existingHistories);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            List<ProfileHistory> result = getProfileHistoryService.getProfileHistory(
                    userId, ProfileHistoryType.AVATAR, viewerId);

            // then
            assertThat(result).hasSize(1);
            // syncCurrentHistoryWithUser가 User 조회하지만 값이 같으므로 save는 호출 안 됨
            verify(profileHistoryRepository, never()).save(any(ProfileHistory.class));
        }

        @Test
        @DisplayName("User에 해당 값이 없으면 자동 생성 안 함")
        void should_notCreateHistory_when_userHasNoValue() {
            // given
            Long userId = 1L;
            Long viewerId = 1L;

            User user = User.builder()
                    .id(userId)
                    .email("test@test.com")
                    .nickname("테스트")
                    .passwordHash("hash")
                    .avatarUrl(null)  // 아바타 URL 없음
                    .build();

            given(profileHistoryRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, ProfileHistoryType.AVATAR))
                    .willReturn(new ArrayList<>());
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            List<ProfileHistory> result = getProfileHistoryService.getProfileHistory(
                    userId, ProfileHistoryType.AVATAR, viewerId);

            // then
            assertThat(result).isEmpty();
            verify(profileHistoryRepository, never()).save(any(ProfileHistory.class));
        }

        @Test
        @DisplayName("타인이 조회할 때는 자동 생성 안 함")
        void should_notCreateHistory_when_viewerIsNotOwner() {
            // given
            Long userId = 1L;
            Long viewerId = 2L;  // 다른 사용자가 조회

            given(profileHistoryRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, ProfileHistoryType.AVATAR))
                    .willReturn(new ArrayList<>());

            // when
            List<ProfileHistory> result = getProfileHistoryService.getProfileHistory(
                    userId, ProfileHistoryType.AVATAR, viewerId);

            // then
            assertThat(result).isEmpty();
            verify(userRepository, never()).findById(anyLong());
            verify(profileHistoryRepository, never()).save(any(ProfileHistory.class));
        }

        @Test
        @DisplayName("User의 현재 값과 isCurrent 이력의 값이 다르면 새 이력 생성")
        void should_createNewHistory_when_userValueDiffersFromCurrent() {
            // given
            Long userId = 1L;
            Long viewerId = 1L;
            String oldAvatarUrl = "https://example.com/old-avatar.png";
            String newAvatarUrl = "https://example.com/new-avatar.png";

            ProfileHistory existingHistory = ProfileHistory.builder()
                    .id(1L)
                    .userId(userId)
                    .type(ProfileHistoryType.AVATAR)
                    .url(oldAvatarUrl)
                    .isPrivate(false)
                    .isCurrent(true)
                    .build();

            List<ProfileHistory> existingHistories = new ArrayList<>(List.of(existingHistory));

            // User의 avatarUrl이 기존 이력과 다름
            User user = User.builder()
                    .id(userId)
                    .email("test@test.com")
                    .nickname("테스트")
                    .passwordHash("hash")
                    .avatarUrl(newAvatarUrl)
                    .build();

            ProfileHistory newHistory = ProfileHistory.builder()
                    .id(2L)
                    .userId(userId)
                    .type(ProfileHistoryType.AVATAR)
                    .url(newAvatarUrl)
                    .isPrivate(false)
                    .isCurrent(true)
                    .build();

            given(profileHistoryRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, ProfileHistoryType.AVATAR))
                    .willReturn(existingHistories);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(profileHistoryRepository.save(any(ProfileHistory.class))).willAnswer(invocation -> {
                ProfileHistory saved = invocation.getArgument(0);
                // 새 이력인지 기존 이력 업데이트인지 확인
                if (saved.getUrl() != null && saved.getUrl().equals(newAvatarUrl)) {
                    return newHistory;
                }
                return saved;
            });

            // when
            List<ProfileHistory> result = getProfileHistoryService.getProfileHistory(
                    userId, ProfileHistoryType.AVATAR, viewerId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getUrl()).isEqualTo(newAvatarUrl);
            assertThat(result.get(0).isCurrent()).isTrue();
            // 기존 이력의 current는 해제됨
            verify(profileHistoryRepository, times(2)).save(any(ProfileHistory.class));
        }
    }
}
