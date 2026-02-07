package com.cotalk.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import com.cotalk.infrastructure.config.JpaAuditingConfig;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
@DisplayName("BaseEntity JPA Auditing")
class BaseEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("엔티티 저장 시 createdAt과 updatedAt이 자동 설정된다")
    void should_SetCreatedAtAndUpdatedAt_when_EntityPersisted() {
        // given - Message는 JPA 엔티티이며 BaseEntity(감사)를 상속한다
        Message message = Message.builder()
                .id(1L)
                .chatRoomId(1L)
                .senderId(1L)
                .content("test")
                .build();

        // when
        entityManager.persistAndFlush(message);
        entityManager.clear();

        Message found = entityManager.find(Message.class, 1L);

        // then
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("엔티티 수정 시 updatedAt이 갱신된다")
    void should_UpdateUpdatedAt_when_EntityUpdated() throws InterruptedException {
        // given
        Message message = Message.builder()
                .id(2L)
                .chatRoomId(1L)
                .senderId(1L)
                .content("test")
                .build();
        entityManager.persistAndFlush(message);
        entityManager.clear();

        Message found = entityManager.find(Message.class, 2L);
        var originalUpdatedAt = found.getUpdatedAt();

        // when
        Thread.sleep(10);
        found.updateContent("updated");
        entityManager.persistAndFlush(found);
        entityManager.clear();

        Message updated = entityManager.find(Message.class, 2L);

        // then
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
    }

    @Test
    @DisplayName("엔티티 수정 시 createdAt은 변경되지 않는다")
    void should_NotChangeCreatedAt_when_EntityUpdated() {
        // given
        Message message = Message.builder()
                .id(3L)
                .chatRoomId(1L)
                .senderId(1L)
                .content("test")
                .build();
        entityManager.persistAndFlush(message);
        entityManager.clear();

        Message found = entityManager.find(Message.class, 3L);
        var originalCreatedAt = found.getCreatedAt();

        // when
        found.updateContent("updated");
        entityManager.persistAndFlush(found);
        entityManager.clear();

        Message updated = entityManager.find(Message.class, 3L);

        // then
        assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
    }
}
