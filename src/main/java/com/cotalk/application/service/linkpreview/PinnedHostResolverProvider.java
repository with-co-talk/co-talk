package com.cotalk.application.service.linkpreview;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.spi.InetAddressResolver;
import java.net.spi.InetAddressResolverProvider;
import java.util.stream.Stream;

/**
 * JVM 전역에 설치되는 위임형(delegating) DNS 리졸버 프로바이더.
 * <p>
 * {@code META-INF/services/java.net.spi.InetAddressResolverProvider}로 등록되어 JVM 시작 시
 * 1회 설치된다. 기본 동작은 플랫폼 내장 리졸버로의 완전 위임이며, 오직
 * {@link PinnedHostResolver}에 현재 스레드 기준으로 핀이 등록된 호스트에 한해서만
 * 등록된 검증 IP 하나만 반환한다.
 * </p>
 * <p>
 * 이로써 링크프리뷰 fetch는 원본 호스트명 URL을 그대로 사용할 수 있고(가상 호스팅 Host 헤더와
 * TLS SNI/인증서 검증이 JDK 기본 경로로 정상 동작), 동시에 실제 연결 IP는 사전 검증된 IP로
 * 강제 고정되어 DNS rebinding(TOCTOU)을 차단한다. 핀이 없는 모든 호스트의 조회는 플랫폼
 * 리졸버로 그대로 위임되므로 애플리케이션의 다른 네트워크 동작에는 영향을 주지 않는다.
 * </p>
 *
 * @author seunggu.lee
 */
public final class PinnedHostResolverProvider extends InetAddressResolverProvider {

    /**
     * {@inheritDoc}
     * <p>
     * 핀이 등록된 호스트는 등록 IP로, 그 외에는 플랫폼 내장 리졸버로 위임하는 리졸버를 반환한다.
     * </p>
     */
    @Override
    public InetAddressResolver get(Configuration configuration) {
        InetAddressResolver platform = configuration.builtinResolver();
        return new InetAddressResolver() {
            @Override
            public Stream<InetAddress> lookupByName(String host, LookupPolicy lookupPolicy)
                    throws UnknownHostException {
                InetAddress pinned = PinnedHostResolver.lookup(host);
                if (pinned != null) {
                    return Stream.of(pinned);
                }
                return platform.lookupByName(host, lookupPolicy);
            }

            @Override
            public String lookupByAddress(byte[] addr) throws UnknownHostException {
                return platform.lookupByAddress(addr);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return "cotalk-pinned-host-resolver";
    }
}
