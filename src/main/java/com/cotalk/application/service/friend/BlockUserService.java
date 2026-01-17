package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.Block;
import com.cotalk.domain.exception.InvalidBlockException;
import com.cotalk.domain.port.inbound.friend.BlockUserUseCase;
import com.cotalk.domain.port.outbound.BlockRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 차단 유스케이스 구현체.
 * 특정 사용자를 차단하여 상호작용을 제한한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BlockUserService implements BlockUserUseCase {

    private final BlockRepository blockRepository;
    private final UserValidator userValidator;
    private final IdGenerator idGenerator;

    /**
     * 사용자를 차단한다.
     * 자기 자신 차단 불가, 중복 차단 불가 조건을 검증하고 차단 관계를 생성한다.
     *
     * @param blockerId 차단하는 사용자 ID
     * @param blockedId 차단당하는 사용자 ID
     * @throws InvalidBlockException 자기 자신을 차단하거나 이미 차단한 사용자인 경우
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Override
    public void blockUser(Long blockerId, Long blockedId) {
        userValidator.validateNotSelfAction(blockerId, blockedId, "차단");
        userValidator.validateUserExists(blockerId);
        userValidator.validateUserExists(blockedId);

        if (blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId).isPresent()) {
            throw new InvalidBlockException("이미 차단한 사용자입니다");
        }

        Block block = Block.builder()
                .id(idGenerator.nextId())
                .blockerId(blockerId)
                .blockedId(blockedId)
                .build();

        blockRepository.save(block);
    }
}
