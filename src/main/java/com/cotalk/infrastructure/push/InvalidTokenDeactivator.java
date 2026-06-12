package com.cotalk.infrastructure.push;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
import com.cotalk.domain.util.TokenMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
 * <p><b>트랜잭션 전제</b>: 이 컴포넌트의 {@code @Transactional} 메서드는 기본 전파
 * 속성({@code REQUIRED})을 사용한다. 즉, 호출 시점에 활성 트랜잭션이 없어야 새 트랜잭션을
 * 시작하여 "짧은 독립 트랜잭션"이라는 의도가 성립한다. 현재 호출 체인은 이 전제를 만족한다:
 * {@link com.cotalk.application.service.notification.SendPushNotificationService}는
 * {@code @Async}이며 {@code @Transactional}이 없고(별도 스레드 + 트랜잭션 없음),
 * {@link FcmPushNotificationSender} 또한 트랜잭션이 없어 FCM 원격 호출은 트랜잭션 밖에서
 * 수행된다. 따라서 이 메서드 진입 시점에 활성 트랜잭션이 없다.
 *
 * <p>만약 향후 이 컴포넌트를 이미 트랜잭션이 열린 컨텍스트(예: {@code @Transactional}
 * 서비스 내부)에서 호출하도록 호출 체인이 바뀌면, 기본 {@code REQUIRED} 전파로는 기존
 * 트랜잭션에 합류하여 "짧은 독립 트랜잭션"이라는 성능 의도가 깨진다. 그 경우
 * {@code @Transactional(propagation = REQUIRES_NEW)}로 변경하여 항상 독립 트랜잭션을
 * 보장해야 한다.
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
        deactivateSingle(token);
    }

    /**
     * 여러 토큰을 짧은 독립 트랜잭션 안에서 비활성화한다.
     *
     * @param tokens 비활성화할 토큰 목록
     */
    @Transactional
    public void deactivateTokens(List<String> tokens) {
        for (String token : tokens) {
            deactivateSingle(token);
        }
        log.info("Deactivated {} invalid FCM tokens", tokens.size());
    }

    /**
     * 단일 토큰의 비활성화 로직(조회→비활성화→저장→로그)을 수행한다.
     *
     * <p>{@code private} 메서드이므로 호출자({@code deactivateToken} /
     * {@code deactivateTokens})가 이미 진입한 트랜잭션 컨텍스트 안에서 실행된다.
     * 공개 {@code @Transactional} 메서드를 self-invocation으로 호출하면 프록시를
     * 거치지 않아 트랜잭션 경계가 무시되므로, 트랜잭션 경계는 공개 메서드에 두고
     * 공통 로직만 이 헬퍼로 추출하여 중복을 제거한다.
     *
     * @param token 비활성화할 토큰
     */
    private void deactivateSingle(String token) {
        deviceTokenRepository.findByToken(token).ifPresent(deviceToken -> {
            deviceToken.deactivate();
            deviceTokenRepository.save(deviceToken);
            log.info("Deactivated invalid FCM token: {}", TokenMasker.mask(token));
        });
    }
}
