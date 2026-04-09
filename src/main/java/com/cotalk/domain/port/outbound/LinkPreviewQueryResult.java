package com.cotalk.domain.port.outbound;

/**
 * 링크 미리보기 조회 결과.
 *
 * @param url 원본 URL
 * @param title 제목
 * @param description 설명
 * @param imageUrl 대표 이미지 URL
 * @param domain 도메인
 * @param siteName 사이트명
 * @param favicon 파비콘 URL
 */
public record LinkPreviewQueryResult(
        String url,
        String title,
        String description,
        String imageUrl,
        String domain,
        String siteName,
        String favicon
) {
}
