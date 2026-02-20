package com.cotalk.application.service.profile;

import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * 현재 프로필 설정 서비스 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SetCurrentProfileService 테스트")
class SetCurrentProfileServiceTest {

    @Mock
    private ProfileHistoryRepository profileHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SetCurrentProfileService setCurrentProfileService;

    @Nested
    @DisplayName("현재 프로필 설정")
    class SetCurrentProfile {

        @Test
        @DisplayName("유효한 요청으로 현재 프로필 설정 성공")
        void should_setAsCurrent_when_validRequest() {
            // given
            Long historyId = 2L;
            Long userId = 1L;

            User user = User.builder()
                    .id(userId)
                    .email(new Email("test@example.com"))
                    .nickname("테스트유저")
                    .passwordHash("hash")
                    .build();

            ProfileHistory history = ProfileHistory.builder()
                    .id(historyId)
                    .userId(userId)
                    .type(ProfileHistoryType.AVATAR)
                    .url("https://example.com/avatar2.png")
                    .isPrivate(false)
                    .isCurrent(false)
                    .build();

            given(profileHistoryRepository.findById(anyLong())).willReturn(Optional.of(history));
            given(profileHistoryRepository.findByUserIdAndTypeAndIsCurrentTrue(anyLong(), any()))
                    .willReturn(Optional.empty());
            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));

            // when
            setCurrentProfileService.setCurrentProfile(historyId, userId);

            // then
            assertThat(history.isCurrent()).isTrue();
            assertThat(user.getAvatarUrl()).isEqualTo("https://example.com/avatar2.png");
            then(profileHistoryRepository).should(times(1)).save(history);
            then(userRepository).should(times(1)).save(user);
        }

        @Test
        @DisplayName("이미 현재 프로필이 있을 때 기존 프로필 해제")
        void should_unsetPreviousCurrent_when_anotherHistoryWasCurrent() {
            // given
            Long historyId = 2L;
            Long userId = 1L;

            User user = User.builder()
                    .id(userId)
                    .email(new Email("test@example.com"))
                    .nickname("테스트유저")
                    .passwordHash("hash")
                    .avatarUrl("https://example.com/avatar1.png")
                    .build();

            ProfileHistory existingCurrent = ProfileHistory.builder()
                    .id(1L)
                    .userId(userId)
                    .type(ProfileHistoryType.AVATAR)
                    .url("https://example.com/avatar1.png")
                    .isPrivate(false)
                    .isCurrent(true)
                    .build();

            ProfileHistory newCurrent = ProfileHistory.builder()
                    .id(historyId)
                    .userId(userId)
                    .type(ProfileHistoryType.AVATAR)
                    .url("https://example.com/avatar2.png")
                    .isPrivate(false)
                    .isCurrent(false)
                    .build();

            given(profileHistoryRepository.findById(anyLong())).willReturn(Optional.of(newCurrent));
            given(profileHistoryRepository.findByUserIdAndTypeAndIsCurrentTrue(anyLong(), any()))
                    .willReturn(Optional.of(existingCurrent));
            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));

            // when
            setCurrentProfileService.setCurrentProfile(historyId, userId);

            // then
            assertThat(existingCurrent.isCurrent()).isFalse();
            assertThat(newCurrent.isCurrent()).isTrue();
            assertThat(user.getAvatarUrl()).isEqualTo("https://example.com/avatar2.png");
            then(profileHistoryRepository).should(times(1)).save(existingCurrent);
            then(profileHistoryRepository).should(times(1)).save(newCurrent);
        }

        @Test
        @DisplayName("현재 프로필 설정 시 User 엔티티 업데이트")
        void should_updateUserProfile_when_settingCurrent() {
            // given
            Long historyId = 1L;
            Long userId = 1L;

            User user = User.builder()
                    .id(userId)
                    .email(new Email("test@example.com"))
                    .nickname("테스트유저")
                    .passwordHash("hash")
                    .build();

            ProfileHistory history = ProfileHistory.builder()
                    .id(historyId)
                    .userId(userId)
                    .type(ProfileHistoryType.STATUS_MESSAGE)
                    .content("새로운 상태메시지")
                    .isPrivate(false)
                    .isCurrent(false)
                    .build();

            given(profileHistoryRepository.findById(anyLong())).willReturn(Optional.of(history));
            given(profileHistoryRepository.findByUserIdAndTypeAndIsCurrentTrue(anyLong(), any()))
                    .willReturn(Optional.empty());
            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));

            // when
            setCurrentProfileService.setCurrentProfile(historyId, userId);

            // then
            assertThat(user.getStatusMessage()).isEqualTo("새로운 상태메시지");
            then(userRepository).should(times(1)).save(user);
        }

        @Test
        @DisplayName("다른 사용자의 이력을 현재 프로필로 설정 시 예외 발생")
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
                    .isCurrent(false)
                    .build();

            given(profileHistoryRepository.findById(anyLong())).willReturn(Optional.of(history));

            // when & then
            assertThatThrownBy(() -> setCurrentProfileService.setCurrentProfile(historyId, otherUserId))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("본인의 프로필 이력만 설정할 수 있습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 이력을 현재 프로필로 설정 시 예외 발생")
        void should_throwException_when_historyNotFound() {
            // given
            Long historyId = 999L;
            Long userId = 1L;

            given(profileHistoryRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> setCurrentProfileService.setCurrentProfile(historyId, userId))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("프로필 이력을 찾을 수 없습니다.");
        }
    }
}
