package com.cotalk.application.service.chat;

import com.cotalk.domain.port.outbound.ChatRoomPresenceTracker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * {@link UpdatePresenceStatusService} 유닛 테스트.
 * 채팅방 presence 상태 업데이트 로직을 검증한다.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
class UpdatePresenceStatusServiceTest {

    @Mock
    private ChatRoomPresenceTracker chatRoomPresenceTracker;

    @InjectMocks
    private UpdatePresenceStatusService updatePresenceStatusService;

    @Test
    void should_markUserAsActive_when_userEntersRoom() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;
        String sessionId = "session-123";

        // when
        updatePresenceStatusService.markActive(chatRoomId, userId, sessionId);

        // then
        verify(chatRoomPresenceTracker).markActive(chatRoomId, userId, sessionId);
    }

    @Test
    void should_markUserAsInactive_when_userLeavesRoom() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;
        String sessionId = "session-123";

        // when
        updatePresenceStatusService.markInactive(chatRoomId, userId, sessionId);

        // then
        verify(chatRoomPresenceTracker).markInactive(chatRoomId, userId, sessionId);
    }

    @Test
    void should_handleMultipleSessions_when_userHasMultipleDevices() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;
        String sessionId1 = "session-mobile";
        String sessionId2 = "session-desktop";

        // when
        updatePresenceStatusService.markActive(chatRoomId, userId, sessionId1);
        updatePresenceStatusService.markActive(chatRoomId, userId, sessionId2);

        // then
        verify(chatRoomPresenceTracker).markActive(chatRoomId, userId, sessionId1);
        verify(chatRoomPresenceTracker).markActive(chatRoomId, userId, sessionId2);
    }

    @Test
    void should_markInactive_when_specificSessionDisconnects() {
        // given
        Long chatRoomId = 100L;
        Long userId = 1L;
        String sessionId = "session-mobile";

        // when
        updatePresenceStatusService.markInactive(chatRoomId, userId, sessionId);

        // then
        verify(chatRoomPresenceTracker).markInactive(chatRoomId, userId, sessionId);
    }
}
