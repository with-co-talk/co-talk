package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.HiddenFriend;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.FriendNotFoundException;
import com.cotalk.domain.exception.InvalidHiddenFriendException;
import com.cotalk.domain.port.outbound.FriendRepository;
import com.cotalk.domain.port.outbound.HiddenFriendRepository;
import com.cotalk.domain.validator.UserValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * HideFriendService 단위 테스트.
 * 친구 숨김 유스케이스를 검증한다.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
class HideFriendServiceTest {

    @Mock
    private HiddenFriendRepository hiddenFriendRepository;

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private UserValidator userValidator;

    @Captor
    private ArgumentCaptor<HiddenFriend> hiddenFriendCaptor;

    @InjectMocks
    private HideFriendService hideFriendService;

    @Test
    @DisplayName("친구 숨김 성공")
    void should_hideFriend_when_validInput() {
        // Given
        Long userId = 1L;
        Long friendId = 2L;

        given(userValidator.validateUserExists(anyLong())).willReturn(User.builder().id(1L).build());
        given(friendRepository.existsByUserIdAndFriendId(anyLong(), anyLong())).willReturn(true);
        given(hiddenFriendRepository.existsByUserIdAndFriendId(anyLong(), anyLong())).willReturn(false);

        // When
        hideFriendService.hideFriend(userId, friendId);

        // Then
        verify(hiddenFriendRepository, times(1)).save(hiddenFriendCaptor.capture());
        HiddenFriend savedHiddenFriend = hiddenFriendCaptor.getValue();

        assertThat(savedHiddenFriend.getUserId()).isEqualTo(userId);
        assertThat(savedHiddenFriend.getFriendId()).isEqualTo(friendId);
    }

    @Test
    @DisplayName("친구 관계가 없을 때 예외 발생")
    void should_throwException_when_notFriends() {
        // Given
        Long userId = 1L;
        Long friendId = 2L;

        given(userValidator.validateUserExists(anyLong())).willReturn(User.builder().id(1L).build());
        given(friendRepository.existsByUserIdAndFriendId(anyLong(), anyLong())).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> hideFriendService.hideFriend(userId, friendId))
                .isInstanceOf(FriendNotFoundException.class)
                .hasMessage("친구 관계를 찾을 수 없습니다");

        verify(hiddenFriendRepository, never()).save(any(HiddenFriend.class));
    }

    @Test
    @DisplayName("이미 숨긴 친구일 때 예외 발생")
    void should_throwException_when_alreadyHidden() {
        // Given
        Long userId = 1L;
        Long friendId = 2L;

        given(userValidator.validateUserExists(anyLong())).willReturn(User.builder().id(1L).build());
        given(friendRepository.existsByUserIdAndFriendId(anyLong(), anyLong())).willReturn(true);
        given(hiddenFriendRepository.existsByUserIdAndFriendId(anyLong(), anyLong())).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> hideFriendService.hideFriend(userId, friendId))
                .isInstanceOf(InvalidHiddenFriendException.class)
                .hasMessage("이미 숨긴 친구입니다");

        verify(hiddenFriendRepository, never()).save(any(HiddenFriend.class));
    }
}
