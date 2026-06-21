package com.cotalk.adapter.outbound.persistence.friend;

import com.cotalk.adapter.outbound.persistence.entity.BlockJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 차단 JPA 리포지토리.
 * persistence 계층 전용이며, 도메인 반환은 Adapter에서 매핑한다.
 *
 * @author seunggu.lee
 */
public interface BlockJpaRepository extends JpaRepository<BlockJpaEntity, Long> {

    /**
     * 차단자 ID와 피차단자 ID로 차단 정보를 조회한다.
     *
     * @param blockerId 차단자 ID
     * @param blockedId 피차단자 ID
     * @return 차단 정보 (Optional)
     */
    Optional<BlockJpaEntity> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /**
     * 차단자 ID로 차단 목록을 조회한다.
     *
     * @param blockerId 차단자 ID
     * @return 차단 목록
     */
    List<BlockJpaEntity> findByBlockerId(Long blockerId);

    /**
     * 차단자 ID와 피차단자 ID로 차단 관계가 존재하는지 확인한다.
     *
     * @param blockerId 차단자 ID
     * @param blockedId 피차단자 ID
     * @return 차단 관계 존재 여부
     */
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /**
     * 차단자 ID로 모든 차단 정보를 삭제한다.
     *
     * @param blockerId 차단자 ID
     */
    void deleteByBlockerId(Long blockerId);

    /**
     * 피차단자 ID로 모든 차단 정보를 삭제한다.
     *
     * @param blockedId 피차단자 ID
     */
    void deleteByBlockedId(Long blockedId);
}
