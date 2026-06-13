package com.cotalk.domain.validator;

import com.cotalk.domain.exception.BlockedRelationshipException;
import com.cotalk.domain.port.outbound.BlockRepository;
import lombok.RequiredArgsConstructor;

/**
 * 차단 관계 검증기.
 * <p>
 * 두 사용자 사이에 차단 관계가 존재하는지 검증한다. 제품 정책상 차단은 <b>양방향</b>으로 적용되어,
 * 한쪽이라도 상대를 차단한 경우 메시지 전송 · 친구 요청 · 1:1 채팅방 생성/재초대 등의
 * 상호작용을 거부한다.
 * </p>
 *
 * @author seunggu.lee
 */
@RequiredArgsConstructor
public class BlockValidator {

    private final BlockRepository blockRepository;

    /**
     * 두 사용자 사이에 차단 관계가 없는지 검증한다.
     * <p>
     * 양방향으로 검사하며, 어느 한쪽이라도 상대를 차단한 경우 예외를 던진다.
     * </p>
     *
     * @param userId1 첫 번째 사용자 ID
     * @param userId2 두 번째 사용자 ID
     * @throws BlockedRelationshipException 두 사용자 사이에 차단 관계가 존재하는 경우
     */
    public void validateNotBlocked(Long userId1, Long userId2) {
        if (isBlockedBetween(userId1, userId2)) {
            throw new BlockedRelationshipException();
        }
    }

    /**
     * 두 사용자 사이에 (양방향) 차단 관계가 존재하는지 확인한다.
     *
     * @param userId1 첫 번째 사용자 ID
     * @param userId2 두 번째 사용자 ID
     * @return 어느 한쪽이라도 상대를 차단했으면 {@code true}
     */
    public boolean isBlockedBetween(Long userId1, Long userId2) {
        return blockRepository.existsByBlockerIdAndBlockedId(userId1, userId2)
                || blockRepository.existsByBlockerIdAndBlockedId(userId2, userId1);
    }
}
