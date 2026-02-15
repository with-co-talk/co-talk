package com.cotalk.application.service.chat;

import com.cotalk.common.fixture.UserTestFixture;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.UserRepository;
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
}
