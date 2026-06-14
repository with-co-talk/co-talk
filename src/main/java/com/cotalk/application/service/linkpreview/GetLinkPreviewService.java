package com.cotalk.application.service.linkpreview;

import com.cotalk.domain.port.inbound.linkpreview.GetLinkPreviewUseCase;
import com.cotalk.domain.port.outbound.LinkPreviewQueryPort;
import com.cotalk.domain.port.outbound.LinkPreviewQueryResult;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

/**
 * URL에서 링크 미리보기 정보를 추출하는 서비스.
 * Jsoup을 사용하여 HTML을 파싱하고 Open Graph 메타데이터를 추출한다.
 *
 * @author seunggu.lee
 */
@Service
public class GetLinkPreviewService implements GetLinkPreviewUseCase, LinkPreviewQueryPort {

    @Override
    public LinkPreviewQueryResult queryLinkPreview(String url) {
        LinkPreviewResult result = getLinkPreview(url);
        return new LinkPreviewQueryResult(
                result.url(),
                result.title(),
                result.description(),
                result.imageUrl(),
                result.domain(),
                result.siteName(),
                result.favicon()
        );
    }

    private static final int TIMEOUT_MILLIS = 5000;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; CoTalkBot/1.0; +https://cotalk.com)";
    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final int DESCRIPTION_TRUNCATION_OFFSET = 197;
    private static final int HTTP_REDIRECT_STATUS_MIN = 300;
    private static final int HTTP_REDIRECT_STATUS_MAX = 400;
    private static final int IPV4_CLOUD_METADATA_FIRST_OCTET = 169;
    private static final int IPV4_CLOUD_METADATA_SECOND_OCTET = 254;
    private static final int IPV4_ADDRESS_LENGTH = 4;
    private static final int IPV6_ADDRESS_LENGTH = 16;
    private static final int IPV4_LOOPBACK_FIRST_OCTET = 127;
    private static final int IPV4_PRIVATE_10 = 10;
    private static final int IPV4_PRIVATE_172 = 172;
    private static final int IPV4_PRIVATE_172_LOW = 16;
    private static final int IPV4_PRIVATE_172_HIGH = 31;
    private static final int IPV4_PRIVATE_192 = 192;
    private static final int IPV4_PRIVATE_192_SECOND = 168;
    /** IPv6 ULA(fc00::/7) 판정: 최상위 7비트(0xFE 마스크)가 0xFC인지 확인 */
    private static final int IPV6_ULA_MASK = 0xFE;
    private static final int IPV6_ULA_PREFIX = 0xFC;
    private static final String ELLIPSIS = "...";
    /** HTML meta 태그의 content 속성명 */
    private static final String META_ATTR_CONTENT = "content";

    /**
     * {@inheritDoc}
     */
    @Override
    public LinkPreviewResult getLinkPreview(String url) {
        validateUrl(url);

        String domain = extractDomain(url);

        try {
            Document doc = fetchWithSafeRedirects(url);

            String title = extractTitle(doc);
            String description = extractDescription(doc);
            String imageUrl = extractImageUrl(doc, url);
            String siteName = extractMetaContent(doc, "og:site_name");
            String favicon = extractFavicon(doc, url);

            return new LinkPreviewResult(
                    url,
                    title,
                    description,
                    imageUrl,
                    domain,
                    siteName,
                    favicon
            );
        } catch (Exception _) {
            // 연결 실패 시 기본 정보만 반환
            return LinkPreviewResult.empty(url, domain);
        }
    }

