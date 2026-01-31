package com.cotalk.application.service.profile;

import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.outbound.ProfileHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * 프로필 이력 수정 서비스 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateProfileHistoryService 테스트")
class UpdateProfileHistoryServiceTest {

    @Mock
    private ProfileHistoryRepository profileHistoryRepository;

    @InjectMocks
    private UpdateProfileHistoryService updateProfileHistoryService;

    @Nested
    @DisplayName("프로필 이력 공개 설정 수정")
    class UpdatePrivacy {

        @Test
        @DisplayName("본인의 이력 공개 설정 수정 성공")
        void should_updatePrivacy_when_ownerUpdates() {
            // given
            Long historyId = 1L;
            Long userId = 1L;
            boolean newPrivacy = true;

            ProfileHistory history = ProfileHistory.builder()
                    .id(historyId)
                    .userId(userId)
                    .type(ProfileHistoryType.AVATAR)
                    .url("https://example.com/avatar.png")
                    .isPrivate(false)
                    .isCurrent(true)
                    .build();

            given(profileHistoryRepository.findById(anyLong())).willReturn(Optional.of(history));
            given(profileHistoryRepository.save(any(ProfileHistory.class))).willReturn(history);

            // when
            updateProfileHistoryService.updatePrivacy(historyId, userId, newPrivacy);

            // then
            assertThat(history.isPrivate()).isTrue();
            then(profileHistoryRepository).should(times(1)).save(history);
        }

        @Test
        @DisplayName("다른 사용자의 이력 수정 시 예외 발생")
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
            assertThatThrownBy(() -> updateProfileHistoryService.updatePrivacy(historyId, otherUserId, true))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("본인의 프로필 이력만 수정할 수 있습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 이력 수정 시 예외 발생")
        void should_throwException_when_historyNotFound() {
            // given
            Long historyId = 999L;
            Long userId = 1L;

            given(profileHistoryRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> updateProfileHistoryService.updatePrivacy(historyId, userId, true))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("프로필 이력을 찾을 수 없습니다.");
        }
    }
}
