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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * 프로필 이력 생성 서비스 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateProfileHistoryService 테스트")
class CreateProfileHistoryServiceTest {

    @Mock
    private ProfileHistoryRepository profileHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CreateProfileHistoryService createProfileHistoryService;

    @Nested
    @DisplayName("프로필 이력 생성")
    class CreateProfileHistory {

        @Test
        @DisplayName("유효한 요청으로 이력 생성 성공")
        void should_createHistory_when_validRequest() {
            // given
            Long userId = 1L;
            String url = "https://example.com/avatar.png";

            User user = User.builder()
                    .id(userId)
                    .email(new Email("test@example.com"))
                    .nickname("테스트유저")
                    .passwordHash("hash")
                    .build();

            ProfileHistory history = ProfileHistory.builder()
                    .id(1L)
                    .userId(userId)
                    .type(ProfileHistoryType.AVATAR)
                    .url(url)
                    .isPrivate(false)
                    .isCurrent(true)
                    .build();

            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
            given(profileHistoryRepository.findByUserIdAndTypeAndIsCurrentTrue(anyLong(), any()))
                    .willReturn(Optional.empty());
            given(profileHistoryRepository.save(any(ProfileHistory.class))).willReturn(history);

            // when
            ProfileHistory result = createProfileHistoryService.createProfileHistory(
                    userId, ProfileHistoryType.AVATAR, url, null, false, true);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(ProfileHistoryType.AVATAR);
            assertThat(result.getUrl()).isEqualTo(url);
            then(userRepository).should(times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("AVATAR 타입 현재 프로필 설정 시 User avatarUrl 업데이트")
        void should_updateUserAvatar_when_typeIsAvatarAndSetCurrentTrue() {
            // given
            Long userId = 1L;
            String url = "https://example.com/avatar.png";

            User user = User.builder()
                    .id(userId)
                    .email(new Email("test@example.com"))
                    .nickname("테스트유저")
                    .passwordHash("hash")
                    .build();

            ProfileHistory history = ProfileHistory.builder()
                    .id(1L)
                    .userId(userId)
                    .type(ProfileHistoryType.AVATAR)
                    .url(url)
                    .isPrivate(false)
                    .isCurrent(true)
                    .build();

            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
            given(profileHistoryRepository.findByUserIdAndTypeAndIsCurrentTrue(anyLong(), any()))
                    .willReturn(Optional.empty());
            given(profileHistoryRepository.save(any(ProfileHistory.class))).willReturn(history);

            // when
            createProfileHistoryService.createProfileHistory(
                    userId, ProfileHistoryType.AVATAR, url, null, false, true);

            // then
            assertThat(user.getAvatarUrl()).isEqualTo(url);
            then(userRepository).should(times(1)).save(user);
        }

        @Test
        @DisplayName("BACKGROUND 타입 현재 프로필 설정 시 User backgroundUrl 업데이트")
        void should_updateUserBackground_when_typeIsBackgroundAndSetCurrentTrue() {
            // given
            Long userId = 1L;
            String url = "https://example.com/background.png";

            User user = User.builder()
                    .id(userId)
                    .email(new Email("test@example.com"))
                    .nickname("테스트유저")
                    .passwordHash("hash")
                    .build();

            ProfileHistory history = ProfileHistory.builder()
                    .id(1L)
                    .userId(userId)
                    .type(ProfileHistoryType.BACKGROUND)
                    .url(url)
                    .isPrivate(false)
                    .isCurrent(true)
                    .build();

            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
            given(profileHistoryRepository.findByUserIdAndTypeAndIsCurrentTrue(anyLong(), any()))
                    .willReturn(Optional.empty());
            given(profileHistoryRepository.save(any(ProfileHistory.class))).willReturn(history);

            // when
            createProfileHistoryService.createProfileHistory(
                    userId, ProfileHistoryType.BACKGROUND, url, null, false, true);

            // then
            assertThat(user.getBackgroundUrl()).isEqualTo(url);
            then(userRepository).should(times(1)).save(user);
        }

        @Test
        @DisplayName("STATUS_MESSAGE 타입 현재 프로필 설정 시 User statusMessage 업데이트")
        void should_updateUserStatusMessage_when_typeIsStatusMessageAndSetCurrentTrue() {
            // given
            Long userId = 1L;
            String content = "새로운 상태메시지";

            User user = User.builder()
                    .id(userId)
                    .email(new Email("test@example.com"))
                    .nickname("테스트유저")
                    .passwordHash("hash")
                    .build();

            ProfileHistory history = ProfileHistory.builder()
                    .id(1L)
                    .userId(userId)
                    .type(ProfileHistoryType.STATUS_MESSAGE)
                    .content(content)
                    .isPrivate(false)
                    .isCurrent(true)
                    .build();

            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
            given(profileHistoryRepository.findByUserIdAndTypeAndIsCurrentTrue(anyLong(), any()))
                    .willReturn(Optional.empty());
            given(profileHistoryRepository.save(any(ProfileHistory.class))).willReturn(history);

            // when
            createProfileHistoryService.createProfileHistory(
                    userId, ProfileHistoryType.STATUS_MESSAGE, null, content, false, true);

            // then
            assertThat(user.getStatusMessage()).isEqualTo(content);
            then(userRepository).should(times(1)).save(user);
        }

        @Test
        @DisplayName("현재 프로필 설정 시 기존 현재 프로필 해제")
        void should_unsetPreviousCurrent_when_setCurrentTrue() {
            // given
            Long userId = 1L;
            String url = "https://example.com/avatar.png";

            User user = User.builder()
                    .id(userId)
                    .email(new Email("test@example.com"))
                    .nickname("테스트유저")
                    .passwordHash("hash")
                    .build();

            ProfileHistory existingCurrent = ProfileHistory.builder()
                    .id(1L)
                    .userId(userId)
                    .type(ProfileHistoryType.AVATAR)
                    .url("https://example.com/old-avatar.png")
                    .isPrivate(false)
                    .isCurrent(true)
                    .build();

            ProfileHistory newHistory = ProfileHistory.builder()
                    .id(2L)
                    .userId(userId)
                    .type(ProfileHistoryType.AVATAR)
                    .url(url)
                    .isPrivate(false)
                    .isCurrent(true)
                    .build();

            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
            given(profileHistoryRepository.findByUserIdAndTypeAndIsCurrentTrue(anyLong(), any()))
                    .willReturn(Optional.of(existingCurrent));
            given(profileHistoryRepository.save(any(ProfileHistory.class))).willReturn(newHistory);

            // when
            createProfileHistoryService.createProfileHistory(
                    userId, ProfileHistoryType.AVATAR, url, null, false, true);

            // then
            assertThat(existingCurrent.isCurrent()).isFalse();
        }

        @Test
        @DisplayName("현재 프로필로 설정하지 않을 때 기존 프로필 유지")
        void should_notUnsetPreviousCurrent_when_setCurrentFalse() {
            // given
            Long userId = 1L;
            String url = "https://example.com/avatar.png";

            User user = User.builder()
                    .id(userId)
                    .email(new Email("test@example.com"))
                    .nickname("테스트유저")
                    .passwordHash("hash")
                    .build();

            ProfileHistory history = ProfileHistory.builder()
                    .id(1L)
                    .userId(userId)
                    .type(ProfileHistoryType.AVATAR)
                    .url(url)
                    .isPrivate(false)
                    .isCurrent(false)
                    .build();

            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
            given(profileHistoryRepository.save(any(ProfileHistory.class))).willReturn(history);

            // when
            createProfileHistoryService.createProfileHistory(
                    userId, ProfileHistoryType.AVATAR, url, null, false, false);

            // then
            then(profileHistoryRepository).should(never())
                    .findByUserIdAndTypeAndIsCurrentTrue(anyLong(), any());
            then(userRepository).should(never()).save(any(User.class));
        }

        @Test
        @DisplayName("존재하지 않는 사용자일 때 예외 발생")
        void should_throwException_when_userNotFound() {
            // given
            Long userId = 999L;
            given(userRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> createProfileHistoryService.createProfileHistory(
                    userId, ProfileHistoryType.AVATAR, "url", null, false, true))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("사용자를 찾을 수 없습니다.");
        }
    }
}
