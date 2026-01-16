package com.cotalk.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
            String email = "test@example.com";
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
                    .email("test@example.com")
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
                    .email("test@example.com")
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
                    .email("test@example.com")
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
                    .email("test@example.com")
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
                    .email("test@example.com")
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
                    .email("test@example.com")
                    .passwordHash("hash")
                    .nickname("test")
                    .build();
            String newAvatarUrl = "https://example.com/avatar.png";

            // when
            user.updateAvatarUrl(newAvatarUrl);

            // then
            assertThat(user.getAvatarUrl()).isEqualTo(newAvatarUrl);
        }
    }
}
