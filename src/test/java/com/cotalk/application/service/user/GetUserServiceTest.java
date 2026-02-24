package com.cotalk.application.service.user;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.model.Email;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GetUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetUserService getUserService;

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("ID로 사용자 조회 성공")
        void should_returnUser_when_userExists() {
            // given
            Long userId = 1L;
            User expectedUser = User.builder()
                    .id(userId)
                    .email(new Email("test@example.com"))
                    .nickname("테스트유저")
                    .passwordHash("hash")
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(expectedUser));

            // when
            User result = getUserService.getUserById(userId);

            // then
            assertThat(result.getId()).isEqualTo(userId);
            assertThat(result.getNickname()).isEqualTo("테스트유저");
            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 UserNotFoundException 발생")
        void should_throwUserNotFoundException_when_userNotFound() {
            // given
            Long userId = 999L;
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> getUserService.getUserById(userId))
                    .isInstanceOf(UserNotFoundException.class);
            verify(userRepository).findById(userId);
        }
    }

    @Nested
    @DisplayName("getUsersByIds")
    class GetUsersByIds {

        @Test
        @DisplayName("여러 ID로 사용자 목록 조회 성공")
        void should_returnUsers_when_usersExist() {
            // given
            List<Long> userIds = List.of(1L, 2L);
            List<User> expectedUsers = List.of(
                    User.builder()
                            .id(1L)
                            .email(new Email("user1@example.com"))
                            .nickname("유저1")
                            .passwordHash("hash")
                            .build(),
                    User.builder()
                            .id(2L)
                            .email(new Email("user2@example.com"))
                            .nickname("유저2")
                            .passwordHash("hash")
                            .build()
            );

            given(userRepository.findAllById(userIds)).willReturn(expectedUsers);

            // when
            List<User> result = getUserService.getUsersByIds(userIds);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getNickname()).isEqualTo("유저1");
            assertThat(result.get(1).getNickname()).isEqualTo("유저2");
            verify(userRepository).findAllById(userIds);
        }

        @Test
        @DisplayName("빈 ID 목록으로 조회 시 빈 리스트 반환")
        void should_returnEmptyList_when_emptyIds() {
            // given
            List<Long> userIds = List.of();
            given(userRepository.findAllById(userIds)).willReturn(List.of());

            // when
            List<User> result = getUserService.getUsersByIds(userIds);

            // then
            assertThat(result).isEmpty();
            verify(userRepository).findAllById(userIds);
        }
    }
}
