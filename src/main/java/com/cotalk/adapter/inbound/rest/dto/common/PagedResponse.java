package com.cotalk.adapter.inbound.rest.dto.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이지네이션 메타데이터를 포함하는 공통 응답 래퍼.
 * 모든 페이지네이션된 API 응답에서 사용한다.
 *
 * @param <T> 컨텐츠 요소 타입
 * @param content       페이지 내 데이터 목록
 * @param page          현재 페이지 번호 (0-based)
 * @param size          페이지 크기
 * @param totalElements 전체 요소 수
 * @param totalPages    전체 페이지 수
 * @param first         첫 번째 페이지 여부
 * @param last          마지막 페이지 여부
 * @author seunggu.lee
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    /**
     * Spring Data의 Page 객체로부터 PagedResponse를 생성한다.
     *
     * @param <T>  컨텐츠 요소 타입
     * @param page Spring Data Page 객체
     * @return PagedResponse 인스턴스
     */
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    /**
     * 컨텐츠 목록과 Page 메타데이터를 조합하여 PagedResponse를 생성한다.
     * Page의 컨텐츠를 별도로 매핑한 경우 사용한다.
     *
     * @param <T>     컨텐츠 요소 타입
     * @param content 매핑된 컨텐츠 목록
     * @param page    Spring Data Page 객체 (메타데이터 소스)
     * @return PagedResponse 인스턴스
     */
    public static <T> PagedResponse<T> of(List<T> content, Page<?> page) {
        return new PagedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