    /**
     * SSRF 방어가 적용된 안전한 리다이렉트 처리.
     * 각 리다이렉트 대상 URL을 재검증하여 내부 네트워크로의 우회를 차단한다.
     *
     * @param url 접근할 초기 URL
     * @return 파싱된 HTML Document
     * @throws IOException 연결 실패, 리다이렉트 초과, 또는 내부 네트워크 접근 시도 시
     */
    private Document fetchWithSafeRedirects(String url) throws IOException {
        String currentUrl = url;

        for (int i = 0; i <= MAX_REDIRECTS; i++) {
            // 연결 직전에 호스트를 1회만 DNS 조회·검증하고, 검증에 사용한 바로 그 IP로
            // 연결을 핀(pin)한다. 이로써 검증 시점 IP와 실제 연결 IP가 동일해지므로
            // 공격자 DNS가 검증/연결에 서로 다른 IP를 반환하는 DNS rebinding(TOCTOU)을 차단한다.
            Connection.Response response = executePinned(currentUrl);

            int statusCode = response.statusCode();

            // 리다이렉트 응답 (3xx)
            if (statusCode >= HTTP_REDIRECT_STATUS_MIN && statusCode < HTTP_REDIRECT_STATUS_MAX) {
                String redirectUrl = response.header("Location");
                if (redirectUrl == null || redirectUrl.isBlank()) {
                    throw new IOException("Redirect without Location header");
                }

                // 상대 URL을 절대 URL로 변환
                redirectUrl = resolveRedirectUrl(currentUrl, redirectUrl);

                // 리다이렉트 대상 스킴 검증 (http/https만 허용 — file:, gopher: 등 차단)
                String redirectScheme = URI.create(redirectUrl).getScheme();
                if (redirectScheme == null
                        || (!redirectScheme.equalsIgnoreCase("http") && !redirectScheme.equalsIgnoreCase("https"))) {
                    throw new IOException("Unsupported redirect scheme: " + redirectUrl);
                }

                // 다음 루프에서 executePinned가 리다이렉트 대상 호스트를 다시 검증·핀한다.
                currentUrl = redirectUrl;
            } else {
                // 최종 응답 반환
                return response.parse();
            }
        }

        throw new IOException("Too many redirects (max: " + MAX_REDIRECTS + ")");
    }

    /**
     * 호스트를 1회 DNS 조회·검증한 뒤, 검증에 사용한 그 IP로 연결을 고정(pin)하여 요청을 실행한다.
     * <p>
     * 연결 IP를 검증된 IP로 고정하기 때문에, 검증과 실제 fetch 사이에 DNS 응답이 바뀌어도
     * 내부 주소로 우회되지 않는다(DNS rebinding 방어). 검증된 IP 리터럴로 연결하되
     * {@code Host} 헤더에는 원본 호스트를 보존하여 가상 호스팅·TLS SNI 동작을 유지한다.
     * </p>
     *
     * @param targetUrl 요청할 URL
     * @return Jsoup 응답
     * @throws IOException 연결 실패 또는 내부 주소 접근 시도 시
     */
    private Connection.Response executePinned(String targetUrl) throws IOException {
        URI uri;
        try {
            uri = new URI(targetUrl);
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URL: " + targetUrl, e);
        }

        String host = uri.getHost();
        String scheme = uri.getScheme();
        if (host == null || scheme == null) {
            throw new IOException("Invalid URL (no host/scheme): " + targetUrl);
        }

        // 호스트명 기반 1차 차단(localhost 등) + DNS 조회 후 검증된 단일 IP 확보.
        InetAddress pinnedIp = resolveAndValidate(host);

        if (scheme.equalsIgnoreCase("https")) {
            // HTTPS: 원본 호스트명 URL을 유지하여 SNI·인증서 검증을 보존하되,
            // 소켓 연결만 검증된 IP로 고정하는 SSLSocketFactory를 주입한다(연결 IP 핀).
            return Jsoup.connect(targetUrl)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MILLIS)
                    .followRedirects(false)
                    .ignoreContentType(true)
                    .sslSocketFactory(new PinnedSslSocketFactory(
                            (SSLSocketFactory) SSLSocketFactory.getDefault(), pinnedIp, TIMEOUT_MILLIS))
                    .execute();
        }

