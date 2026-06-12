package com.cotalk.application.service.notification;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.inbound.notification.RegisterDeviceTokenUseCase;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
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
    private final DeviceTokenInserter deviceTokenInserter;

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
     * {@code token} UNIQUE 제약을 위반한다. 신규 INSERT는
     * {@link DeviceTokenInserter#insertNew}({@code REQUIRES_NEW})로 분리되어 있어,
     * 위반 시 그 <b>독립 트랜잭션만 롤백</b>되고 이 {@code register}의 바깥 트랜잭션은
     * 오염되지 않는다. 발생한 {@link DataIntegrityViolationException}을 잡아 깨끗한
     * 상태에서 재조회 후 UPDATE 경로(소유자 이전/활성화)로 폴백하여, 경쟁에서 진
     * 요청도 정상적으로 등록 결과를 반환한다.</p>
     *
     * <p>{@code DeviceToken}은 {@code @Id} 수동 할당(Snowflake)이라 {@code save()}가
     * {@code merge()}를 타고 INSERT/UNIQUE 위반이 커밋까지 지연되는 특성이 있어,
     * {@code @GeneratedValue(IDENTITY)} + read-only 폴백인
     * {@code AddMessageReactionService} 패턴을 그대로 쓸 수 없다. 이 차이 때문에
     * 신규 INSERT를 별도 트랜잭션 경계로 분리하고 즉시 flush한다.
     * 자세한 근거는 {@link DeviceTokenInserter} JavaDoc 참고.</p>
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
     * <p>실제 INSERT는 {@link DeviceTokenInserter#insertNew}의 {@code REQUIRES_NEW}
     * 트랜잭션에서 수행되고 즉시 flush된다. 동시 경합으로 {@code token} UNIQUE 위반이
     * 발생하면 그 독립 트랜잭션만 롤백되고 {@link DataIntegrityViolationException}이
     * 전파되며, 이 메서드(및 바깥 {@code register} 트랜잭션)는 rollback-only로
     * 오염되지 않으므로 안전하게 재조회→UPDATE 폴백을 수행할 수 있다.</p>
     *
     * @param userId     사용자 ID
     * @param token      디바이스 토큰
     * @param deviceType 디바이스 타입
     * @return 등록되거나 폴백으로 갱신된 디바이스 토큰 정보
     */
    private DeviceToken registerNewTokenSafely(Long userId, String token, DeviceToken.DeviceType deviceType) {
        try {
            return deviceTokenInserter.insertNew(userId, token, deviceType);
        } catch (DataIntegrityViolationException e) {
            // 동시 등록 경합: 다른 요청이 같은 토큰을 먼저 INSERT함.
            // 신규 INSERT는 REQUIRES_NEW 경계에서 롤백되었으므로 바깥 트랜잭션은 깨끗하다.
            // 깨끗한 상태에서 재조회 후 UPDATE(소유자 이전/활성화)로 폴백한다.
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
