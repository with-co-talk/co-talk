package com.cotalk.domain.entity;

import com.cotalk.adapter.outbound.persistence.entity.MessageJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import com.cotalk.infrastructure.config.JpaAuditingConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JPA 감사(BaseJpaEntity) 자동 설정 검증.
 * 감사 필드는 persistence 계층의 BaseJpaEntity로 이관되었으며,
 * 대표 JPA 엔티티(MessageJpaEntity)로 동작을 확인한다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
@DisplayName("BaseJpaEntity JPA Auditing")
class BaseEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("엔티티 저장 시 createdAt과 updatedAt이 자동 설정된다")
    void should_SetCreatedAtAndUpdatedAt_when_EntityPersisted() {
        // given - MessageJpaEntity는 BaseJpaEntity(감사)를 상속한다
        MessageJpaEntity message = MessageJpaEntity.builder()
                .id(1L)
                .chatRoomId(1L)
                .senderId(1L)
                .content("test")
                .build();

        // when
        entityManager.persistAndFlush(message);
        entityManager.clear();

        MessageJpaEntity found = entityManager.find(MessageJpaEntity.class, 1L);

        // then
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("엔티티 수정 시 updatedAt이 갱신된다")
    void should_UpdateUpdatedAt_when_EntityUpdated() throws InterruptedException {
        // given
        MessageJpaEntity message = MessageJpaEntity.builder()
                .id(2L)
                .chatRoomId(1L)
                .senderId(1L)
                .content("test")
                .build();
        entityManager.persistAndFlush(message);
        entityManager.clear();

        MessageJpaEntity found = entityManager.find(MessageJpaEntity.class, 2L);
        var originalUpdatedAt = found.getUpdatedAt();

        // when
        Thread.sleep(10);
        found.setUpdatedAt(found.getUpdatedAt());
        entityManager.persistAndFlush(found);
        entityManager.clear();

        MessageJpaEntity updated = entityManager.find(MessageJpaEntity.class, 2L);

        // then
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
    }

    @Test
    @DisplayName("엔티티 수정 시 createdAt은 변경되지 않는다")
    void should_NotChangeCreatedAt_when_EntityUpdated() {
        // given
        MessageJpaEntity message = MessageJpaEntity.builder()
                .id(3L)
                .chatRoomId(1L)
                .senderId(1L)
                .content("test")
                .build();
        entityManager.persistAndFlush(message);
        entityManager.clear();

        MessageJpaEntity found = entityManager.find(MessageJpaEntity.class, 3L);
        var originalCreatedAt = found.getCreatedAt();

        // when
        found.setUpdatedAt(found.getUpdatedAt());
        entityManager.persistAndFlush(found);
        entityManager.clear();

        MessageJpaEntity updated = entityManager.find(MessageJpaEntity.class, 3L);

        // then
        assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
    }
}
