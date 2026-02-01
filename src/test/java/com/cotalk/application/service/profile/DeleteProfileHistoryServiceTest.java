package com.cotalk.application.service.profile;

import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.outbound.ProfileHistoryRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * 프로필 이력 삭제 서비스 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteProfileHistoryService 테스트")
class DeleteProfileHistoryServiceTest {

    @Mock
    private ProfileHistoryRepository profileHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DeleteProfileHistoryService deleteProfileHistoryService;

    @Nested
    @DisplayName("프로필 이력 삭제")
    class DeleteProfileHistory {

        @Test
        @DisplayName("본인의 이력 삭제 성공")
        void should_deleteHistory_when_ownerDeletes() {
            // given
            Long historyId = 1L;
            Long userId = 1L;

            ProfileHistory history = ProfileHistory.builder()
                    .id(historyId)
                    .userId(userId)
                    .type(ProfileHistoryType.AVATAR)
                    .url("https://example.com/avatar.png")
                    .isPrivate(false)
                    .isCurrent(false)
                    .build();

            given(profileHistoryRepository.findById(anyLong())).willReturn(Optional.of(history));

            // when
            deleteProfileHistoryService.deleteProfileHistory(historyId, userId);

            // then
            then(profileHistoryRepository).should(times(1)).delete(history);
        }

        @Test
        @DisplayName("현재 프로필 삭제 시 다음 이력으로 자동 전환")
        void should_promoteNextToCurrent_when_deletingCurrentHistory() {
            // given
            Long historyId = 1L;
            Long userId = 1L;

            User user = User.builder()
                    .id(userId)
                    .email("test@example.com")
                    .nickname("테스트유저")
                    .passwordHash("hash")
                    .avatarUrl("https://example.com/avatar1.png")
                    .build();

            ProfileHistory currentHistory = ProfileHistory.builder()
                    .id(historyId)
                    .userId(userId)
                    .type(ProfileHistoryType.AVATAR)
                    .url("https://example.com/avatar1.png")
                    .isPrivate(false)
                    .isCurrent(true)
                    .build();

            ProfileHistory nextHistory = ProfileHistory.builder()
                    .id(2L)
                    .userId(userId)
                    .type(ProfileHistoryType.AVATAR)
                    .url("https://example.com/avatar2.png")
                    .isPrivate(false)
                    .isCurrent(false)
                    .build();

            given(profileHistoryRepository.findById(anyLong())).willReturn(Optional.of(currentHistory));
            given(profileHistoryRepository.findByUserIdAndTypeOrderByCreatedAtDesc(anyLong(), any()))
                    .willReturn(List.of(nextHistory));
            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));

            // when
            deleteProfileHistoryService.deleteProfileHistory(historyId, userId);

            // then
            assertThat(nextHistory.isCurrent()).isTrue();
            assertThat(user.getAvatarUrl()).isEqualTo("https://example.com/avatar2.png");
            then(profileHistoryRepository).should(times(1)).save(nextHistory);
            then(userRepository).should(times(1)).save(user);
        }

        @Test
        @DisplayName("마지막 현재 프로필 삭제 시 User 프로필 필드 null로 설정")
        void should_clearUserProfile_when_deletingLastCurrentHistory() {
            // given
            Long historyId = 1L;
            Long userId = 1L;

            User user = User.builder()
                    .id(userId)
                    .email("test@example.com")
                    .nickname("테스트유저")
                    .passwordHash("hash")
                    .avatarUrl("https://example.com/avatar.png")
                    .build();

            ProfileHistory currentHistory = ProfileHistory.builder()
                    .id(historyId)
                    .userId(userId)
                    .type(ProfileHistoryType.AVATAR)
                    .url("https://example.com/avatar.png")
                    .isPrivate(false)
                    .isCurrent(true)
                    .build();

            given(profileHistoryRepository.findById(anyLong())).willReturn(Optional.of(currentHistory));
            given(profileHistoryRepository.findByUserIdAndTypeOrderByCreatedAtDesc(anyLong(), any()))
                    .willReturn(List.of());
            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));

            // when
            deleteProfileHistoryService.deleteProfileHistory(historyId, userId);

            // then
            assertThat(user.getAvatarUrl()).isNull();
            then(userRepository).should(times(1)).save(user);
        }

        @Test
        @DisplayName("현재 프로필이 아닌 이력 삭제 시 User 프로필 변경 없음")
        void should_notAffectUserProfile_when_deletingNonCurrentHistory() {
            // given
            Long historyId = 1L;
            Long userId = 1L;

            ProfileHistory history = ProfileHistory.builder()
                    .id(historyId)
                    .userId(userId)
                    .type(ProfileHistoryType.AVATAR)
                    .url("https://example.com/avatar.png")
                    .isPrivate(false)
                    .isCurrent(false)
                    .build();

            given(profileHistoryRepository.findById(anyLong())).willReturn(Optional.of(history));

            // when
            deleteProfileHistoryService.deleteProfileHistory(historyId, userId);

            // then
            then(profileHistoryRepository).should(times(1)).delete(history);
            then(userRepository).should(never()).findById(anyLong());
            then(userRepository).should(never()).save(any(User.class));
        }

        @Test
        @DisplayName("다른 사용자의 이력 삭제 시 예외 발생")
        void should_throwException_when_notOwner() {
            // given
            Long historyId = 1L;
            Long ownerId = 1L;
            Long otherUserId = 2L;

            ProfileHistory history = ProfileHistory.builder()
                    .id(historyId)
                    .userId(ownerId)
                    .type(ProfileHistoryType.AVATAR)
                    .url("https://example.com/avatar.png")
                    .isPrivate(false)
                    .isCurrent(true)
                    .build();

            given(profileHistoryRepository.findById(anyLong())).willReturn(Optional.of(history));

            // when & then
            assertThatThrownBy(() -> deleteProfileHistoryService.deleteProfileHistory(historyId, otherUserId))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("본인의 프로필 이력만 삭제할 수 있습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 이력 삭제 시 예외 발생")
        void should_throwException_when_historyNotFound() {
            // given
            Long historyId = 999L;
            Long userId = 1L;

            given(profileHistoryRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deleteProfileHistoryService.deleteProfileHistory(historyId, userId))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("프로필 이력을 찾을 수 없습니다.");
        }
    }
}
