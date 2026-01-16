package com.cotalk.application.service;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.inbound.RegisterDeviceTokenUseCase;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RegisterDeviceTokenService implements RegisterDeviceTokenUseCase {

    private final DeviceTokenRepository deviceTokenRepository;
    private final SnowflakeIdGenerator idGenerator;

    @Override
    public DeviceToken register(Long userId, String token, DeviceToken.DeviceType deviceType) {
        Optional<DeviceToken> existingToken = deviceTokenRepository.findByToken(token);

        if (existingToken.isPresent()) {
            DeviceToken existing = existingToken.get();
            
            // 같은 사용자면 활성화만
            if (existing.getUserId().equals(userId)) {
                existing.updateToken(token);
                log.info("Device token reactivated for user: {}", userId);
                return deviceTokenRepository.save(existing);
            }
            
            // 다른 사용자면 기존 토큰 삭제 후 새로 생성
            deviceTokenRepository.deleteByToken(token);
            log.info("Device token transferred from user {} to user {}", existing.getUserId(), userId);
        }

        DeviceToken newToken = DeviceToken.builder()
                .id(idGenerator.nextId())
                .userId(userId)
                .token(token)
                .deviceType(deviceType)
                .build();

        log.info("New device token registered for user: {}, type: {}", userId, deviceType);
        return deviceTokenRepository.save(newToken);
    }

    @Override
    public void unregister(String token) {
        deviceTokenRepository.deleteByToken(token);
        log.info("Device token unregistered: {}", token);
    }
}
