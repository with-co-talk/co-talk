package com.cotalk.application.service.linkpreview;

import com.cotalk.domain.port.inbound.linkpreview.GetLinkPreviewUseCase;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * URL에서 링크 미리보기 정보를 추출하는 서비스.
 * Jsoup을 사용하여 HTML을 파싱하고 Open Graph 메타데이터를 추출한다.
 *
 * @author seunggu.lee
 */
@Service
public class GetLinkPreviewService implements GetLinkPreviewUseCase {

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
            Connection.Response response = Jsoup.connect(currentUrl)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MILLIS)
                    .followRedirects(false)
                    .execute();

            int statusCode = response.statusCode();

            // 리다이렉트 응답 (3xx)
            if (statusCode >= HTTP_REDIRECT_STATUS_MIN && statusCode < HTTP_REDIRECT_STATUS_MAX) {
                String redirectUrl = response.header("Location");
                if (redirectUrl == null || redirectUrl.isBlank()) {
                    throw new IOException("Redirect without Location header");
                }

                // 상대 URL을 절대 URL로 변환
                redirectUrl = resolveRedirectUrl(currentUrl, redirectUrl);

                // 리다이렉트 대상 URL 검증 (SSRF 방어)
                try {
                    URI uri = new URI(redirectUrl);
                    String host = uri.getHost();
                    if (host == null) {
                        throw new IOException("Invalid redirect URL: " + redirectUrl);
                    }
                    validateNotInternalHost(host);
                } catch (URISyntaxException e) {
                    throw new IOException("Invalid redirect URL: " + redirectUrl, e);
                }

                currentUrl = redirectUrl;
            } else {
                // 최종 응답 반환
                return response.parse();
            }
        }

        throw new IOException("Too many redirects (max: " + MAX_REDIRECTS + ")");
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
        // Block obvious internal hostnames
        String lowerHost = host.toLowerCase();
        if (lowerHost.equals("localhost") || lowerHost.endsWith(".local") || lowerHost.endsWith(".internal")) {
            throw new IllegalArgumentException("내부 네트워크 주소는 허용되지 않습니다.");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (address.isLoopbackAddress() ||
                    address.isSiteLocalAddress() ||
                    address.isLinkLocalAddress() ||
                    address.isAnyLocalAddress()) {
                    throw new IllegalArgumentException("내부 네트워크 주소는 허용되지 않습니다.");
                }

                // Block specific ranges (169.254.x.x cloud metadata)
                byte[] addr = address.getAddress();
                if (addr.length == IPV4_ADDRESS_LENGTH && (addr[0] & 0xFF) == IPV4_CLOUD_METADATA_FIRST_OCTET && (addr[1] & 0xFF) == IPV4_CLOUD_METADATA_SECOND_OCTET) {
                    throw new IllegalArgumentException("내부 네트워크 주소는 허용되지 않습니다.");
                }
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("호스트를 확인할 수 없습니다: " + host, e);
        }
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
}
