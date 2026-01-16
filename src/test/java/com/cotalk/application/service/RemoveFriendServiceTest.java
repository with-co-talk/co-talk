package com.cotalk.application.service;

import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.RemoveFriendUseCase;
import com.cotalk.domain.port.outbound.FriendRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RemoveFriendServiceTest {

    @Mock
    private FriendRepository friendRepository;

    private RemoveFriendUseCase removeFriendUseCase;

    @BeforeEach
    void setUp() {
        removeFriendUseCase = new RemoveFriendService(friendRepository);
    }

    @Test
    @DisplayName("친구 삭제 성공")
    void should_removeFriend_when_validRequest() {
        // given
        Long userId = 1L;
        Long friendId = 2L;
        Friend friend = Friend.builder()
                .id(100L)
                .userId(userId)
                .friendId(friendId)
                .status(Friend.FriendStatus.ACCEPTED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(friendRepository.findByUserIdAndFriendId(userId, friendId)).willReturn(Optional.of(friend));

        // when
        removeFriendUseCase.removeFriend(userId, friendId);

        // then
        verify(friendRepository).delete(friend);
    }

    @Test
    @DisplayName("친구 관계가 존재하지 않으면 예외 발생")
    void should_throwException_when_friendshipNotFound() {
        // given
        Long userId = 1L;
        Long friendId = 2L;
        given(friendRepository.findByUserIdAndFriendId(userId, friendId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> removeFriendUseCase.removeFriend(userId, friendId))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("친구 관계를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("양방향 친구 관계 모두 삭제")
    void should_removeBothDirections_when_removeFriend() {
        // given
        Long userId = 1L;
        Long friendId = 2L;
        Friend friend1 = Friend.builder()
                .id(100L)
                .userId(userId)
                .friendId(friendId)
                .status(Friend.FriendStatus.ACCEPTED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Friend friend2 = Friend.builder()
                .id(101L)
                .userId(friendId)
                .friendId(userId)
                .status(Friend.FriendStatus.ACCEPTED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(friendRepository.findByUserIdAndFriendId(userId, friendId)).willReturn(Optional.of(friend1));
        given(friendRepository.findByUserIdAndFriendId(friendId, userId)).willReturn(Optional.of(friend2));

        // when
        removeFriendUseCase.removeFriend(userId, friendId);

        // then
        verify(friendRepository).delete(friend1);
        verify(friendRepository).delete(friend2);
    }
}
