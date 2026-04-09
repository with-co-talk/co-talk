package com.cotalk.domain.port.outbound;

/**
 * 링크 미리보기 조회를 다른 도메인 모듈에서 요청할 때 사용하는 아웃바운드 포트.
 *
 * @author seunggu.lee
 */
public interface LinkPreviewQueryPort {

    /**
     * 주어진 URL의 링크 미리보기를 조회한다.
     *
     * @param url 조회할 URL
     * @return 링크 미리보기 결과
     */
    LinkPreviewQueryResult queryLinkPreview(String url);
}
