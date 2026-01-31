package com.cotalk.infrastructure.presence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryChatRoomPresenceTracker")
class InMemoryChatRoomPresenceTrackerTest {

    @Test
    @DisplayName("markActive 후 isActive/countActiveMembers가 true/1 이다")
    void should_beActive_after_markActive() {
        // given
        InMemoryChatRoomPresenceTracker tracker = new InMemoryChatRoomPresenceTracker();

        // when
        tracker.markActive(10L, 1L, "s1");

        // then
        assertThat(tracker.isActive(10L, 1L)).isTrue();
        assertThat(tracker.countActiveMembers(10L)).isEqualTo(1);
    }

    @Test
    @DisplayName("markInactive 후 isActive/countActiveMembers가 false/0 이다")
    void should_beInactive_after_markInactive() {
        // given
        InMemoryChatRoomPresenceTracker tracker = new InMemoryChatRoomPresenceTracker();
        tracker.markActive(10L, 1L, "s1");

        // when
        tracker.markInactive(10L, 1L, "s1");

        // then
        assertThat(tracker.isActive(10L, 1L)).isFalse();
        assertThat(tracker.countActiveMembers(10L)).isEqualTo(0);
    }

    @Test
    @DisplayName("clearSession은 해당 세션이 활성화한 방에서 유저를 제거한다")
    void should_clearUserFromRooms_when_clearSession() {
        // given
        InMemoryChatRoomPresenceTracker tracker = new InMemoryChatRoomPresenceTracker();
        tracker.markActive(10L, 1L, "s1");
        tracker.markActive(11L, 1L, "s1");
        assertThat(tracker.countActiveMembers(10L)).isEqualTo(1);
        assertThat(tracker.countActiveMembers(11L)).isEqualTo(1);

        // when
        tracker.clearSession(1L, "s1");

        // then
        assertThat(tracker.countActiveMembers(10L)).isEqualTo(0);
        assertThat(tracker.countActiveMembers(11L)).isEqualTo(0);
        assertThat(tracker.isActive(10L, 1L)).isFalse();
        assertThat(tracker.isActive(11L, 1L)).isFalse();
    }
}

