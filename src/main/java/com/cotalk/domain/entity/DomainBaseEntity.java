package com.cotalk.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 순수 도메인 엔티티용 공통 필드.
 * JPA 매핑 없이 createdAt, updatedAt만 보관한다.
 * persistence 계층으로 완전 분리된 엔티티(User 등)가 사용한다.
 *
 * @author seunggu.lee
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class DomainBaseEntity {

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
