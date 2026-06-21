package com.cotalk.domain.model;

/**
 * 순수 도메인 페이지네이션 요청 값 객체.
 * Spring Data의 {@code Pageable}을 대체하여 도메인/애플리케이션 계층이
 * 프레임워크에 의존하지 않도록 한다. persistence 어댑터에서 Spring
 * {@code PageRequest}로 변환된다.
 *
 * @param page 0부터 시작하는 페이지 번호
 * @param size 페이지 크기 (1 이상)
 * @author seunggu.lee
 */
public record PageQuery(int page, int size) {

    /**
     * PageQuery를 생성한다.
     *
     * @param page 0부터 시작하는 페이지 번호
     * @param size 페이지 크기
     * @throws IllegalArgumentException page가 음수이거나 size가 1 미만인 경우
     */
    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }
        if (size < 1) {
            throw new IllegalArgumentException("페이지 크기는 1 이상이어야 합니다.");
        }
    }

    /**
     * 페이지/크기로 PageQuery를 생성한다.
     *
     * @param page 0부터 시작하는 페이지 번호
     * @param size 페이지 크기
     * @return PageQuery 인스턴스
     */
    public static PageQuery of(int page, int size) {
        return new PageQuery(page, size);
    }
}
