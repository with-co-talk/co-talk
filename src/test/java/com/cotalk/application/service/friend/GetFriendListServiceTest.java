package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.domain.port.outbound.FriendRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.cotalk.domain.model.PageQuery;
import com.cotalk.domain.model.PageResult;

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
                .email(new Email("friend1@example.com"))
                .nickname("친구1")
                .passwordHash("hash")
                .build();

        User friend2 = User.builder()
                .id(3L)
                .email(new Email("friend2@example.com"))
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

    @Test
    @DisplayName("Pageable을 사용한 친구 목록 DB 레벨 페이지네이션 조회 성공")
    void should_returnPagedFriendList_when_pageableProvided() {
        // given
        Long userId = 1L;
        PageQuery query = PageQuery.of(0, 20);

        User friend1 = User.builder()
                .id(2L)
                .email(new Email("friend1@example.com"))
                .nickname("친구1")
                .passwordHash("hash")
                .build();

        PageResult<User> friendPage = new PageResult<>(List.of(friend1), 0, 20, 1);
        given(friendRepository.findAcceptedFriendsWithUserData(userId, query)).willReturn(friendPage);

        // when
        PageResult<User> result = getFriendListService.getFriendList(userId, query);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).getNickname()).isEqualTo("친구1");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Pageable을 사용한 두 번째 페이지 조회 성공")
    void should_returnSecondPage_when_pageableWithOffset() {
        // given
        Long userId = 1L;
        PageQuery query = PageQuery.of(1, 5);

        User friend = User.builder()
                .id(10L)
                .email(new Email("friend10@example.com"))
                .nickname("친구10")
                .passwordHash("hash")
                .build();

        PageResult<User> friendPage = new PageResult<>(List.of(friend), 1, 5, 10);
        given(friendRepository.findAcceptedFriendsWithUserData(userId, query)).willReturn(friendPage);

        // when
        PageResult<User> result = getFriendListService.getFriendList(userId, query);

        // then
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.totalElements()).isEqualTo(10);
        assertThat(result.totalPages()).isEqualTo(2);
    }
}
