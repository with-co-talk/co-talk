package com.cotalk.application.service.notification;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.inbound.notification.RegisterDeviceTokenUseCase;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.util.TokenMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
     * <p>동시성 처리: 두 요청이 거의 동시에 동일한 신규 토큰을 등록하면 양쪽 모두
     * {@code findByToken}에서 empty를 읽고 각자 새 레코드를 INSERT하여 한쪽이
     * {@code token} UNIQUE 제약을 위반한다. 이때 발생하는
     * {@link DataIntegrityViolationException}을 잡아 동일 트랜잭션을 롤백시키지 않고
     * 재조회 후 UPDATE 경로로 폴백하여, 경쟁에서 진 요청도 정상적으로 등록 결과를
     * 반환한다. ({@code AddMessageReactionService}의 UNIQUE + 예외 폴백 패턴과 동일)</p>
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
            return reuseExistingToken(existingToken.get(), userId, token, deviceType);
        }

        return registerNewTokenSafely(userId, token, deviceType);
    }

    /**
     * 기존 레코드를 재사용하여 토큰을 재활성화하거나 소유자를 이전한다.
     *
     * @param existing   기존 디바이스 토큰 레코드
     * @param userId     요청 사용자 ID
     * @param token      디바이스 토큰
     * @param deviceType 디바이스 타입
     * @return 갱신된 디바이스 토큰 정보
     */
    private DeviceToken reuseExistingToken(DeviceToken existing, Long userId, String token, DeviceToken.DeviceType deviceType) {
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

    /**
     * 신규 토큰을 등록하되, 동시 INSERT로 인한 UNIQUE 위반 시 재조회 후 UPDATE로 폴백한다.
     *
     * @param userId     사용자 ID
     * @param token      디바이스 토큰
     * @param deviceType 디바이스 타입
     * @return 등록되거나 폴백으로 갱신된 디바이스 토큰 정보
     */
    private DeviceToken registerNewTokenSafely(Long userId, String token, DeviceToken.DeviceType deviceType) {
        try {
            DeviceToken newToken = DeviceToken.builder()
                    .id(idGenerator.nextId())
                    .userId(userId)
                    .token(token)
                    .deviceType(deviceType)
                    .build();

            log.info("New device token registered for user: {}, type: {}", userId, deviceType);
            return deviceTokenRepository.save(newToken);
        } catch (DataIntegrityViolationException e) {
            // 동시 등록 경합: 다른 요청이 같은 토큰을 먼저 INSERT함 → 재조회 후 UPDATE로 폴백
            log.debug("Concurrent device token registration detected, falling back to update for user: {}", userId);
            DeviceToken existing = deviceTokenRepository.findByToken(token)
                    .orElseThrow(() -> new IllegalStateException(
                            "Device token should exist after DataIntegrityViolationException"));
            return reuseExistingToken(existing, userId, token, deviceType);
        }
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
