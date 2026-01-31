package com.cotalk.adapter.inbound.rest.dto.user;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.entity.User.OnlineStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserDto")
class UserDtoTest {

    @Nested
    @DisplayName("from 메서드")
    class FromMethod {

        @Test
        @DisplayName("User 엔티티로부터 UserDto를 생성할 수 있다")
        void should_createDto_when_fromUser() {
            // given
            User user = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .nickname("테스트유저")
                    .passwordHash("hash")
                    .avatarUrl("https://example.com/avatar.png")
                    .onlineStatus(OnlineStatus.ONLINE)
                    .lastActiveAt(LocalDateTime.now())
                    .build();

            // when
            UserDto dto = UserDto.from(user);

            // then
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.email()).isEqualTo("test@example.com");
            assertThat(dto.nickname()).isEqualTo("테스트유저");
            assertThat(dto.avatarUrl()).isEqualTo("https://example.com/avatar.png");
            assertThat(dto.onlineStatus()).isEqualTo(OnlineStatus.ONLINE);
            assertThat(dto.lastActiveAt()).isNotNull();
        }

        @Test
        @DisplayName("avatarUrl이 null인 경우 null로 변환된다")
        void should_handleNullAvatarUrl_when_fromUser() {
            // given
            User user = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .nickname("테스트유저")
                    .passwordHash("hash")
                    .avatarUrl(null)
                    .onlineStatus(OnlineStatus.OFFLINE)
                    .lastActiveAt(LocalDateTime.now())
                    .build();

            // when
            UserDto dto = UserDto.from(user);

            // then
            assertThat(dto.avatarUrl()).isNull();
        }

        @Test
        @DisplayName("lastActiveAt이 null인 경우 null로 변환된다")
        void should_handleNullLastActiveAt_when_fromUser() {
            // given
            User user = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .nickname("테스트유저")
                    .passwordHash("hash")
                    .onlineStatus(OnlineStatus.AWAY)
                    .lastActiveAt(null)
                    .build();

            // when
            UserDto dto = UserDto.from(user);

            // then
            assertThat(dto.lastActiveAt()).isNull();
        }

        @Test
        @DisplayName("user가 null인 경우 NullPointerException 발생")
        void should_throwException_when_userIsNull() {
            // when & then
            assertThatThrownBy(() -> UserDto.from(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
