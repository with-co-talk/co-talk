package com.cotalk.domain.port.inbound.linkpreview;

/**
 * URL에서 링크 미리보기 정보를 조회하는 UseCase.
 * Open Graph 메타데이터를 추출하여 미리보기 정보를 제공한다.
 *
 * @author seunggu.lee
 */
public interface GetLinkPreviewUseCase {

    /**
     * 주어진 URL에서 링크 미리보기 정보를 추출한다.
     *
     * @param url 미리보기 정보를 추출할 URL
     * @return 링크 미리보기 결과
     * @throws IllegalArgumentException URL이 유효하지 않은 경우
     */
    LinkPreviewResult getLinkPreview(String url);

    /**
     * 링크 미리보기 결과.
     *
     * @param url         원본 URL
     * @param title       페이지 제목 (og:title 또는 title 태그)
     * @param description 페이지 설명 (og:description 또는 meta description)
     * @param imageUrl    대표 이미지 URL (og:image)
     * @param domain      도메인명
     * @param siteName    사이트명 (og:site_name)
     * @param favicon     파비콘 URL
     */
    record LinkPreviewResult(
            String url,
            String title,
            String description,
            String imageUrl,
            String domain,
            String siteName,
            String favicon
    ) {
        /**
         * 빈 결과를 생성한다.
         *
         * @param url    원본 URL
         * @param domain 도메인명
         * @return 빈 미리보기 결과
         */
        public static LinkPreviewResult empty(String url, String domain) {
            return new LinkPreviewResult(url, null, null, null, domain, null, null);
        }
    }
}
