package com.cotalk.adapter.outbound.persistence.profile;

import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;
import com.cotalk.domain.port.outbound.ProfileHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 프로필 이력 영속성 어댑터.
 * JPA를 통해 프로필 이력 데이터를 저장하고 조회한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class ProfileHistoryRepositoryAdapter implements ProfileHistoryRepository {

    private final ProfileHistoryJpaRepository jpaRepository;

    @Override
    public ProfileHistory save(ProfileHistory profileHistory) {
        return jpaRepository.save(profileHistory);
    }

    @Override
    public Optional<ProfileHistory> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<ProfileHistory> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, ProfileHistoryType type) {
        return jpaRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type);
    }

    @Override
    public List<ProfileHistory> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Optional<ProfileHistory> findByUserIdAndTypeAndIsCurrentTrue(Long userId, ProfileHistoryType type) {
        return jpaRepository.findByUserIdAndTypeAndIsCurrentTrue(userId, type);
    }

    @Override
    public void delete(ProfileHistory profileHistory) {
        jpaRepository.delete(profileHistory);
    }

    @Override
    public long countByUserIdAndType(Long userId, ProfileHistoryType type) {
        return jpaRepository.countByUserIdAndType(userId, type);
    }
}
