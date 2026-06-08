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
 * <p>주의: {@code ROOM_TTL}은 클라이언트 presence ping 주기와 암묵적으로 결합돼 있다.
 * 시청 중인 사용자가 만료로 잘못 푸시를 받는 것을 피하려면 단일 ping 누락에도
 * 엔트리가 살아남도록 {@code ROOM_TTL > 2 * pingInterval} 불변식을 만족해야 한다.
 * 현재 클라이언트(co-talk-flutter {@code PresenceManager})의 ping 주기는 12s이고
 * {@code ROOM_TTL}은 30s이므로 {@code 30s > 2 * 12s = 24s}로 이 불변식을 만족한다.
 * ping 주기를 늘리려면 이 상수도 함께 재검토해야 한다.
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

    /**
     * session -> rooms Set의 유효 시간.
     *
     * <p>{@code COUNT_KEY_TTL}(40s)보다 길게(45s) 잡는다. session Set이 countKey보다
     * 먼저 만료되면 markInactive가 어느 방의 countKey를 DECR해야 할지 추적 정보를 잃어
     * countKey가 자연 만료될 때까지 카운트가 과대 집계된 채 남는다. session Set을 더
     * 오래 살려 두면 markInactive가 끝까지 정확한 countKey를 가리킬 수 있다.
     */
    public static final long SESSION_TTL_MILLIS = TimeUnit.SECONDS.toMillis(45);
}
