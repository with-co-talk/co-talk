package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.Block;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 차단 레포지토리 포트.
 * 사용자 차단 데이터 저장 및 조회를 위한 인터페이스를 정의한다.
 *
 * @author seunggu.lee
 */
public interface BlockRepository {

    /**
     * 차단 정보를 저장한다.
     *
     * @param block 저장할 차단 정보
     * @return 저장된 차단 정보
     */
    Block save(Block block);

    /**
     * ID로 차단 정보를 조회한다.
     *
     * @param id 차단 ID
     * @return 조회된 차단 정보 (Optional)
     */
    Optional<Block> findById(Long id);

    /**
     * 차단한 사용자 ID와 차단당한 사용자 ID로 차단 정보를 조회한다.
     *
     * @param blockerId 차단한 사용자 ID
     * @param blockedId 차단당한 사용자 ID
     * @return 조회된 차단 정보 (Optional)
     */
    Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /**
     * 특정 사용자가 차단한 모든 차단 정보를 조회한다.
     *
     * @param blockerId 차단한 사용자 ID
     * @return 차단 정보 목록
     */
    List<Block> findByBlockerId(Long blockerId);

    /**
     * 차단 관계 존재 여부를 확인한다.
     *
     * @param blockerId 차단한 사용자 ID
     * @param blockedId 차단당한 사용자 ID
     * @return 존재 여부
     */
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /**
     * 차단 정보를 삭제한다.
     *
     * @param block 삭제할 차단 정보
     */
    void delete(Block block);

    /**
     * 특정 사용자가 차단한 모든 차단 정보를 삭제한다.
     *
     * @param userId 차단한 사용자 ID
     */
    void deleteByBlockerId(Long userId);

    /**
     * 특정 사용자가 차단당한 모든 차단 정보를 삭제한다.
     *
     * @param userId 차단당한 사용자 ID
     */
    void deleteByBlockedId(Long userId);
}
