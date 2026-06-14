package com.cotalk.adapter.outbound.persistence.message;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 임의의 (offset, limit) 윈도우를 표현하는 {@link Pageable} 구현체.
 *
 * <p>표준 {@link org.springframework.data.domain.PageRequest}는 offset이
 * {@code pageNumber * pageSize}로 고정되어, over-fetch(limit을 page 크기보다 크게)와
 * page 기반 오프셋을 동시에 만족시킬 수 없다. 블라인드 인덱스 검색 1단계는
 * "사용자 page 기준 오프셋(page * size)"은 유지하면서 "윈도우만 넉넉히(over-fetch)"
 * 가져와야 page&gt;0에서도 결과 누락/중복이 발생하지 않는다. 이를 위해 offset과 limit을
 * 독립적으로 지정할 수 있는 Pageable을 제공한다.</p>
 *
 * <p>정렬은 쿼리({@code ORDER BY m.createdAt DESC})에 내장되어 있으므로 unsorted로 둔다.</p>
 *
 * @author seunggu.lee
 */
final class OffsetLimitPageable implements Pageable {

    private final long offset;
    private final int limit;

    private OffsetLimitPageable(long offset, int limit) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset은 0 이상이어야 합니다: " + offset);
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit은 1 이상이어야 합니다: " + limit);
        }
        this.offset = offset;
        this.limit = limit;
    }

    /**
     * (offset, limit) Pageable을 생성한다.
     *
     * @param offset 조회 시작 오프셋 (0 이상)
     * @param limit  조회 윈도우 크기 (1 이상)
     * @return Pageable
     */
    static OffsetLimitPageable of(long offset, int limit) {
        return new OffsetLimitPageable(offset, limit);
    }

    @Override
    public int getPageNumber() {
        return (int) (offset / limit);
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return Sort.unsorted();
    }

    @Override
    public Pageable next() {
        return new OffsetLimitPageable(offset + limit, limit);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? new OffsetLimitPageable(offset - limit, limit) : first();
    }

    @Override
    public Pageable first() {
        return new OffsetLimitPageable(0, limit);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetLimitPageable((long) pageNumber * limit, limit);
    }

    @Override
    public boolean hasPrevious() {
        return offset >= limit;
    }
}
