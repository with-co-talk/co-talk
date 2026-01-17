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
     * 차단 관계를 조회하고 해당 사용자 정보를 반환한다.
     *
     * @param blockerId 차단 목록을 조회할 사용자 ID
     * @return 차단한 사용자 목록
     */
    @Override
    public List<User> getBlockedUsers(Long blockerId) {
        List<Block> blocks = blockRepository.findByBlockerId(blockerId);

        return blocks.stream()
                .map(block -> userRepository.findById(block.getBlockedId()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();
    }
}
