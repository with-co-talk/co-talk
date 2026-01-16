package com.cotalk.application.service;

import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.outbound.FriendRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetFriendListServiceTest {

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetFriendListService getFriendListService;

    @Test
    @DisplayName("친구 목록 조회 성공")
    void should_returnFriendList_when_validUserId() {
        // given
        Long userId = 1L;

        List<Friend> friends = List.of(
                Friend.builder()
                        .id(100L)
                        .userId(userId)
                        .friendId(2L)
                        .status(Friend.FriendStatus.ACCEPTED)
                        .build(),
                Friend.builder()
                        .id(101L)
                        .userId(userId)
                        .friendId(3L)
                        .status(Friend.FriendStatus.ACCEPTED)
                        .build()
        );

        User friend1 = User.builder()
                .id(2L)
                .email("friend1@example.com")
                .nickname("친구1")
                .passwordHash("hash")
                .build();

        User friend2 = User.builder()
                .id(3L)
                .email("friend2@example.com")
                .nickname("친구2")
                .passwordHash("hash")
                .build();

        given(friendRepository.findAcceptedFriendsByUserId(userId)).willReturn(friends);
        given(userRepository.findById(2L)).willReturn(Optional.of(friend1));
        given(userRepository.findById(3L)).willReturn(Optional.of(friend2));

        // when
        List<User> result = getFriendListService.getFriendList(userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNickname()).isEqualTo("친구1");
        assertThat(result.get(1).getNickname()).isEqualTo("친구2");
    }

    @Test
    @DisplayName("친구가 없을 때 빈 리스트 반환")
    void should_returnEmptyList_when_noFriends() {
        // given
        Long userId = 1L;
        given(friendRepository.findAcceptedFriendsByUserId(userId)).willReturn(List.of());

        // when
        List<User> result = getFriendListService.getFriendList(userId);

        // then
        assertThat(result).isEmpty();
    }
}
