package com.cotalk.application.service.notification;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.inbound.notification.RegisterDeviceTokenUseCase;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
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
     * - 다른 사용자: 기존 토큰 삭제 후 새로 등록
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
        log.info("Device token unregistered by user {}: {}", userId, maskToken(token));
    }

    /**
     * 토큰을 마스킹하여 로그에 안전하게 출력한다.
     * 앞 6자 + ... + 뒤 4자 형식으로 변환한다.
     *
     * @param token 마스킹할 토큰
     * @return 마스킹된 토큰 문자열
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 10) {
            return "***";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }
}
