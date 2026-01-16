package com.cotalk.application.service;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.entity.User.OnlineStatus;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.UpdateUserOnlineStatusUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateUserOnlineStatusService implements UpdateUserOnlineStatusUseCase {

    private final UserRepository userRepository;

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

    @Override
    public void setOnline(Long userId) {
        updateOnlineStatus(userId, OnlineStatus.ONLINE);
        log.info("User set online: userId={}", userId);
    }

    @Override
    public void setOffline(Long userId) {
        updateOnlineStatus(userId, OnlineStatus.OFFLINE);
        log.info("User set offline: userId={}", userId);
    }

    @Override
    public void updateLastActiveAt(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.updateLastActiveAt();
        userRepository.save(user);
        log.debug("User last active time updated: userId={}", userId);
    }
}
