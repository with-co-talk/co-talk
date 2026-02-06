package com.cotalk.adapter.outbound.persistence.profile;

import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 프로필 이력 JPA 리포지토리.
 *
 * @author seunggu.lee
 */
public interface ProfileHistoryJpaRepository extends JpaRepository<ProfileHistory, Long> {

    List<ProfileHistory> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, ProfileHistoryType type);

    List<ProfileHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ProfileHistory> findByUserIdAndTypeAndIsCurrentTrue(Long userId, ProfileHistoryType type);

    long countByUserIdAndType(Long userId, ProfileHistoryType type);

    /**
     * 사용자 ID로 모든 프로필 이력을 삭제한다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);
}
