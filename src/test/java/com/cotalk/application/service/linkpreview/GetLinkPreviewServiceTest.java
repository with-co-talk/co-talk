package com.cotalk.application.service.linkpreview;

import com.cotalk.domain.port.inbound.linkpreview.GetLinkPreviewUseCase;
import com.cotalk.domain.port.inbound.linkpreview.GetLinkPreviewUseCase.LinkPreviewResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
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
}
