package com.cotalk.adapter.outbound.persistence.friend;

import com.cotalk.domain.entity.Block;
import com.cotalk.domain.port.outbound.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 차단 영속성 어댑터.
 * JPA를 통해 사용자 차단 데이터를 저장하고 조회한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class BlockRepositoryAdapter implements BlockRepository {

    private final BlockJpaRepository blockJpaRepository;

    /**
     * 차단 정보를 저장한다.
     *
     * @param block 저장할 차단 엔티티
     * @return 저장된 차단 엔티티
     */
    @Override
    public Block save(Block block) {
        return blockJpaRepository.save(block);
    }

    /**
     * ID로 차단 정보를 조회한다.
     *
     * @param id 차단 ID
     * @return 차단 정보 (Optional)
     */
    @Override
    public Optional<Block> findById(Long id) {
        return blockJpaRepository.findById(id);
    }

    /**
     * 차단자 ID와 피차단자 ID로 차단 정보를 조회한다.
     *
     * @param blockerId 차단자 ID
     * @param blockedId 피차단자 ID
     * @return 차단 정보 (Optional)
     */
    @Override
    public Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId) {
        return blockJpaRepository.findByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    /**
     * 차단자 ID로 차단 목록을 조회한다.
     *
     * @param blockerId 차단자 ID
     * @return 차단 목록
     */
    @Override
    public List<Block> findByBlockerId(Long blockerId) {
        return blockJpaRepository.findByBlockerId(blockerId);
    }

    /**
     * 차단자 ID와 피차단자 ID로 차단 관계가 존재하는지 확인한다.
     *
     * @param blockerId 차단자 ID
     * @param blockedId 피차단자 ID
     * @return 차단 관계 존재 여부
     */
    @Override
    public boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId) {
        return blockJpaRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    /**
     * 차단 정보를 삭제한다.
     *
     * @param block 삭제할 차단 엔티티
     */
    @Override
    public void delete(Block block) {
        blockJpaRepository.delete(block);
    }

    /**
     * 차단자 ID로 모든 차단 정보를 삭제한다.
     *
     * @param userId 차단한 사용자 ID
     */
    @Override
    public void deleteByBlockerId(Long userId) {
        blockJpaRepository.deleteByBlockerId(userId);
    }

    /**
     * 피차단자 ID로 모든 차단 정보를 삭제한다.
     *
     * @param userId 차단당한 사용자 ID
     */
    @Override
    public void deleteByBlockedId(Long userId) {
        blockJpaRepository.deleteByBlockedId(userId);
    }
}
