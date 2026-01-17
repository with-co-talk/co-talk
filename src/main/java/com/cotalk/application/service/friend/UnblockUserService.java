package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.Block;
import com.cotalk.domain.exception.BlockNotFoundException;
import com.cotalk.domain.port.inbound.friend.UnblockUserUseCase;
import com.cotalk.domain.port.outbound.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 차단 해제 유스케이스 구현체.
 * 기존에 차단한 사용자의 차단을 해제한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UnblockUserService implements UnblockUserUseCase {

    private final BlockRepository blockRepository;

    /**
     * 사용자 차단을 해제한다.
     * 기존 차단 관계를 삭제한다.
     *
     * @param blockerId 차단을 해제하는 사용자 ID
     * @param blockedId 차단 해제되는 사용자 ID
     * @throws BlockNotFoundException 차단 관계를 찾을 수 없는 경우
     */
    @Override
    public void unblockUser(Long blockerId, Long blockedId) {
        Block block = blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .orElseThrow(() -> new BlockNotFoundException("차단 관계를 찾을 수 없습니다"));

        blockRepository.delete(block);
    }
}
