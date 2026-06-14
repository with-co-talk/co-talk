package com.cotalk.application.service.linkpreview;

import com.cotalk.domain.port.inbound.linkpreview.GetLinkPreviewUseCase;
import com.cotalk.domain.port.inbound.linkpreview.GetLinkPreviewUseCase.LinkPreviewResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GetLinkPreviewService 단위 테스트.
 * URL에서 Open Graph 메타데이터를 추출하는 기능을 테스트한다.
 */
class GetLinkPreviewServiceTest {

    private GetLinkPreviewUseCase getLinkPreviewUseCase;

    @BeforeEach
    void setUp() {
        getLinkPreviewUseCase = new GetLinkPreviewService();
    }

    @Test
    @DisplayName("should_URL에서_OG메타데이터_추출_when_유효한URL")
    void should_ExtractOgMetadata_when_ValidUrl() {
        // given
        String url = "https://www.naver.com";

        // when
        LinkPreviewResult result = getLinkPreviewUseCase.getLinkPreview(url);

        // then
        assertThat(result).isNotNull();
        assertThat(result.url()).isEqualTo(url);
        assertThat(result.domain()).isEqualTo("www.naver.com");
        // title, description, imageUrl은 실제 페이지에 따라 달라질 수 있음
    }

    @Test
    @DisplayName("should_HTTPS실제콘텐츠추출_when_공인HTTPS사이트 (IP핀이 SNI/인증서 깨지 않음 검증)")
    void should_ExtractRealContent_when_PublicHttpsSite() {
        // given: example.com은 안정적으로 <title>을 제공한다.
        String url = "https://example.com";

        // when
        LinkPreviewResult result = getLinkPreviewUseCase.getLinkPreview(url);

        // then: IP 핀으로 HTTPS가 깨졌다면 fetch가 swallow되어 title이 null이 된다.
        // title이 채워졌다는 것은 SNI/인증서 검증을 보존한 채 실제 콘텐츠를 받았음을 의미한다.
        assertThat(result.title()).isNotBlank();
    }

    @Test
    @DisplayName("should_도메인추출_when_URL에서")
    void should_ExtractDomain_when_FromUrl() {
        // given
        String url = "https://github.com/anthropics/claude-code";

        // when
        LinkPreviewResult result = getLinkPreviewUseCase.getLinkPreview(url);

        // then
        assertThat(result.domain()).isEqualTo("github.com");
    }

    @Test
    @DisplayName("should_빈결과반환_when_OG태그없는페이지")
    void should_ReturnEmptyFields_when_NoOgTags() {
        // given
        String url = "https://example.com";

        // when
        LinkPreviewResult result = getLinkPreviewUseCase.getLinkPreview(url);

        // then
        assertThat(result).isNotNull();
        assertThat(result.url()).isEqualTo(url);
        assertThat(result.domain()).isEqualTo("example.com");
        // title은 <title> 태그에서 폴백될 수 있음
    }

