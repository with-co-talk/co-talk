package com.cotalk.adapter.outbound.persistence.friend;

import com.cotalk.domain.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 차단 JPA 리포지토리.
 * Spring Data JPA를 통해 사용자 차단 데이터에 접근한다.
 *
 * @author seunggu.lee
 */
public interface BlockJpaRepository extends JpaRepository<Block, Long> {

    /**
     * 차단자 ID와 피차단자 ID로 차단 정보를 조회한다.
     *
     * @param blockerId 차단자 ID
     * @param blockedId 피차단자 ID
     * @return 차단 정보 (Optional)
     */
    Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /**
     * 차단자 ID로 차단 목록을 조회한다.
     *
     * @param blockerId 차단자 ID
     * @return 차단 목록
     */
    List<Block> findByBlockerId(Long blockerId);

    /**
     * 차단자 ID와 피차단자 ID로 차단 관계가 존재하는지 확인한다.
     *
     * @param blockerId 차단자 ID
     * @param blockedId 피차단자 ID
     * @return 차단 관계 존재 여부
     */
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}