        // HTTP: 검증된 IP 리터럴로 URL을 재구성하여 연결을 고정하고, Host 헤더로 원본 호스트 보존.
        String ipLiteral = pinnedIp.getHostAddress();
        // IPv6 리터럴은 대괄호로 감싼다.
        String ipAuthority = ipLiteral.contains(":") ? "[" + ipLiteral + "]" : ipLiteral;
        if (uri.getPort() != -1) {
            ipAuthority = ipAuthority + ":" + uri.getPort();
        }

        String pathAndQuery = (uri.getRawPath() == null ? "" : uri.getRawPath())
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        String pinnedUrl = scheme + "://" + ipAuthority + pathAndQuery;

        String hostHeader = host + (uri.getPort() != -1 ? ":" + uri.getPort() : "");

        return Jsoup.connect(pinnedUrl)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MILLIS)
                .followRedirects(false)
                .ignoreContentType(true)
                .header("Host", hostHeader)
                .execute();
    }

    /**
     * 호스트를 1회 DNS 조회하고 모든 결과 IP를 내부 주소 차단 규칙으로 검증한 뒤,
     * 연결에 사용할 검증된 IP 하나를 반환한다.
     *
     * @param host 검증할 호스트명
     * @return 검증을 통과한 연결 대상 IP
     * @throws IllegalArgumentException 내부 네트워크 주소인 경우
     */
    private InetAddress resolveAndValidate(String host) {
        // Block obvious internal hostnames
        String lowerHost = host.toLowerCase();
        if (lowerHost.equals("localhost") || lowerHost.endsWith(".local") || lowerHost.endsWith(".internal")) {
            throw new IllegalArgumentException("내부 네트워크 주소는 허용되지 않습니다.");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                validateIpNotInternal(address);
            }
            // 모든 IP가 검증을 통과했으므로 첫 번째 IP로 연결을 고정한다.
            return addresses[0];
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("호스트를 확인할 수 없습니다: " + host, e);
        }
    }

    /**
     * 리다이렉트 URL을 절대 경로로 변환한다.
     * 상대 경로, 프로토콜 상대 경로, 절대 경로를 모두 처리한다.
     *
     * @param baseUrl     현재 URL
     * @param redirectUrl Location 헤더의 리다이렉트 URL
     * @return 절대 URL
     * @throws MalformedURLException URL 형식이 잘못된 경우
     */
    private String resolveRedirectUrl(String baseUrl, String redirectUrl) throws MalformedURLException {
        // 이미 절대 URL인 경우
        if (redirectUrl.startsWith("http://") || redirectUrl.startsWith("https://")) {
            return redirectUrl;
        }

        URL base = URI.create(baseUrl).toURL();

        // 프로토콜 상대 URL (//example.com/path)
        if (redirectUrl.startsWith("//")) {
            return base.getProtocol() + ":" + redirectUrl;
        }

        // 절대 경로 (/path)
        if (redirectUrl.startsWith("/")) {
            return base.getProtocol() + "://" + base.getHost() +
                   (base.getPort() != -1 ? ":" + base.getPort() : "") + redirectUrl;
        }

        // 상대 경로 (path 또는 ./path 또는 ../path)
        try {
            return base.toURI().resolve(redirectUrl).toURL().toString();
        } catch (URISyntaxException e) {
            throw new MalformedURLException("Invalid redirect URL: " + e.getMessage());
        }
    }

    /**
     * URL 유효성을 검증한다.
     *
     * @param url 검증할 URL
     * @throws IllegalArgumentException URL이 유효하지 않은 경우
     */
    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL은 필수입니다.");
        }

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("유효하지 않은 URL 형식입니다: " + url);
            }
            String host = uri.getHost();
            if (host == null) {
                throw new IllegalArgumentException("유효하지 않은 URL 형식입니다: " + url);
            }

            // Block private/internal hostnames and IPs
            validateNotInternalHost(host);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("유효하지 않은 URL 형식입니다: " + url, e);
        }
    }

    /**
     * 내부/사설 네트워크 호스트를 차단한다.
     * DNS 확인을 수행하여 실제 IP가 사설 범위에 속하는지 검증한다.
     *
     * @param host 검증할 호스트명
     * @throws IllegalArgumentException 내부 네트워크 주소인 경우
     */
    private void validateNotInternalHost(String host) {
        // 호스트명 기반 차단 + DNS 검증을 resolveAndValidate에 위임한다.
        resolveAndValidate(host);
    }

    /**
     * 단일 IP가 내부/사설 네트워크 범위에 속하는지 검증한다.
     * <p>
     * DNS rebinding(TOCTOU) 방어의 핵심 판정 로직이다. 실제 연결은 이 검증을 통과한
     * 바로 그 IP에 고정되므로, 검증 시점과 연결 시점의 IP가 항상 일치한다.
     * </p>
     * <p>
     * 차단 범위:
     * <ul>
     *   <li>루프백/사이트로컬(사설)/링크로컬/와일드카드(0.0.0.0, ::)</li>
     *   <li>IPv4 169.254.0.0/16 클라우드 메타데이터</li>
     *   <li>IPv6 ULA fc00::/7 (fc00::~fdff::)</li>
     *   <li>IPv4-mapped IPv6(::ffff:a.b.c.d)는 내부의 IPv4로 환원하여 동일 규칙 적용</li>
     * </ul>
     * </p>
     *
     * @param address 검증할 IP 주소
     * @throws IllegalArgumentException 내부 네트워크 주소인 경우
     */
    void validateIpNotInternal(InetAddress address) {
        if (address.isLoopbackAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress()
                || address.isAnyLocalAddress()) {
            throw new IllegalArgumentException("내부 네트워크 주소는 허용되지 않습니다.");
        }

        byte[] addr = address.getAddress();

        // IPv4-mapped IPv6 (::ffff:a.b.c.d)는 16바이트지만 마지막 4바이트가 실제 IPv4다.
        // 매핑된 IPv4를 환원해 IPv4 규칙(메타데이터 등)을 동일 적용한다.
        byte[] effectiveV4 = null;
        if (addr.length == IPV4_ADDRESS_LENGTH) {
            effectiveV4 = addr;
        } else if (addr.length == IPV6_ADDRESS_LENGTH && isIpv4Mapped(addr)) {
            effectiveV4 = new byte[]{addr[12], addr[13], addr[14], addr[15]};
        }

        if (effectiveV4 != null) {
            int first = effectiveV4[0] & 0xFF;
            int second = effectiveV4[1] & 0xFF;
            // 169.254.0.0/16 클라우드 메타데이터(및 IPv4 링크로컬)
            if (first == IPV4_CLOUD_METADATA_FIRST_OCTET && second == IPV4_CLOUD_METADATA_SECOND_OCTET) {
                throw new IllegalArgumentException("내부 네트워크 주소는 허용되지 않습니다.");
            }
            // 127.0.0.0/8 루프백 (::ffff:127.x 처럼 isLoopbackAddress가 놓치는 케이스 보강)
            if (first == IPV4_LOOPBACK_FIRST_OCTET) {
                throw new IllegalArgumentException("내부 네트워크 주소는 허용되지 않습니다.");
            }
            // 10/8, 172.16/12, 192.168/16 사설 (IPv4-mapped 환원 케이스 보강)
            if (first == IPV4_PRIVATE_10
                    || (first == IPV4_PRIVATE_172 && second >= IPV4_PRIVATE_172_LOW && second <= IPV4_PRIVATE_172_HIGH)
                    || (first == IPV4_PRIVATE_192 && second == IPV4_PRIVATE_192_SECOND)) {
                throw new IllegalArgumentException("내부 네트워크 주소는 허용되지 않습니다.");
            }
        }

        // IPv6 ULA fc00::/7 (최상위 7비트가 1111 110 → 0xFC/0xFD)
        if (addr.length == IPV6_ADDRESS_LENGTH && (addr[0] & IPV6_ULA_MASK) == IPV6_ULA_PREFIX) {
            throw new IllegalArgumentException("내부 네트워크 주소는 허용되지 않습니다.");
        }
    }

    /**
     * IPv4-mapped IPv6 주소(::ffff:0:0/96)인지 판정한다.
     *
     * @param addr 16바이트 IPv6 주소
     * @return IPv4-mapped이면 true
     */
    private boolean isIpv4Mapped(byte[] addr) {
        for (int i = 0; i < 10; i++) {
            if (addr[i] != 0x00) {
                return false;
            }
        }
        return (addr[10] & 0xFF) == 0xFF && (addr[11] & 0xFF) == 0xFF;
    }

    /**
     * URL에서 도메인을 추출한다.
     *
     * @param url 도메인을 추출할 URL
     * @return 도메인명
     */
    private String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * 페이지 제목을 추출한다.
     * og:title -> title 태그 순으로 폴백한다.
     *
     * @param doc HTML 문서
     * @return 페이지 제목
     */
    private String extractTitle(Document doc) {
        String ogTitle = extractMetaContent(doc, "og:title");
        if (ogTitle != null && !ogTitle.isBlank()) {
            return ogTitle;
        }

        // og:title이 없으면 title 태그 사용 (Jsoup Document#title()은 null을 반환하지 않음)
        String title = doc.title();
        return title.isBlank() ? null : title;
    }

    /**
     * 페이지 설명을 추출한다.
     * og:description -> meta description 순으로 폴백한다.
     *
     * @param doc HTML 문서
     * @return 페이지 설명
     */
    private String extractDescription(Document doc) {
        String ogDescription = extractMetaContent(doc, "og:description");
        if (ogDescription != null && !ogDescription.isBlank()) {
            return truncateDescription(ogDescription);
        }

        // og:description이 없으면 meta description 사용
        Element metaDesc = doc.selectFirst("meta[name=description]");
        if (metaDesc != null) {
            String content = metaDesc.attr(META_ATTR_CONTENT);
            if (!content.isBlank()) {
                return truncateDescription(content);
            }
        }

        return null;
    }

    /**
     * 설명 텍스트를 적절한 길이로 자른다.
     *
     * @param description 원본 설명
     * @return 잘린 설명 (최대 200자)
     */
    private String truncateDescription(String description) {
        if (description == null) {
            return null;
        }
        if (description.length() <= MAX_DESCRIPTION_LENGTH) {
            return description;
        }
        return description.substring(0, DESCRIPTION_TRUNCATION_OFFSET) + ELLIPSIS;
    }

    /**
     * 대표 이미지 URL을 추출한다.
     *
     * @param doc     HTML 문서
     * @param baseUrl 기본 URL (상대 경로 변환용)
     * @return 이미지 URL
     */
    private String extractImageUrl(Document doc, String baseUrl) {
        String ogImage = extractMetaContent(doc, "og:image");
        if (ogImage != null && !ogImage.isBlank()) {
            return resolveUrl(ogImage, baseUrl);
        }

        // og:image가 없으면 twitter:image 시도
        String twitterImage = extractMetaContent(doc, "twitter:image");
        if (twitterImage != null && !twitterImage.isBlank()) {
            return resolveUrl(twitterImage, baseUrl);
        }

        return null;
    }

    /**
     * 파비콘 URL을 추출한다.
     *
     * @param doc     HTML 문서
     * @param baseUrl 기본 URL (상대 경로 변환용)
     * @return 파비콘 URL
     */
    private String extractFavicon(Document doc, String baseUrl) {
        // link[rel=icon] 또는 link[rel="shortcut icon"]
        Elements icons = doc.select("link[rel~=(?i)^(shortcut )?icon]");
        if (!icons.isEmpty()) {
            Element firstIcon = icons.get(0);
            String href = firstIcon.attr("href");
            if (!href.isBlank()) {
                return resolveUrl(href, baseUrl);
            }
        }

        // 기본 favicon.ico 경로
        try {
            URI uri = new URI(baseUrl);
            return uri.getScheme() + "://" + uri.getHost() + "/favicon.ico";
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Open Graph 메타 태그 내용을 추출한다.
     *
     * @param doc      HTML 문서
     * @param property OG 속성명 (예: og:title)
     * @return 메타 태그 내용
     */
    private String extractMetaContent(Document doc, String property) {
        Element meta = doc.selectFirst("meta[property=" + property + "]");
        if (meta != null) {
            return meta.attr(META_ATTR_CONTENT);
        }

        // property 대신 name으로 시도 (일부 사이트)
        meta = doc.selectFirst("meta[name=" + property + "]");
        if (meta != null) {
            return meta.attr(META_ATTR_CONTENT);
        }

        return null;
    }

    /**
     * 상대 URL을 절대 URL로 변환한다.
     *
     * @param url     변환할 URL
     * @param baseUrl 기본 URL
     * @return 절대 URL
     */
    private String resolveUrl(String url, String baseUrl) {
        if (url == null || url.isBlank()) {
            return null;
        }

        // 이미 절대 URL인 경우
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }

        // 프로토콜 상대 URL
        if (url.startsWith("//")) {
            try {
                URI baseUri = new URI(baseUrl);
                return baseUri.getScheme() + ":" + url;
            } catch (URISyntaxException e) {
                return "https:" + url;
            }
        }

        // 상대 경로
        try {
            URI baseUri = new URI(baseUrl);
            return baseUri.resolve(url).toString();
        } catch (URISyntaxException e) {
            return url;
        }
    }

    /**
     * HTTPS 연결을 검증된 IP에 고정(pin)하는 {@link SSLSocketFactory}.
     * <p>
     * 소켓의 TCP 연결은 사전 검증된 {@code pinnedIp}로만 맺고, TLS 핸드셰이크의
     * SNI/인증서 호스트명 검증에는 원본 호스트명을 사용한다. 이로써 검증 시점과 연결
     * 시점의 IP가 동일하게 유지되어 DNS rebinding을 차단하면서도 인증서 검증은 정상 동작한다.
     * </p>
     */
    private static final class PinnedSslSocketFactory extends SSLSocketFactory {

        private final SSLSocketFactory delegate;
        private final InetAddress pinnedIp;
        private final int connectTimeoutMillis;

        private PinnedSslSocketFactory(SSLSocketFactory delegate, InetAddress pinnedIp, int connectTimeoutMillis) {
            this.delegate = delegate;
            this.pinnedIp = pinnedIp;
            this.connectTimeoutMillis = connectTimeoutMillis;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        /**
         * 검증된 IP로 TCP 연결을 맺고, 원본 호스트명으로 TLS 핸드셰이크(SNI 포함)를 수행하는 소켓을 만든다.
         *
         * @param host 원본 호스트명 (SNI·인증서 검증용)
         * @param port 포트
         * @return 검증된 IP에 고정된 SSL 소켓
         * @throws IOException 연결 실패 시
         */
        private SSLSocket createPinnedSocket(String host, int port) throws IOException {
            Socket underlying = new Socket();
            underlying.connect(new InetSocketAddress(pinnedIp, port), connectTimeoutMillis);
            SSLSocket sslSocket = (SSLSocket) delegate.createSocket(underlying, host, port, true);
            SSLParameters params = sslSocket.getSSLParameters();
            params.setServerNames(List.of(new SNIHostName(host)));
            sslSocket.setSSLParameters(params);
            return sslSocket;
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return createPinnedSocket(host, port);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return createPinnedSocket(host, port);
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return createPinnedSocket(host.getHostName(), port);
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
                throws IOException {
            return createPinnedSocket(address.getHostName(), port);
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            // 기존 소켓은 무시하고 검증된 IP로 새로 연결한다(연결 IP 고정 보장).
            return createPinnedSocket(host, port);
        }
    }
}
