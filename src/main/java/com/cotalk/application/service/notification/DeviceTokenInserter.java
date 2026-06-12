package com.cotalk.application.service.notification;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신규 디바이스 토큰을 독립 트랜잭션(REQUIRES_NEW) 안에서 INSERT하는 컴포넌트.
 *
 * <p><b>왜 별도 빈 + REQUIRES_NEW인가:</b> {@link DeviceToken}은 {@code @Id}를 수동
 * 할당(Snowflake)하며 {@code @GeneratedValue}/{@code Persistable}/{@code @Version}가
 * 없다. 따라서 Spring Data {@code save()}는 {@code isNew()==false}로 판단해
 * {@code persist()}가 아닌 {@code merge()}를 타고, 기본 AUTO flush에서 INSERT(및
 * {@code token} UNIQUE 위반)가 커밋 시점까지 지연된다. 만약 이 INSERT를
 * {@link RegisterDeviceTokenService#register}의 클래스 레벨 {@code @Transactional}이
 * 만든 동일 트랜잭션 안에서 수행하면, 동시 등록 경합 시:
 * <ul>
 *   <li>flush가 커밋까지 지연되어 UNIQUE 위반이 {@code register}의 {@code try/catch}
 *       <b>바깥</b>(트랜잭션 프록시 경계)에서 터져 폴백이 동작하지 않을 수 있고,</li>
 *   <li>flush가 동기로 일어나 catch가 잡히더라도 그 트랜잭션은 이미 rollback-only로
 *       마킹되어, 그 위에서 폴백 UPDATE를 쓰면 커밋 시 {@code UnexpectedRollbackException}이
 *       발생한다.</li>
 * </ul>
 *
 * <p>이를 해결하기 위해 신규 INSERT를 이 빈의 {@code REQUIRES_NEW} 메서드로 분리한다.
 * UNIQUE 위반은 이 <b>독립된 내부 트랜잭션</b> 경계에서 surface·롤백되며, 호출자의
 * 바깥 트랜잭션은 오염되지 않은 깨끗한 상태로 유지된다. 호출자는 위반을 감지하면
 * 깨끗한 상태에서 재조회→UPDATE(소유자 이전/활성화)를 수행한다.</p>
 *
 * <p>self-invocation 프록시 함정(같은 클래스 내부에서 {@code @Transactional} 메서드를
 * 호출하면 AOP 프록시를 거치지 않아 전파 속성이 무시됨)을 피하기 위해 반드시 외부
 * 빈으로 분리한다.</p>
 *
 * @author seunggu.lee
 * @see RegisterDeviceTokenService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceTokenInserter {

    private final DeviceTokenRepository deviceTokenRepository;
    private final IdGenerator idGenerator;

    /**
     * 신규 디바이스 토큰을 독립 트랜잭션 안에서 INSERT한다.
     *
     * <p>{@code saveAndFlush}로 즉시 flush하여, 동시 등록 경합 시 {@code token} UNIQUE
     * 위반이 이 메서드 안에서 동기적으로 발생하도록 한다. 위반이 발생하면 이 독립
     * 트랜잭션만 롤백되고 예외가 호출자에게 전파된다. (호출자의 바깥 트랜잭션은
     * 오염되지 않는다.)</p>
     *
     * @param userId     사용자 ID
     * @param token      디바이스 토큰
     * @param deviceType 디바이스 타입
     * @return 저장된 디바이스 토큰
     * @throws DataIntegrityViolationException 동시 등록 경합으로 {@code token} UNIQUE
     *                                         제약을 위반한 경우
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DeviceToken insertNew(Long userId, String token, DeviceToken.DeviceType deviceType) {
        DeviceToken newToken = DeviceToken.builder()
                .id(idGenerator.nextId())
                .userId(userId)
                .token(token)
                .deviceType(deviceType)
                .build();

        DeviceToken saved = deviceTokenRepository.saveAndFlush(newToken);
        log.info("New device token registered for user: {}, type: {}", userId, deviceType);
        return saved;
    }
}
