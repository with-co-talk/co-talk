package com.cotalk.application.service.chat;

import com.cotalk.common.fixture.UserTestFixture;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link PublishTypingStatusService} 유닛 테스트.
 * 타이핑 상태 발행 로직을 검증한다.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
class PublishTypingStatusServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMessageBroker chatMessageBroker;

    @InjectMocks
    private PublishTypingStatusService publishTypingStatusService;

    @Test
    void should_publishTypingEvent_when_userStartsTyping() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;
        boolean isTyping = true;
        User user = UserTestFixture.createUser(userId, "user@example.com", "테스트유저");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        publishTypingStatusService.publishTypingStatus(chatRoomId, userId, isTyping);

        // then
        verify(userRepository).findById(userId);
        verify(chatMessageBroker).publishRoomEvent(eq(chatRoomId), any());
    }

    @Test
    void should_publishStopTypingEvent_when_userStopsTyping() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;
        boolean isTyping = false;
        User user = UserTestFixture.createUser(userId, "user@example.com", "테스트유저");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        publishTypingStatusService.publishTypingStatus(chatRoomId, userId, isTyping);

        // then
        verify(userRepository).findById(userId);
        verify(chatMessageBroker).publishRoomEvent(eq(chatRoomId), any());
    }

    @Test
    void should_useCachedNickname_when_userTypesMultipleTimes() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;
        User user = UserTestFixture.createUser(userId, "user@example.com", "테스트유저");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        publishTypingStatusService.publishTypingStatus(chatRoomId, userId, true);
        publishTypingStatusService.publishTypingStatus(chatRoomId, userId, false);
        publishTypingStatusService.publishTypingStatus(chatRoomId, userId, true);

        // then
        verify(userRepository, times(1)).findById(userId); // 캐시 사용으로 1번만 조회
        verify(chatMessageBroker, times(3)).publishRoomEvent(eq(chatRoomId), any());
    }

    @Test
    void should_handleMissingUser_when_userNotFound() {
        // given
        Long chatRoomId = 100L;
        Long userId = 999L;
        boolean isTyping = true;

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when
        publishTypingStatusService.publishTypingStatus(chatRoomId, userId, isTyping);

        // then
        verify(userRepository).findById(userId);
        verify(chatMessageBroker).publishRoomEvent(eq(chatRoomId), any());
    }

    @Nested
    @DisplayName("TTL 기반 닉네임 캐시")
    class NicknameCacheTtl {

        @Test
        @DisplayName("서로 다른 사용자는 독립적으로 캐시된다")
        void should_cacheIndependently_when_differentUsers() {
            // given
            Long chatRoomId = 100L;
            Long userId1 = 1L;
            Long userId2 = 2L;
            User user1 = UserTestFixture.createUser(userId1, "user1@example.com", "유저1");
            User user2 = UserTestFixture.createUser(userId2, "user2@example.com", "유저2");

            given(userRepository.findById(userId1)).willReturn(Optional.of(user1));
            given(userRepository.findById(userId2)).willReturn(Optional.of(user2));

            // when
            publishTypingStatusService.publishTypingStatus(chatRoomId, userId1, true);
            publishTypingStatusService.publishTypingStatus(chatRoomId, userId2, true);
            publishTypingStatusService.publishTypingStatus(chatRoomId, userId1, false);
            publishTypingStatusService.publishTypingStatus(chatRoomId, userId2, false);

            // then
            verify(userRepository, times(1)).findById(userId1);
            verify(userRepository, times(1)).findById(userId2);
            verify(chatMessageBroker, times(4)).publishRoomEvent(eq(chatRoomId), any());
        }

        @Test
        @DisplayName("100회 호출마다 만료 캐시 정리가 트리거된다 (메모리 누수 방지)")
        void should_notThrow_when_evictionTriggeredAfter100Calls() {
            // given
            Long chatRoomId = 100L;
            Long userId = 1L;
            User user = UserTestFixture.createUser(userId, "user@example.com", "테스트유저");

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when: 100회 호출하여 eviction 트리거
            for (int i = 0; i < 100; i++) {
                publishTypingStatusService.publishTypingStatus(chatRoomId, userId, i % 2 == 0);
            }

            // then: 예외 없이 정상 실행되고, 캐시가 동작하므로 DB 조회는 1번
            verify(userRepository, times(1)).findById(userId);
            verify(chatMessageBroker, times(100)).publishRoomEvent(eq(chatRoomId), any());
        }

        @Test
        @DisplayName("캐시된 null 닉네임도 캐시 히트로 처리된다")
        void should_cacheNullNickname_when_userNotFound() {
            // given
            Long chatRoomId = 100L;
            Long userId = 999L;

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when
            publishTypingStatusService.publishTypingStatus(chatRoomId, userId, true);
            publishTypingStatusService.publishTypingStatus(chatRoomId, userId, false);

            // then: null 결과도 캐시되므로 1번만 조회
            verify(userRepository, times(1)).findById(userId);
            verify(chatMessageBroker, times(2)).publishRoomEvent(eq(chatRoomId), any());
        }
    }
}
