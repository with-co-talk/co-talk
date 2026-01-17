package com.cotalk.application.service.user;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.entity.User.OnlineStatus;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.user.UpdateUserOnlineStatusUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 온라인 상태 업데이트 유스케이스 구현체.
 * 사용자의 온라인/오프라인/자리비움 상태와 마지막 활동 시간을 관리한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateUserOnlineStatusService implements UpdateUserOnlineStatusUseCase {

    private final UserRepository userRepository;

    /**
     * 사용자의 온라인 상태를 업데이트한다.
     *
     * @param userId 사용자 ID
     * @param status 변경할 온라인 상태 (ONLINE, OFFLINE, AWAY)
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Override
    public void updateOnlineStatus(Long userId, OnlineStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        switch (status) {
            case ONLINE -> user.setOnline();
            case OFFLINE -> user.setOffline();
            case AWAY -> user.setAway();
        }

        userRepository.save(user);
        log.debug("User online status updated: userId={}, status={}", userId, status);
    }

    /**
     * 사용자를 온라인 상태로 설정한다.
     *
     * @param userId 사용자 ID
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Override
    public void setOnline(Long userId) {
        updateOnlineStatus(userId, OnlineStatus.ONLINE);
        log.info("User set online: userId={}", userId);
    }

    /**
     * 사용자를 오프라인 상태로 설정한다.
     *
     * @param userId 사용자 ID
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Override
    public void setOffline(Long userId) {
        updateOnlineStatus(userId, OnlineStatus.OFFLINE);
        log.info("User set offline: userId={}", userId);
    }

    /**
     * 사용자의 마지막 활동 시간을 현재 시간으로 업데이트한다.
     *
     * @param userId 사용자 ID
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Override
    public void updateLastActiveAt(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.updateLastActiveAt();
        userRepository.save(user);
        log.debug("User last active time updated: userId={}", userId);
    }
}
