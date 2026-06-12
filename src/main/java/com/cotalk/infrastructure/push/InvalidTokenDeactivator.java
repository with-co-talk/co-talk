package com.cotalk.infrastructure.push;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
import com.cotalk.domain.util.TokenMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * FCM 전송 실패로 무효화된 디바이스 토큰을 비활성화하는 컴포넌트.
 *
 * <p>FCM 네트워크 호출(수백ms~수초)과 토큰 비활성화 DB 쓰기를 분리하기 위해
 * 별도 빈으로 추출되었다. {@link FcmPushNotificationSender}는 트랜잭션 밖에서
 * FCM 원격 호출을 수행한 뒤, 비활성화가 필요한 토큰만 이 컴포넌트의
 * {@code @Transactional} 메서드로 위임한다. 이렇게 하면 DB 커넥션 점유 시간이
 * FCM 왕복 시간이 아닌 짧은 UPDATE 시간으로 제한된다.
 *
 * <p>별도 빈으로 분리한 이유: 동일 클래스 내부에서 {@code @Transactional} 메서드를
 * 호출하면 Spring AOP 프록시를 거치지 않아 트랜잭션이 적용되지 않는
 * self-invocation 함정에 빠진다. 외부 빈으로 호출하면 프록시를 거쳐 트랜잭션이
 * 정상 적용된다.
 *
 * @author seunggu.lee
 * @see FcmPushNotificationSender
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvalidTokenDeactivator {

    private final DeviceTokenRepository deviceTokenRepository;

    /**
     * 단일 토큰을 짧은 독립 트랜잭션 안에서 비활성화한다.
     *
     * @param token 비활성화할 토큰
     */
    @Transactional
    public void deactivateToken(String token) {
        Optional<DeviceToken> found = deviceTokenRepository.findByToken(token);
        found.ifPresent(deviceToken -> {
            deviceToken.deactivate();
            deviceTokenRepository.save(deviceToken);
            log.info("Deactivated invalid FCM token: {}", TokenMasker.mask(token));
        });
    }

    /**
     * 여러 토큰을 짧은 독립 트랜잭션 안에서 비활성화한다.
     *
     * @param tokens 비활성화할 토큰 목록
     */
    @Transactional
    public void deactivateTokens(List<String> tokens) {
        for (String token : tokens) {
            deviceTokenRepository.findByToken(token).ifPresent(deviceToken -> {
                deviceToken.deactivate();
                deviceTokenRepository.save(deviceToken);
                log.info("Deactivated invalid FCM token: {}", TokenMasker.mask(token));
            });
        }
        log.info("Deactivated {} invalid FCM tokens", tokens.size());
    }
}
