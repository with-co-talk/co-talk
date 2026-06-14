package com.cotalk.application.service.linkpreview;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * 현재 스레드에서 특정 호스트명을 검증된 단일 IP로 강제 해석(pin)하기 위한 스레드 로컬 레지스트리.
 * <p>
 * SSRF DNS rebinding(TOCTOU) 방어의 핵심 장치다. 링크프리뷰 fetch 직전에 호스트를 1회
 * 검증하고 그 IP를 이곳에 등록하면, JVM 전역에 설치된 {@link PinnedHostResolverProvider}가
 * 해당 호스트의 모든 DNS 조회를 등록된 IP로만 해석한다. 그 결과 검증 시점 IP와 실제 연결
 * 시점 IP가 항상 동일해져, 공격자 DNS가 검증/연결에 서로 다른 IP를 반환하는 rebinding을 차단한다.
 * </p>
 * <p>
 * 핀은 항상 호출 스레드로 한정되며, fetch가 끝나면 반드시 {@link #clear()}로 해제해야 한다
 * (다른 요청·다른 호스트의 정상 DNS 조회에 영향을 주지 않기 위함). 등록되지 않은 호스트는
 * 플랫폼 기본 리졸버로 그대로 위임되므로, 정상 외부 사이트 조회는 영향을 받지 않는다.
 * </p>
 *
 * @author seunggu.lee
 */
final class PinnedHostResolver {

    /**
     * 호출 스레드별 "호스트명(소문자) → 고정 IP" 매핑.
     */
    private static final ThreadLocal<Map<String, InetAddress>> PINS = ThreadLocal.withInitial(HashMap::new);

    private PinnedHostResolver() {
    }

    /**
     * 현재 스레드에서 주어진 호스트를 지정한 IP로 고정한다.
     *
     * @param host 고정할 호스트명
     * @param ip   해당 호스트로의 모든 조회가 반환할 검증된 IP
     */
    static void pin(String host, InetAddress ip) {
        PINS.get().put(host.toLowerCase(), ip);
    }

    /**
     * 현재 스레드에 등록된 호스트의 고정 IP를 조회한다.
     *
     * @param host 조회할 호스트명
     * @return 고정 IP. 등록되지 않았으면 {@code null}
     */
    static InetAddress lookup(String host) {
        return PINS.get().get(host.toLowerCase());
    }

    /**
     * 현재 스레드의 모든 핀을 해제한다. fetch 종료 시 반드시 호출한다.
     */
    static void clear() {
        PINS.get().clear();
    }
}
