package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.HiddenFriend;
import com.cotalk.domain.exception.HiddenFriendNotFoundException;
import com.cotalk.domain.port.outbound.HiddenFriendRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * UnhideFriendService 단위 테스트.
 * 친구 숨김 해제 유스케이스를 검증한다.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
class UnhideFriendServiceTest {

    @Mock
    private HiddenFriendRepository hiddenFriendRepository;

    @InjectMocks
    private UnhideFriendService unhideFriendService;

    @Test
    @DisplayName("친구 숨김 해제 성공")
    void should_unhideFriend_when_validInput() {
        // Given
        Long userId = 1L;
        Long friendId = 2L;

        HiddenFriend hiddenFriend = HiddenFriend.builder()
                .userId(userId)
                .friendId(friendId)
                .build();

        given(hiddenFriendRepository.findByUserIdAndFriendId(userId, friendId))
                .willReturn(Optional.of(hiddenFriend));

        // When
        unhideFriendService.unhideFriend(userId, friendId);

        // Then
        verify(hiddenFriendRepository, times(1)).delete(hiddenFriend);
    }

    @Test
    @DisplayName("숨김 관계가 없을 때 예외 발생")
    void should_throwException_when_notHidden() {
        // Given
        Long userId = 1L;
        Long friendId = 2L;

        given(hiddenFriendRepository.findByUserIdAndFriendId(userId, friendId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> unhideFriendService.unhideFriend(userId, friendId))
                .isInstanceOf(HiddenFriendNotFoundException.class)
                .hasMessage("숨김 관계를 찾을 수 없습니다");

        verify(hiddenFriendRepository, never()).delete(any(HiddenFriend.class));
    }
}
