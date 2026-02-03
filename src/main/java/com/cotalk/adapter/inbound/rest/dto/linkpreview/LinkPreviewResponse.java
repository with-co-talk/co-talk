package com.cotalk.adapter.inbound.rest.dto.linkpreview;

import com.cotalk.domain.port.inbound.linkpreview.GetLinkPreviewUseCase.LinkPreviewResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 링크 미리보기 응답 DTO.
 *
 * @param url         원본 URL
 * @param title       페이지 제목
 * @param description 페이지 설명
 * @param imageUrl    대표 이미지 URL
 * @param domain      도메인명
 * @param siteName    사이트명
 * @param favicon     파비콘 URL
 */
@Schema(description = "링크 미리보기 정보")
public record LinkPreviewResponse(
        @Schema(description = "원본 URL", example = "https://www.naver.com")
        String url,

        @Schema(description = "페이지 제목", example = "NAVER")
        String title,

        @Schema(description = "페이지 설명", example = "네이버 메인에서 다양한 정보와 유용한 컨텐츠를 만나 보세요")
        String description,

        @Schema(description = "대표 이미지 URL", example = "https://www.naver.com/og-image.png")
        String imageUrl,

        @Schema(description = "도메인명", example = "www.naver.com")
        String domain,

        @Schema(description = "사이트명", example = "네이버")
        String siteName,

        @Schema(description = "파비콘 URL", example = "https://www.naver.com/favicon.ico")
        String favicon
) {
    /**
     * UseCase 결과를 응답 DTO로 변환한다.
     *
     * @param result UseCase 결과
     * @return 응답 DTO
     */
    public static LinkPreviewResponse from(LinkPreviewResult result) {
        return new LinkPreviewResponse(
                result.url(),
                result.title(),
                result.description(),
                result.imageUrl(),
                result.domain(),
                result.siteName(),
                result.favicon()
        );
    }
}
