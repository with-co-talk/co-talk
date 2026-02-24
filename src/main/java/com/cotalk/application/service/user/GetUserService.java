package com.cotalk.application.service.user;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.user.GetUserUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자 조회 유스케이스 구현체.
 * ID로 단일 또는 다수의 사용자를 조회한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetUserService implements GetUserUseCase {

    private final UserRepository userRepository;

    /**
     * ID로 사용자를 조회한다.
     *
     * @param userId 조회할 사용자 ID
     * @return 조회된 사용자
     * @throws UserNotFoundException 사용자가 존재하지 않는 경우
     */
    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    /**
     * 여러 ID로 사용자 목록을 조회한다.
     *
     * @param userIds 조회할 사용자 ID 목록
     * @return 조회된 사용자 목록
     */
    @Override
    public List<User> getUsersByIds(List<Long> userIds) {
        return userRepository.findAllById(userIds);
    }
}
