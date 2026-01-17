package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.outbound.FriendRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetFriendListServiceTest {

    @Mock
    private FriendRepository friendRepository;

    @InjectMocks
    private GetFriendListService getFriendListService;

    @Test
    @DisplayName("친구 목록 조회 성공")
    void should_returnFriendList_when_validUserId() {
        // given
        Long userId = 1L;

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

        List<User> friends = List.of(friend1, friend2);
        given(friendRepository.findAcceptedFriendsWithUserData(userId)).willReturn(friends);

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
        given(friendRepository.findAcceptedFriendsWithUserData(userId)).willReturn(List.of());

        // when
        List<User> result = getFriendListService.getFriendList(userId);

        // then
        assertThat(result).isEmpty();
    }
}
