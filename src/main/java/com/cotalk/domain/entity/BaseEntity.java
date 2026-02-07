package com.cotalk.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * JPA 매핑이 필요한 엔티티용 공통 베이스.
 * 아직 persistence 계층으로 분리되지 않은 엔티티(Message, ChatRoom 등)가 사용한다.
 * 분리 완료 후 제거하고, 순수 도메인용 BaseEntity만 유지할 예정이다.
 *
 * @author seunggu.lee
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
