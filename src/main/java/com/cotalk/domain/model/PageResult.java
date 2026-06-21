package com.cotalk.domain.model;

import java.util.List;
import java.util.function.Function;

/**
 * 순수 도메인 페이지네이션 결과 값 객체.
 * Spring Data의 {@code Page}를 대체하여 도메인/애플리케이션 계층이
 * 프레임워크에 의존하지 않도록 한다. persistence 어댑터에서 Spring
 * {@code Page}로부터 변환된다.
 *
 * @param content       현재 페이지의 요소 목록
 * @param page          0부터 시작하는 현재 페이지 번호
 * @param size          페이지 크기
 * @param totalElements 전체 요소 수
 * @param <T>           요소 타입
 * @author seunggu.lee
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

    /**
     * 전체 페이지 수를 반환한다.
     *
     * @return 전체 페이지 수 (size가 0이면 0)
     */
    public int totalPages() {
        if (size == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / (double) size);
    }

    /**
     * 마지막 페이지 여부를 반환한다.
     *
     * @return 마지막 페이지이면 true
     */
    public boolean last() {
        return page + 1 >= totalPages();
    }

    /**
     * 다음 페이지 존재 여부를 반환한다.
     *
     * @return 다음 페이지가 있으면 true
     */
    public boolean hasNext() {
        return page + 1 < totalPages();
    }

    /**
     * 컨텐츠를 다른 타입으로 매핑한 새 PageResult를 반환한다.
     * 페이지 메타데이터(page/size/totalElements)는 그대로 유지된다.
     *
     * @param mapper 요소 변환 함수
     * @param <R>    변환 후 요소 타입
     * @return 매핑된 PageResult
     */
    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = content.stream().<R>map(mapper).toList();
        return new PageResult<>(mapped, page, size, totalElements);
    }
}
