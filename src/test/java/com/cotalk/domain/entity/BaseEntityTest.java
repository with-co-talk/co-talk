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
        // given
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hash")
                .nickname("test")
                .build();

        // when
        entityManager.persistAndFlush(user);
        entityManager.clear();

        User foundUser = entityManager.find(User.class, 1L);

        // then
        assertThat(foundUser.getCreatedAt()).isNotNull();
        assertThat(foundUser.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("엔티티 수정 시 updatedAt이 갱신된다")
    void should_UpdateUpdatedAt_when_EntityUpdated() throws InterruptedException {
        // given
        User user = User.builder()
                .id(2L)
                .email("test2@example.com")
                .passwordHash("hash")
                .nickname("test")
                .build();
        entityManager.persistAndFlush(user);
        entityManager.clear();

        User foundUser = entityManager.find(User.class, 2L);
        var originalUpdatedAt = foundUser.getUpdatedAt();

        // when
        Thread.sleep(10); // 시간 차이 보장
        foundUser.updateNickname("newNickname");
        entityManager.persistAndFlush(foundUser);
        entityManager.clear();

        User updatedUser = entityManager.find(User.class, 2L);

        // then
        assertThat(updatedUser.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
    }

    @Test
    @DisplayName("엔티티 수정 시 createdAt은 변경되지 않는다")
    void should_NotChangeCreatedAt_when_EntityUpdated() {
        // given
        User user = User.builder()
                .id(3L)
                .email("test3@example.com")
                .passwordHash("hash")
                .nickname("test")
                .build();
        entityManager.persistAndFlush(user);
        entityManager.clear();

        User foundUser = entityManager.find(User.class, 3L);
        var originalCreatedAt = foundUser.getCreatedAt();

        // when
        foundUser.updateNickname("newNickname");
        entityManager.persistAndFlush(foundUser);
        entityManager.clear();

        User updatedUser = entityManager.find(User.class, 3L);

        // then
        assertThat(updatedUser.getCreatedAt()).isEqualTo(originalCreatedAt);
    }
}
