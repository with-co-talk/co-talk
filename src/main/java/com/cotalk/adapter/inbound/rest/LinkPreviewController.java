package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.linkpreview.LinkPreviewResponse;
import com.cotalk.domain.port.inbound.linkpreview.GetLinkPreviewUseCase;
import com.cotalk.domain.port.inbound.linkpreview.GetLinkPreviewUseCase.LinkPreviewResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 링크 미리보기를 위한 REST 컨트롤러.
 * URL에서 Open Graph 메타데이터를 추출하여 미리보기 정보를 제공한다.
 *
 * @author seunggu.lee
 */
@Tag(name = "Link Preview", description = "링크 미리보기 API")
@RestController
@RequestMapping("/api/v1/link-preview")
@RequiredArgsConstructor
public class LinkPreviewController {

    private final GetLinkPreviewUseCase getLinkPreviewUseCase;

    /**
     * URL에서 링크 미리보기 정보를 조회한다.
     *
     * @param url 미리보기 정보를 조회할 URL
     * @return 링크 미리보기 정보
     */
    @Operation(
            summary = "링크 미리보기 조회",
            description = "URL에서 Open Graph 메타데이터(제목, 설명, 이미지 등)를 추출하여 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "미리보기 정보 조회 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 URL 형식")
    @GetMapping
    public ResponseEntity<LinkPreviewResponse> getLinkPreview(
            @Parameter(description = "미리보기 정보를 조회할 URL", required = true, example = "https://www.naver.com")
            @RequestParam("url") String url) {

        LinkPreviewResult result = getLinkPreviewUseCase.getLinkPreview(url);
        return ResponseEntity.ok(LinkPreviewResponse.from(result));
    }
}
