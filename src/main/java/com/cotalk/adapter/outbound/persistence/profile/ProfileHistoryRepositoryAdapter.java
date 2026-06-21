package com.cotalk.adapter.outbound.persistence.profile;

import com.cotalk.adapter.outbound.persistence.entity.ProfileHistoryJpaEntity;
import com.cotalk.adapter.outbound.persistence.mapper.ProfileHistoryMapper;
import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;
import com.cotalk.domain.port.outbound.ProfileHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 프로필 이력 영속성 어댑터.
 * JPA 엔티티와 도메인 간 매핑을 수행하며, 도메인 포트를 구현한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class ProfileHistoryRepositoryAdapter implements ProfileHistoryRepository {

    private final ProfileHistoryJpaRepository jpaRepository;
    private final ProfileHistoryMapper mapper;

    @Override
    public ProfileHistory save(ProfileHistory profileHistory) {
        ProfileHistoryJpaEntity saved = jpaRepository.save(mapper.toJpa(profileHistory));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ProfileHistory> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ProfileHistory> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, ProfileHistoryType type) {
        return jpaRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ProfileHistory> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ProfileHistory> findByUserIdAndTypeAndIsCurrentTrue(Long userId, ProfileHistoryType type) {
        return jpaRepository.findByUserIdAndTypeAndIsCurrentTrue(userId, type).map(mapper::toDomain);
    }

    @Override
    public void delete(ProfileHistory profileHistory) {
        jpaRepository.delete(mapper.toJpa(profileHistory));
    }

    @Override
    public long countByUserIdAndType(Long userId, ProfileHistoryType type) {
        return jpaRepository.countByUserIdAndType(userId, type);
    }

    /**
     * 사용자 ID로 모든 프로필 이력을 삭제한다.
     *
     * @param userId 사용자 ID
     */
    @Override
    public void deleteByUserId(Long userId) {
        jpaRepository.deleteByUserId(userId);
    }
}