    @Test
    @DisplayName("should_예외발생_when_잘못된URL형식")
    void should_ThrowException_when_InvalidUrlFormat() {
        // given
        String invalidUrl = "not-a-valid-url";

        // when & then
        assertThatThrownBy(() -> getLinkPreviewUseCase.getLinkPreview(invalidUrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 URL");
    }

    @Test
    @DisplayName("should_예외발생_when_URL이null")
    void should_ThrowException_when_NullUrl() {
        // when & then
        assertThatThrownBy(() -> getLinkPreviewUseCase.getLinkPreview(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL");
    }

    @Test
    @DisplayName("should_예외발생_when_URL이빈문자열")
    void should_ThrowException_when_EmptyUrl() {
        // when & then
        assertThatThrownBy(() -> getLinkPreviewUseCase.getLinkPreview(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL");
    }

    @Test
    @DisplayName("should_HTTP프로토콜처리_when_httpURL")
    void should_HandleHttpProtocol_when_HttpUrl() {
        // given
        String url = "http://example.com";

        // when
        LinkPreviewResult result = getLinkPreviewUseCase.getLinkPreview(url);

        // then
        assertThat(result).isNotNull();
        assertThat(result.url()).isEqualTo(url);
    }

    @Test
    @DisplayName("should_예외발생_when_localhostURL")
    void should_ThrowException_when_LocalhostUrl() {
        // when & then
        assertThatThrownBy(() -> getLinkPreviewUseCase.getLinkPreview("http://localhost/test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("내부 네트워크 주소");
    }

    @Test
    @DisplayName("should_예외발생_when_사설IP_URL")
    void should_ThrowException_when_PrivateIpUrl() {
        // when & then
        assertThatThrownBy(() -> getLinkPreviewUseCase.getLinkPreview("http://10.0.0.1/test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("내부 네트워크 주소");
    }

    @Test
    @DisplayName("should_예외발생_when_메타데이터URL")
    void should_ThrowException_when_MetadataUrl() {
        // when & then
        assertThatThrownBy(() -> getLinkPreviewUseCase.getLinkPreview("http://169.254.169.254/latest/meta-data/"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("내부 네트워크 주소");
    }

    /**
     * IP 단위 차단 로직 단위 테스트.
     * <p>
     * Jsoup 연결을 모킹하기 어려우므로, DNS rebinding(TOCTOU) 방어의 핵심인
     * "검증에 사용한 바로 그 IP가 사설/내부 범위인지" 판정 로직을 IP 레벨에서 직접 검증한다.
     * 실제 fetch는 이 검증을 통과한 IP에 핀(pin)되어 연결되므로 검증-연결 IP가 일치한다.
     * </p>
     */
    @Nested
    @DisplayName("IP 차단 로직 (DNS rebinding 방어 핵심)")
    class IpValidation {

        private final GetLinkPreviewService service = new GetLinkPreviewService();

        private InetAddress ip(String literal) throws Exception {
            // 리터럴 IP는 DNS 조회 없이 InetAddress로 변환된다.
            return InetAddress.getByName(literal);
        }

        @Test
        @DisplayName("IPv4 루프백 127.0.0.1 차단")
        void should_block_ipv4Loopback() throws Exception {
            assertThatThrownBy(() -> service.validateIpNotInternal(ip("127.0.0.1")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("IPv4 사설 10.0.0.1 차단")
        void should_block_ipv4Private() throws Exception {
            assertThatThrownBy(() -> service.validateIpNotInternal(ip("10.0.0.1")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("IPv4 클라우드 메타데이터 169.254.169.254 차단")
        void should_block_cloudMetadata() throws Exception {
            assertThatThrownBy(() -> service.validateIpNotInternal(ip("169.254.169.254")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("IPv6 루프백 ::1 차단")
        void should_block_ipv6Loopback() throws Exception {
            assertThatThrownBy(() -> service.validateIpNotInternal(ip("::1")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("IPv6 ULA fc00::/7 차단 (fd00::1)")
        void should_block_ipv6UniqueLocal() throws Exception {
            assertThatThrownBy(() -> service.validateIpNotInternal(ip("fd00::1")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("IPv4-mapped IPv6 ::ffff:127.0.0.1 차단 (루프백 우회 방지)")
        void should_block_ipv4MappedLoopback() throws Exception {
            assertThatThrownBy(() -> service.validateIpNotInternal(ip("::ffff:127.0.0.1")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("IPv4-mapped IPv6 ::ffff:169.254.169.254 차단 (메타데이터 우회 방지)")
        void should_block_ipv4MappedMetadata() throws Exception {
            assertThatThrownBy(() -> service.validateIpNotInternal(ip("::ffff:169.254.169.254")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("공인 IPv4 8.8.8.8 통과")
        void should_allow_publicIpv4() throws Exception {
            assertThatCode(() -> service.validateIpNotInternal(ip("8.8.8.8")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("공인 IPv6 2001:4860:4860::8888 통과")
        void should_allow_publicIpv6() throws Exception {
            assertThatCode(() -> service.validateIpNotInternal(ip("2001:4860:4860::8888")))
                    .doesNotThrowAnyException();
        }
    }

    /**
     * DNS 핀(검증-연결 IP 일치) 메커니즘 단위 테스트.
     * <p>
     * fetch 직전 검증한 IP를 {@link PinnedHostResolver}에 등록하면, JVM 전역에 설치된
     * {@link PinnedHostResolverProvider}가 해당 호스트의 모든 DNS 조회를 등록 IP로만 해석한다.
     * 이로써 검증 시점과 실제 연결 시점의 IP가 항상 일치하여 DNS rebinding(TOCTOU)을 차단한다.
     * 핀이 없는 호스트는 플랫폼 리졸버로 위임되어 정상 외부 사이트 조회가 깨지지 않는다.
     * </p>
     */
    @Nested
    @DisplayName("DNS 핀 (검증-연결 IP 일치 보장)")
    class HostPinning {

        @org.junit.jupiter.api.AfterEach
        void cleanup() {
            PinnedHostResolver.clear();
        }

        @Test
        @DisplayName("핀 등록 시 InetAddress.getAllByName이 등록된 IP만 반환 (검증=연결 IP)")
        void should_resolveToPinnedIp_when_hostPinned() throws Exception {
            // given: 실제 DNS에는 존재하지 않는 가짜 호스트를 검증 IP로 고정
            String fakeHost = "pinned-fetch-target.cotalk-test.invalid";
            InetAddress validatedIp = InetAddress.getByName("203.0.113.42"); // TEST-NET-3
            PinnedHostResolver.pin(fakeHost, validatedIp);

            // when: JDK 네트워크 스택의 표준 진입점으로 조회
            InetAddress[] resolved = InetAddress.getAllByName(fakeHost);

            // then: 검증에 사용한 바로 그 IP 하나만 반환된다 (실제 연결도 이 IP로 핀됨)
            assertThat(resolved).hasSize(1);
            assertThat(resolved[0].getHostAddress()).isEqualTo("203.0.113.42");
        }

        @Test
        @DisplayName("핀 해제 후에는 미등록 호스트로 취급되어 더 이상 강제 해석되지 않음")
        void should_notResolveToPinnedIp_when_cleared() throws Exception {
            // given
            String fakeHost = "pinned-fetch-target.cotalk-test.invalid";
            PinnedHostResolver.pin(fakeHost, InetAddress.getByName("203.0.113.42"));

            // when
            PinnedHostResolver.clear();

            // then: 핀이 사라졌으므로 레지스트리 조회는 null
            assertThat(PinnedHostResolver.lookup(fakeHost)).isNull();
        }

        @Test
        @DisplayName("호스트명 대소문자 무관하게 핀이 적용됨")
        void should_pinCaseInsensitively() throws Exception {
            InetAddress validatedIp = InetAddress.getByName("198.51.100.7"); // TEST-NET-2
            PinnedHostResolver.pin("MixedCase.Example.Invalid", validatedIp);

            assertThat(PinnedHostResolver.lookup("mixedcase.example.invalid"))
                    .isEqualTo(validatedIp);
        }

        @Test
        @DisplayName("핀이 없는 정상 외부 호스트는 플랫폼 리졸버로 위임되어 정상 조회됨 (회귀 방지)")
        void should_delegateToPlatform_when_notPinned() throws Exception {
            // given: 아무 핀도 등록하지 않음
            // when & then: 잘 알려진 공인 호스트는 플랫폼 DNS로 정상 해석되어야 한다
            InetAddress[] resolved = InetAddress.getAllByName("dns.google");
            assertThat(resolved).isNotEmpty();
        }
    }
}
