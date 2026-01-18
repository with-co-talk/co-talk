package com.cotalk.domain.port.outbound;

/**
 * ID 생성기 포트.
 * 엔티티의 고유 ID를 생성하기 위한 인터페이스를 정의한다.
 *
 * <p>이 포트는 Hexagonal Architecture에서 도메인이 인프라에 의존하지 않도록
 * ID 생성 로직을 추상화한다.
 *
 * @author seunggu.lee
 */
public interface IdGenerator {

    /**
     * 새로운 고유 ID를 생성한다.
     *
     * @return 생성된 고유 ID
     */
    Long nextId();
}
