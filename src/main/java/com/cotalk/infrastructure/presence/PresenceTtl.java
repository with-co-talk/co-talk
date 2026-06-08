package com.cotalk.infrastructure.presence;

import java.util.concurrent.TimeUnit;

/**
 * 채팅방 활성 상태(presence) 트래커의 TTL 상수를 단일 정의하는 클래스.
 *
 * <p>Redis/InMemory 구현이 각자 TTL을 정의하면 한쪽만 수정했을 때 환경별로 동작이
 * 갈리므로, 두 구현이 동일한 값을 참조하도록 여기서 단일화한다.
 *
 * <p>불변식: {@code ROOM_TTL < ROOM_TTL + COUNT_KEY_GRACE < SESSION_TTL}
 * (현재 값 30s &lt; 40s &lt; 45s). {@code COUNT_KEY_GRACE}는 markInactive의 DECR가
 * 도달하기 전에 countKey가 먼저 만료돼 카운트가 유실되는 것을 막는 여유 시간이다.
 *
 * <p>주의: {@code ROOM_TTL}은 클라이언트 presence ping 주기(현재 co-talk-flutter
 * {@code PresenceManager}의 20s)와 암묵적으로 결합돼 있다. 스퓨리어스 푸시
 * (시청 중인데 만료로 누락)를 피하려면 {@code ROOM_TTL > 2 * pingInterval}을
 * 유지해야 한다. ping=20s 기준 현재 30s는 단일 ping 누락에 노출되므로, 근본 해결은
 * 클라이언트 ping 주기 단축(12s 이하)이 필요하다.
 */
public final class PresenceTtl {

    private PresenceTtl() {
    }

    /** 방 활성 엔트리(ZSet score / InMemory 만료시각)의 유효 시간. */
    public static final long ROOM_TTL_MILLIS = TimeUnit.SECONDS.toMillis(30);

    /** markInactive DECR 도달 전 countKey 조기 만료 방지용 여유. */
    public static final long COUNT_KEY_GRACE_MILLIS = TimeUnit.SECONDS.toMillis(10);

    /** countKey 유효 시간 = 방 TTL + 여유. */
    public static final long COUNT_KEY_TTL_MILLIS = ROOM_TTL_MILLIS + COUNT_KEY_GRACE_MILLIS;

    /** session -> rooms Set의 유효 시간(방 TTL보다 길게 잡아 cleanup 경합 방지). */
    public static final long SESSION_TTL_MILLIS = TimeUnit.SECONDS.toMillis(45);
}
