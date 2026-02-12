package com.cotalk.application.service.friend;

import com.cotalk.adapter.inbound.rest.dto.friend.HiddenFriendDto;
import com.cotalk.domain.entity.HiddenFriend;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.outbound.HiddenFriendRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static com.cotalk.common.fixture.UserTestFixture.createUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * GetHiddenFriendsService 단위 테스트.
 * TDD를 통해 N+1 쿼리 문제 해결을 검증한다.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
class GetHiddenFriendsServiceTest {

    @Mock
    private HiddenFriendRepository hiddenFriendRepository;

    @Mock
    private UserRepository userRepository;

    @Captor
    private ArgumentCaptor<Iterable<Long>> idsCaptor;

    @InjectMocks
    private GetHiddenFriendsService getHiddenFriendsService;

    @Test
    @DisplayName("숨긴 친구가 여러 명일 때 findAllById를 한 번만 호출해야 함 (N+1 방지)")
    void should_callFindAllByIdOnce_when_multipleHiddenFriends() {
        // Given: 3명의 숨긴 친구
        Long userId = 1L;

        HiddenFriend hidden1 = createHiddenFriend(1L, userId, 10L);
        HiddenFriend hidden2 = createHiddenFriend(2L, userId, 20L);
        HiddenFriend hidden3 = createHiddenFriend(3L, userId, 30L);

        List<HiddenFriend> hiddenFriends = List.of(hidden1, hidden2, hidden3);
        given(hiddenFriendRepository.findByUserId(userId)).willReturn(hiddenFriends);

        User friend1 = createUser(10L, "friend1@test.com", "친구1");
        User friend2 = createUser(20L, "friend2@test.com", "친구2");
        User friend3 = createUser(30L, "friend3@test.com", "친구3");

        given(userRepository.findAllById(any())).willReturn(List.of(friend1, friend2, friend3));

        // When
        List<HiddenFriendDto> result = getHiddenFriendsService.getHiddenFriends(userId);

        // Then: findById가 호출되지 않고 findAllById가 정확히 1번 호출되어야 함
        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, times(1)).findAllById(idsCaptor.capture());

        // 전달된 ID 목록 검증
        List<Long> capturedIds = (List<Long>) idsCaptor.getValue();
        assertThat(capturedIds).containsExactlyInAnyOrder(10L, 20L, 30L);

        // 결과 검증
        assertThat(result).hasSize(3);
        assertThat(result).extracting("friendId").containsExactlyInAnyOrder(10L, 20L, 30L);
    }

    @Test
    @DisplayName("숨긴 친구가 없으면 빈 리스트 반환")
    void should_returnEmptyList_when_noHiddenFriends() {
        // Given
        Long userId = 1L;
        given(hiddenFriendRepository.findByUserId(userId)).willReturn(List.of());

        // When
        List<HiddenFriendDto> result = getHiddenFriendsService.getHiddenFriends(userId);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("숨긴 친구 중 일부가 탈퇴한 경우 존재하는 친구만 반환")
    void should_returnOnlyExistingFriends_when_someDeleted() {
        // Given: 3명 숨김, 그 중 1명 탈퇴
        Long userId = 1L;

        HiddenFriend hidden1 = createHiddenFriend(1L, userId, 10L);
        HiddenFriend hidden2 = createHiddenFriend(2L, userId, 20L);
        HiddenFriend hidden3 = createHiddenFriend(3L, userId, 30L);

        List<HiddenFriend> hiddenFriends = List.of(hidden1, hidden2, hidden3);
        given(hiddenFriendRepository.findByUserId(userId)).willReturn(hiddenFriends);

        User friend1 = createUser(10L, "friend1@test.com", "친구1");
        User friend3 = createUser(30L, "friend3@test.com", "친구3");
        // friend2는 탈퇴됨 (findAllById에서 반환되지 않음)

        given(userRepository.findAllById(any())).willReturn(List.of(friend1, friend3));

        // When
        List<HiddenFriendDto> result = getHiddenFriendsService.getHiddenFriends(userId);

        // Then: 탈퇴한 친구는 제외되고 2명만 반환
        assertThat(result).hasSize(2);
        assertThat(result).extracting("friendId").containsExactlyInAnyOrder(10L, 30L);

        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, times(1)).findAllById(any());
    }

    /**
     * HiddenFriend 테스트 엔티티 생성 헬퍼 메서드.
     *
     * @param id       숨김 관계 ID
     * @param userId   숨긴 사용자 ID
     * @param friendId 숨겨진 친구 ID
     * @return HiddenFriend 엔티티
     */
    private HiddenFriend createHiddenFriend(Long id, Long userId, Long friendId) {
        HiddenFriend hiddenFriend = HiddenFriend.builder()
                .userId(userId)
                .friendId(friendId)
                .build();
        // ID와 createdAt 설정을 위해 리플렉션 사용
        setField(hiddenFriend, "id", id);
        setField(hiddenFriend, "createdAt", LocalDateTime.now());
        return hiddenFriend;
    }

    /**
     * 리플렉션을 사용하여 필드 값을 설정하는 헬퍼 메서드.
     *
     * @param target    대상 객체
     * @param fieldName 필드 이름
     * @param value     설정할 값
     */
    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field;
            try {
                field = target.getClass().getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // 상위 클래스에서 찾기 (BaseEntity의 createdAt 등)
                field = target.getClass().getSuperclass().getDeclaredField(fieldName);
            }
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
