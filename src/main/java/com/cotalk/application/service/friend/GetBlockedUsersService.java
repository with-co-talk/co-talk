package com.cotalk.application.service.friend;

import com.cotalk.domain.entity.Block;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.friend.GetBlockedUsersUseCase;
import com.cotalk.domain.port.outbound.BlockRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 차단 사용자 목록 조회 유스케이스 구현체.
 * 사용자가 차단한 사용자 목록을 조회한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetBlockedUsersService implements GetBlockedUsersUseCase {

    private final BlockRepository blockRepository;
    private final UserRepository userRepository;

    /**
     * 차단한 사용자 목록을 조회한다.
     * 차단 관계를 조회한 뒤 사용자 정보를 배치로 한 번에 조회하며(N+1 방지),
     * 차단 관계 순서를 보존하고 삭제된 사용자는 결과에서 제외한다.
     *
     * @param blockerId 차단 목록을 조회할 사용자 ID
     * @return 차단한 사용자 목록
     */
    @Override
    public List<User> getBlockedUsers(Long blockerId) {
        List<Block> blocks = blockRepository.findByBlockerId(blockerId);
        if (blocks.isEmpty()) {
            return List.of();
        }

        List<Long> blockedIds = blocks.stream()
                .map(Block::getBlockedId)
                .toList();
        Map<Long, User> usersById = userRepository.findAllById(blockedIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return blocks.stream()
                .map(block -> usersById.get(block.getBlockedId()))
                .filter(Objects::nonNull)
                .toList();
    }
}
