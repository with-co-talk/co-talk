package com.cotalk.application.service.notification;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.inbound.notification.RegisterDeviceTokenUseCase;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.util.TokenMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 디바이스 토큰 등록 유스케이스 구현체.
 * 푸시 알림을 위한 디바이스 토큰을 등록 및 해제한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RegisterDeviceTokenService implements RegisterDeviceTokenUseCase {

    private final DeviceTokenRepository deviceTokenRepository;
    private final IdGenerator idGenerator;

    /**
     * 디바이스 토큰을 등록한다.
     * 동일한 토큰이 이미 존재하는 경우:
     * - 같은 사용자: 토큰을 재활성화
     * - 다른 사용자: 기존 레코드의 소유자를 새 사용자로 이전(UPDATE)하여 재활성화
     *
     * <p>토큰 컬럼은 UNIQUE 제약을 가지므로, 소유자 이전 시 기존 레코드를 삭제 후
     * 동일 토큰으로 새 레코드를 INSERT하면 Hibernate flush 순서에 따라 UNIQUE 위반이
     * 발생할 수 있다. 이를 회피하기 위해 기존 레코드를 그대로 UPDATE한다.</p>
     *
     * @param userId     사용자 ID
     * @param token      디바이스 토큰
     * @param deviceType 디바이스 타입 (iOS, Android 등)
     * @return 등록된 디바이스 토큰 정보
     */
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

            // 다른 사용자면 기존 레코드의 소유자를 이전(UPDATE) — delete+insert UNIQUE 충돌 회피
            Long previousUserId = existing.getUserId();
            existing.transferTo(userId, deviceType);
            log.info("Device token transferred from user {} to user {}", previousUserId, userId);
            return deviceTokenRepository.save(existing);
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

    /**
     * 디바이스 토큰 등록을 해제한다.
     *
     * @param token 해제할 디바이스 토큰
     */
    @Override
    public void unregister(Long userId, String token) {
        Optional<DeviceToken> existingToken = deviceTokenRepository.findByToken(token);
        if (existingToken.isPresent() && !existingToken.get().getUserId().equals(userId)) {
            log.warn("User {} attempted to unregister token owned by user {}", userId, existingToken.get().getUserId());
            return;
        }
        deviceTokenRepository.deleteByToken(token);
        log.info("Device token unregistered by user {}: {}", userId, TokenMasker.mask(token));
    }
}
