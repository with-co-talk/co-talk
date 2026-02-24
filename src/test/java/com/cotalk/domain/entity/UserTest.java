package com.cotalk.domain.entity;

import com.cotalk.domain.model.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User 엔티티")
class UserTest {

    @Nested
    @DisplayName("생성 시")
    class Creation {

        @Test
        @DisplayName("이메일, 비밀번호 해시, 닉네임으로 User를 생성할 수 있다")
        void should_CreateUser_when_ValidInputsProvided() {
            // given
            Email email = new Email("test@example.com");
            String passwordHash = "hashedPassword123";
            String nickname = "testUser";

            // when
            User user = User.builder()
                    .email(email)
                    .passwordHash(passwordHash)
                    .nickname(nickname)
                    .build();

            // then
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getPasswordHash()).isEqualTo(passwordHash);
            assertThat(user.getNickname()).isEqualTo(nickname);
        }

        @Test
        @DisplayName("기본 상태는 ACTIVE이다")
        void should_HaveActiveStatus_when_Created() {
            // given & when
            User user = User.builder()
                    .email(new Email("test@example.com"))
                    .passwordHash("hash")
                    .nickname("test")
                    .build();

            // then
            assertThat(user.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("ID는 Long 타입이다")
        void should_HaveLongTypeId_when_Created() {
            // given
            Long id = 123456789L;

            // when
            User user = User.builder()
                    .id(id)
                    .email(new Email("test@example.com"))
                    .passwordHash("hash")
                    .nickname("test")
                    .build();

            // then
            assertThat(user.getId()).isEqualTo(id);
            assertThat(user.getId()).isInstanceOf(Long.class);
        }
    }

    @Nested
    @DisplayName("닉네임 변경 시")
    class UpdateNickname {

        @Test
        @DisplayName("새 닉네임으로 변경할 수 있다")
        void should_UpdateNickname_when_ValidNicknameProvided() {
            // given
            User user = User.builder()
                    .email(new Email("test@example.com"))
                    .passwordHash("hash")
                    .nickname("oldNickname")
                    .build();
            String newNickname = "newNickname";

            // when
            user.updateNickname(newNickname);

            // then
            assertThat(user.getNickname()).isEqualTo(newNickname);
        }

        @Test
        @DisplayName("빈 닉네임으로 변경할 수 없다")
        void should_ThrowException_when_EmptyNicknameProvided() {
            // given
            User user = User.builder()
                    .email(new Email("test@example.com"))
                    .passwordHash("hash")
                    .nickname("oldNickname")
                    .build();

            // when & then
            assertThatThrownBy(() -> user.updateNickname(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("닉네임");
        }

        @Test
        @DisplayName("null 닉네임으로 변경할 수 없다")
        void should_ThrowException_when_NullNicknameProvided() {
            // given
            User user = User.builder()
                    .email(new Email("test@example.com"))
                    .passwordHash("hash")
                    .nickname("oldNickname")
                    .build();

            // when & then
            assertThatThrownBy(() -> user.updateNickname(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("아바타 URL 변경 시")
    class UpdateAvatarUrl {

        @Test
        @DisplayName("새 아바타 URL로 변경할 수 있다")
        void should_UpdateAvatarUrl_when_ValidUrlProvided() {
            // given
            User user = User.builder()
                    .email(new Email("test@example.com"))
                    .passwordHash("hash")
                    .nickname("test")
                    .build();
            String newAvatarUrl = "https://example.com/avatar.png";

            // when
            user.updateAvatarUrl(newAvatarUrl);

            // then
            assertThat(user.getAvatarUrl()).isEqualTo(newAvatarUrl);
        }

        @Test
        @DisplayName("null 아바타 URL로 변경할 수 있다")
        void should_UpdateAvatarUrl_when_NullUrlProvided() {
            // given
            User user = User.builder()
                    .email(new Email("test@example.com"))
                    .passwordHash("hash")
                    .nickname("test")
                    .avatarUrl("https://example.com/old.png")
                    .build();

            // when
            user.updateAvatarUrl(null);

            // then
            assertThat(user.getAvatarUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("비밀번호 변경 시")
    class UpdatePassword {

        @Test
        @DisplayName("새 비밀번호 해시로 변경할 수 있다")
        void should_UpdatePassword_when_ValidPasswordHashProvided() {
            // given
            User user = User.builder()
                    .email(new Email("test@example.com"))
                    .passwordHash("oldHash")
                    .nickname("test")
                    .build();
            String newPasswordHash = "newHash123";

            // when
            user.updatePassword(newPasswordHash);

            // then
            assertThat(user.getPasswordHash()).isEqualTo(newPasswordHash);
        }

        @Test
        @DisplayName("빈 비밀번호 해시로 변경할 수 없다")
        void should_ThrowException_when_EmptyPasswordHashProvided() {
            // given
            User user = User.builder()
                    .email(new Email("test@example.com"))
                    .passwordHash("oldHash")
                    .nickname("test")
                    .build();

            // when & then
            assertThatThrownBy(() -> user.updatePassword(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("비밀번호");
        }

        @Test
        @DisplayName("null 비밀번호 해시로 변경할 수 없다")
        void should_ThrowException_when_NullPasswordHashProvided() {
            // given
            User user = User.builder()
                    .email(new Email("test@example.com"))
                    .passwordHash("oldHash")
                    .nickname("test")
                    .build();

            // when & then
            assertThatThrownBy(() -> user.updatePassword(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("계정 상태 변경 시")
    class AccountStatus {

        @Test
        @DisplayName("계정을 비활성화할 수 있다")
        void should_Deactivate_when_DeactivateCalled() {
            // given
            User user = User.builder()
                    .email(new Email("test@example.com"))
                    .passwordHash("hash")
                    .nickname("test")
                    .status(User.UserStatus.ACTIVE)
                    .build();

            // when
            user.deactivate();

            // then
            assertThat(user.getStatus()).isEqualTo(User.UserStatus.INACTIVE);
        }

        @Test
        @DisplayName("계정을 정지시킬 수 있다")
        void should_Suspend_when_SuspendCalled() {
            // given
            User user = User.builder()
                    .email(new Email("test@example.com"))
                    .passwordHash("hash")
                    .nickname("test")
                    .status(User.UserStatus.ACTIVE)
                    .build();

            // when
            user.suspend();

            // then
            assertThat(user.getStatus()).isEqualTo(User.UserStatus.SUSPENDED);
        }

        @Test
        @DisplayName("계정을 활성화할 수 있다")
        void should_Activate_when_ActivateCalled() {
            // given
            User user = User.builder()
                    .email(new Email("test@example.com"))
                    .passwordHash("hash")
                    .nickname("test")
                    .status(User.UserStatus.INACTIVE)
                    .build();

            // when
            user.activate();

            // then
            assertThat(user.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("활성 상태인지 확인할 수 있다")
        void should_ReturnTrue_when_UserIsActive() {
            // given
            User activeUser = User.builder()
                    .email(new Email("test@example.com"))
                    .passwordHash("hash")
                    .nickname("test")
                    .status(User.UserStatus.ACTIVE)
                    .build();

            User inactiveUser = User.builder()
                    .email(new Email("test2@example.com"))
                    .passwordHash("hash")
                    .nickname("test2")
                    .status(User.UserStatus.INACTIVE)
                    .build();

            // when & then
            assertThat(activeUser.isActive()).isTrue();
            assertThat(inactiveUser.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("온라인 상태 변경 시")
    class OnlineStatus {

        @Test
        @DisplayName("사용자를 온라인 상태로 설정할 수 있다")
        void should_SetOnline_when_SetOnlineCalled() {
            // given
            User user = User.builder()
                    .email(new Email("test@example.com"))
                    .passwordHash("hash")
                    .nickname("test")
                    .onlineStatus(User.OnlineStatus.OFFLINE)
                    .build();

            // when
            user.goOnline(LocalDateTime.of(2026, 1, 1, 12, 0));

            // then
            assertThat(user.getOnlineStatus()).isEqualTo(User.OnlineStatus.ONLINE);
            assertThat(user.getLastActiveAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 12, 0));
        }

        @Test
        @DisplayName("사용자를 오프라인 상태로 설정할 수 있다")
        void should_SetOffline_when_SetOfflineCalled() {
            // given
            User user = User.builder()
                    .email(new Email("test@example.com"))
                    .passwordHash("hash")
                    .nickname("test")
                    .onlineStatus(User.OnlineStatus.ONLINE)
                    .build();

            // when
            user.goOffline(LocalDateTime.of(2026, 1, 1, 12, 0));

            // then
            assertThat(user.getOnlineStatus()).isEqualTo(User.OnlineStatus.OFFLINE);
            assertThat(user.getLastActiveAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 12, 0));
        }

        @Test
        @DisplayName("사용자를 자리비움 상태로 설정할 수 있다")
        void should_SetAway_when_SetAwayCalled() {
            // given
            User user = User.builder()
                    .email(new Email("test@example.com"))
                    .passwordHash("hash")
                    .nickname("test")
                    .onlineStatus(User.OnlineStatus.ONLINE)
                    .build();

            // when
            user.goAway(LocalDateTime.of(2026, 1, 1, 12, 0));

            // then
            assertThat(user.getOnlineStatus()).isEqualTo(User.OnlineStatus.AWAY);
            assertThat(user.getLastActiveAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 12, 0));
        }

        @Test
        @DisplayName("마지막 활동 시간을 갱신할 수 있다")
        void should_UpdateLastActiveAt_when_UpdateLastActiveAtCalled() {
            // given
            User user = User.builder()
                    .email(new Email("test@example.com"))
                    .passwordHash("hash")
                    .nickname("test")
                    .lastActiveAt(LocalDateTime.of(2026, 1, 1, 11, 0))
                    .build();

            // when
            user.updateLastActiveAt(LocalDateTime.of(2026, 1, 1, 12, 0));

            // then
            assertThat(user.getLastActiveAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 12, 0));
        }

        @Test
        @DisplayName("온라인 상태인지 확인할 수 있다")
        void should_ReturnTrue_when_UserIsOnline() {
            // given
            User onlineUser = User.builder()
                    .email(new Email("test@example.com"))
                    .passwordHash("hash")
                    .nickname("test")
                    .onlineStatus(User.OnlineStatus.ONLINE)
                    .build();

            User offlineUser = User.builder()
                    .email(new Email("test2@example.com"))
                    .passwordHash("hash")
                    .nickname("test2")
                    .onlineStatus(User.OnlineStatus.OFFLINE)
                    .build();

            // when & then
            assertThat(onlineUser.isOnline()).isTrue();
            assertThat(offlineUser.isOnline()).isFalse();
        }
    }

    @Nested
    @DisplayName("OAuth 사용자 확인 시")
    class OAuthUser {

        @Test
        @DisplayName("OAuth 사용자인지 확인할 수 있다")
        void should_ReturnTrue_when_UserIsOAuthUser() {
            // given
            User oauthUser = User.builder()
                    .email(new Email("test@example.com"))
                    .passwordHash("hash")
                    .nickname("test")
                    .oauthProvider(User.OAuthProvider.KAKAO)
                    .oauthId("oauth123")
                    .build();

            User normalUser = User.builder()
                    .email(new Email("test2@example.com"))
                    .passwordHash("hash")
                    .nickname("test2")
                    .build();

            // when & then
            assertThat(oauthUser.isOAuthUser()).isTrue();
            assertThat(normalUser.isOAuthUser()).isFalse();
        }

        @Test
        @DisplayName("OAuth 제공자가 null이면 OAuth 사용자가 아니다")
        void should_ReturnFalse_when_OAuthProviderIsNull() {
            // given
            User user = User.builder()
                    .email(new Email("test@example.com"))
                    .passwordHash("hash")
                    .nickname("test")
                    .oauthProvider(null)
                    .build();

            // when & then
            assertThat(user.isOAuthUser()).isFalse();
        }
    }
}
