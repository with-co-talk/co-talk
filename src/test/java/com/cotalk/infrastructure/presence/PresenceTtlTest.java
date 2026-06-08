package com.cotalk.infrastructure.presence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PresenceTtl")
class PresenceTtlTest {

    @Test
    @DisplayName("방 TTL은 30초로 단축돼 있다(백그라운드 푸시 억제 버그 방지). 60초로 되돌아가면 실패한다")
    void should_keepRoomTtlShortened() {
        assertThat(PresenceTtl.ROOM_TTL_MILLIS).isEqualTo(TimeUnit.SECONDS.toMillis(30));
    }

    @Test
    @DisplayName("불변식 ROOM_TTL < COUNT_KEY_TTL < SESSION_TTL 을 만족한다")
    void should_satisfyTtlInvariant() {
        assertThat(PresenceTtl.ROOM_TTL_MILLIS)
                .isLessThan(PresenceTtl.COUNT_KEY_TTL_MILLIS);
        assertThat(PresenceTtl.COUNT_KEY_TTL_MILLIS)
                .isLessThan(PresenceTtl.SESSION_TTL_MILLIS);
    }

    @Test
    @DisplayName("countKey TTL은 방 TTL + 여유(grace) 이다")
    void should_deriveCountKeyTtl_fromRoomTtlPlusGrace() {
        assertThat(PresenceTtl.COUNT_KEY_TTL_MILLIS)
                .isEqualTo(PresenceTtl.ROOM_TTL_MILLIS + PresenceTtl.COUNT_KEY_GRACE_MILLIS);
    }
}
