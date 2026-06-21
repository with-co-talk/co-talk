package com.cotalk.adapter.outbound.persistence.profile;

import com.cotalk.adapter.outbound.persistence.entity.ProfileHistoryJpaEntity;
import com.cotalk.domain.entity.ProfileHistoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 프로필 이력 JPA 리포지토리.
 * persistence 계층 전용이며, 도메인 반환은 Adapter에서 매핑한다.
 *
 * @author seunggu.lee
 */
public interface ProfileHistoryJpaRepository extends JpaRepository<ProfileHistoryJpaEntity, Long> {

    List<ProfileHistoryJpaEntity> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, ProfileHistoryType type);

    List<ProfileHistoryJpaEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ProfileHistoryJpaEntity> findByUserIdAndTypeAndIsCurrentTrue(Long userId, ProfileHistoryType type);

    long countByUserIdAndType(Long userId, ProfileHistoryType type);

    /**
     * 사용자 ID로 모든 프로필 이력을 삭제한다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);
}
