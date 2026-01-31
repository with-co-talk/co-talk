package com.cotalk.application.service.user;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.outbound.FriendRepository;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UpdateUserOnlineStatusServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private UserEventBroker userEventBroker;

    private UpdateUserOnlineStatusService service;

    @BeforeEach
    void setUp() {
        service = new UpdateUserOnlineStatusService(userRepository, friendRepository, userEventBroker);

        // Default mock behavior - no friends by default (lenient to avoid UnnecessaryStubbingException)
        lenient().when(friendRepository.findAcceptedFriendsByUserId(anyLong())).thenReturn(java.util.List.of());
    }

    @Test
    @DisplayName("온라인 상태로 업데이트 성공")
    void should_setOnline_when_validUser() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .nickname("테스트유저")
                .passwordHash("hash")
                .onlineStatus(User.OnlineStatus.OFFLINE)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        service.setOnline(userId);

        // then
        assertThat(user.isOnline()).isTrue();
        assertThat(user.getLastActiveAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("오프라인 상태로 업데이트 성공")
    void should_setOffline_when_validUser() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .nickname("테스트유저")
                .passwordHash("hash")
                .onlineStatus(User.OnlineStatus.ONLINE)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        service.setOffline(userId);

        // then
        assertThat(user.getOnlineStatus()).isEqualTo(User.OnlineStatus.OFFLINE);
        assertThat(user.getLastActiveAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("온라인 상태 업데이트 - ONLINE")
    void should_updateOnlineStatus_when_online() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .nickname("테스트유저")
                .passwordHash("hash")
                .onlineStatus(User.OnlineStatus.OFFLINE)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        service.updateOnlineStatus(userId, User.OnlineStatus.ONLINE);

        // then
        assertThat(user.getOnlineStatus()).isEqualTo(User.OnlineStatus.ONLINE);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("마지막 접속 시간 업데이트")
    void should_updateLastActiveAt_when_called() {
        // given
        Long userId = 1L;
        LocalDateTime beforeUpdate = LocalDateTime.now().minusMinutes(10);
        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .nickname("테스트유저")
                .passwordHash("hash")
                .lastActiveAt(beforeUpdate)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        service.updateLastActiveAt(userId);

        // then
        assertThat(user.getLastActiveAt()).isAfter(beforeUpdate);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 상태 업데이트 시 예외")
    void should_throwException_when_userNotFound() {
        // given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.setOnline(userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("온라인 상태 업데이트 - AWAY")
    void should_updateOnlineStatus_when_away() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .nickname("테스트유저")
                .passwordHash("hash")
                .onlineStatus(User.OnlineStatus.ONLINE)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        service.updateOnlineStatus(userId, User.OnlineStatus.AWAY);

        // then
        assertThat(user.getOnlineStatus()).isEqualTo(User.OnlineStatus.AWAY);
        verify(userRepository).save(user);
    }
}
