package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.port.inbound.linkpreview.GetLinkPreviewUseCase;
import com.cotalk.domain.port.inbound.linkpreview.GetLinkPreviewUseCase.LinkPreviewResult;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 링크 미리보기 컨트롤러 단위 테스트.
 * <p>
 * URL에서 Open Graph 메타데이터를 추출하는 엔드포인트를 테스트한다.
 *
 * @author seunggu.lee
 */
@WebMvcTest(LinkPreviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RateLimitTestConfiguration.class)
class LinkPreviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetLinkPreviewUseCase getLinkPreviewUseCase;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("링크 미리보기 조회 API")
    class GetLinkPreviewApi {

        @Test
        @DisplayName("유효한 URL로 미리보기 정보 조회 성공")
        void should_returnOk_when_validUrl() throws Exception {
            // given
            String url = "https://www.naver.com";
            LinkPreviewResult result = new LinkPreviewResult(
                    url,
                    "NAVER",
                    "네이버 메인 페이지",
                    "https://www.naver.com/image.png",
                    "www.naver.com",
                    "NAVER",
                    "https://www.naver.com/favicon.ico"
            );
            given(getLinkPreviewUseCase.getLinkPreview(anyString())).willReturn(result);

            // when & then
            mockMvc.perform(get("/api/v1/link-preview")
                            .param("url", url))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("NAVER"))
                    .andExpect(jsonPath("$.description").value("네이버 메인 페이지"))
                    .andExpect(jsonPath("$.imageUrl").value("https://www.naver.com/image.png"))
                    .andExpect(jsonPath("$.url").value(url));
        }

        @Test
        @DisplayName("이미지가 없는 URL의 미리보기 정보 조회 성공")
        void should_returnOk_when_urlWithoutImage() throws Exception {
            // given
            String url = "https://example.com";
            LinkPreviewResult result = new LinkPreviewResult(
                    url,
                    "Example Domain",
                    "This domain is for use in illustrative examples",
                    null,
                    "example.com",
                    null,
                    null
            );
            given(getLinkPreviewUseCase.getLinkPreview(anyString())).willReturn(result);

            // when & then
            mockMvc.perform(get("/api/v1/link-preview")
                            .param("url", url))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Example Domain"))
                    .andExpect(jsonPath("$.description").value("This domain is for use in illustrative examples"))
                    .andExpect(jsonPath("$.imageUrl").isEmpty())
                    .andExpect(jsonPath("$.url").value(url));
        }

        @Test
        @DisplayName("최소 정보만 있는 URL의 미리보기 조회 성공")
        void should_returnOk_when_minimalMetadata() throws Exception {
            // given
            String url = "https://simple.example.com";
            LinkPreviewResult result = new LinkPreviewResult(
                    url,
                    "Simple Page",
                    null,
                    null,
                    "simple.example.com",
                    null,
                    null
            );
            given(getLinkPreviewUseCase.getLinkPreview(anyString())).willReturn(result);

            // when & then
            mockMvc.perform(get("/api/v1/link-preview")
                            .param("url", url))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Simple Page"))
                    .andExpect(jsonPath("$.description").isEmpty())
                    .andExpect(jsonPath("$.imageUrl").isEmpty())
                    .andExpect(jsonPath("$.url").value(url));
        }
    }
}
