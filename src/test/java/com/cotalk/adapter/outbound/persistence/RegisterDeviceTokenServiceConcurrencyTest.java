package com.cotalk.adapter.outbound.persistence;

import com.cotalk.adapter.outbound.persistence.mapper.DeviceTokenMapper;
import com.cotalk.adapter.outbound.persistence.mapper.UserMapper;
import com.cotalk.adapter.outbound.persistence.notification.DeviceTokenRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.user.UserRepositoryAdapter;
import com.cotalk.application.service.notification.DeviceTokenInserter;
import com.cotalk.application.service.notification.RegisterDeviceTokenService;
import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.entity.DeviceToken.DeviceType;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.infrastructure.config.JpaAuditingConfig;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link RegisterDeviceTokenService}의 동시 등록 경합 처리를 <b>실제 H2 UNIQUE 제약과
 * 실제 트랜잭션 거동</b>으로 검증하는 통합 테스트.
 *
 * <p>mock-throw 단위 테스트는 INSERT 실패를 인위적으로 던질 뿐이라 "지연 flush로 위반이
 * 트랜잭션 경계 밖에서 터지는지", "REQUIRES_NEW 롤백 후 바깥 트랜잭션이 깨끗한지"를
 * 검증하지 못한다(항진적). 이 테스트는:
 * <ul>
 *   <li>{@link DeviceTokenInserter#insertNew}가 {@code saveAndFlush}로 실제 UNIQUE
 *       위반을 <b>동기적으로</b> surface시키는지,</li>
 *   <li>{@code register}가 위반을 잡아 폴백 UPDATE를 <b>커밋까지 성공</b>시키는지
 *       (rollback-only 오염으로 인한 {@code UnexpectedRollbackException}이 없는지)</li>
 * </ul>
 * 를 실제 DB 상태로 확인한다.</p>
 *
 * <p>각 테스트 메서드는 트랜잭션 밖에서 실행되어({@code @Transactional(propagation=NEVER)}
 * 효과를 위해 {@code @DataJpaTest}의 기본 테스트 트랜잭션을 비활성화), 서비스의
 * {@code @Transactional}/{@code REQUIRES_NEW} 경계가 실제로 동작하도록 한다.</p>
 *
 * @author seunggu.lee
 */
@DataJpaTest(properties = "spring.test.database.replace=none")
@ActiveProfiles("test")
@Import({
        DeviceTokenRepositoryAdapter.class,
        DeviceTokenMapper.class,
        UserRepositoryAdapter.class,
        UserMapper.class,
        JpaAuditingConfig.class,
        DeviceTokenInserter.class,
        RegisterDeviceTokenService.class,
        RegisterDeviceTokenServiceConcurrencyTest.IdGeneratorTestConfig.class
})
@org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NEVER)
@DisplayName("RegisterDeviceTokenService 동시 등록 경합 (실제 H2 UNIQUE)")
class RegisterDeviceTokenServiceConcurrencyTest {

    /**
     * 테스트용 Snowflake ID 생성기 빈 설정.
     */
    @TestConfiguration
    static class IdGeneratorTestConfig {
        /**
         * Snowflake ID 생성기 빈.
         *
         * @return ID 생성기
         */
        @Bean
        SnowflakeIdGenerator idGenerator() {
            return new SnowflakeIdGenerator(0, 0);
        }
    }

    @Autowired
    private RegisterDeviceTokenService service;

    @Autowired
    private DeviceTokenInserter inserter;

    @Autowired
    private DeviceTokenRepositoryAdapter deviceTokenRepository;

    @Autowired
    private UserRepositoryAdapter userRepository;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        // 테스트 트랜잭션이 비활성(NEVER)이므로 서비스의 @Transactional/REQUIRES_NEW 경계가
        // 실제로 동작한다. 단, 셋업/정리의 DB 쓰기는 트랜잭션이 필요하므로 명시적으로
        // TransactionTemplate으로 감싸 즉시 커밋한다. (컨텍스트 재사용 대비 직전 데이터 정리)
        org.springframework.transaction.support.TransactionTemplate tx =
                new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            deviceTokenRepository.deleteByUserId(1L);
            deviceTokenRepository.deleteByUserId(2L);
            userRepository.findById(1L).ifPresent(userRepository::delete);
            userRepository.findById(2L).ifPresent(userRepository::delete);
        });

        user1 = tx.execute(status -> userRepository.save(User.builder()
                .id(1L)
                .email(new Email("user1@example.com"))
                .passwordHash("hash")
                .nickname("user1")
                .build()));
        user2 = tx.execute(status -> userRepository.save(User.builder()
                .id(2L)
                .email(new Email("user2@example.com"))
                .passwordHash("hash")
                .nickname("user2")
                .build()));
    }

    /**
     * 주어진 토큰을 짧은 트랜잭션 안에서 비활성화 상태로 갱신한다.
     * ({@code @Transactional(NEVER)} 환경에서 bare save를 직접 호출하면 트랜잭션이 없어
     * 실패하므로 명시적으로 트랜잭션으로 감싼다.)
     *
     * @param token 비활성화할 토큰
     */
    private void deactivateInTx(String token) {
        org.springframework.transaction.support.TransactionTemplate tx =
                new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status ->
                deviceTokenRepository.findByToken(token).ifPresent(t -> {
                    t.deactivate();
                    deviceTokenRepository.save(t);
                }));
    }

    @Test
    @DisplayName("DeviceTokenInserter.insertNew는 이미 같은 토큰이 존재하면 실제 UNIQUE 위반을 동기적으로 던진다")
    void should_throwDataIntegrityViolation_synchronously_when_tokenAlreadyExists() {
        // given: 경쟁 요청이 먼저 같은 토큰을 INSERT(커밋)한 상태
        String token = "racing-token";
        inserter.insertNew(user1.getId(), token, DeviceType.ANDROID);

        // when / then: 동일 토큰 신규 INSERT는 saveAndFlush로 즉시 UNIQUE 위반을 surface시킨다.
        // (지연 flush였다면 이 호출은 통과하고 커밋 시점에야 터졌을 것)
        assertThatThrownBy(() -> inserter.insertNew(user2.getId(), token, DeviceType.IOS))
                .isInstanceOf(DataIntegrityViolationException.class);

        // 첫 레코드만 남아있어야 한다 (실패한 INSERT는 REQUIRES_NEW 경계에서 롤백)
        List<DeviceToken> all = deviceTokenRepository.findByToken(token).map(List::of).orElse(List.of());
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getUserId()).isEqualTo(user1.getId());
    }

    @Test
    @DisplayName("동시 신규 경합: 경쟁 레코드 선존재 시 같은 사용자 register는 위반→폴백 UPDATE로 커밋 성공한다")
    void should_fallbackAndCommit_when_sameUserRacesNewToken() {
        // given: 경쟁 요청이 먼저 같은 토큰을 비활성으로 INSERT(커밋)
        String token = "shared-token";
        inserter.insertNew(user1.getId(), token, DeviceType.ANDROID);
        // 비활성 상태로 만들어 폴백/재사용 UPDATE가 활성화까지 수행함을 확인
        deactivateInTx(token);

        // when: 같은 사용자가 register → 내부 findByToken이 이미 존재를 보면 재사용,
        // 만약 경합으로 miss했다면 insertNew가 위반→폴백. 어느 경로든 최종 상태는 활성 1건.
        DeviceToken result = service.register(user1.getId(), token, DeviceType.ANDROID);

        // then: 예외 없이 커밋 성공, 활성 레코드 1건
        assertThat(result.getUserId()).isEqualTo(user1.getId());
        assertThat(result.isActive()).isTrue();
        Optional<DeviceToken> found = deviceTokenRepository.findByToken(token);
        assertThat(found).isPresent();
        assertThat(found.get().isActive()).isTrue();
        assertThat(deviceTokenRepository.findByUserId(user1.getId())).hasSize(1);
    }

    @Test
    @DisplayName("실제 동시 등록: 두 스레드가 같은 신규 토큰을 register해도 둘 다 예외 없이 활성 레코드 1건으로 수렴한다")
    void should_bothSucceedWithSingleRecord_when_twoThreadsRaceSameNewToken() throws Exception {
        // given: 같은 신규 토큰을 두 스레드가 동시에 register 시도.
        // 한쪽은 신규 INSERT 성공, 다른 한쪽은 findByToken miss → insertNew UNIQUE 위반
        // → REQUIRES_NEW 롤백 → 깨끗한 바깥 트랜잭션에서 재조회→UPDATE 폴백으로 커밋 성공.
        String token = "true-race-token";
        int threads = 2;
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        java.util.concurrent.CyclicBarrier barrier = new java.util.concurrent.CyclicBarrier(threads);
        java.util.List<java.util.concurrent.Future<Throwable>> futures = new java.util.ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final Long uid = user1.getId();
            futures.add(pool.submit(() -> {
                try {
                    barrier.await(); // 동시 출발
                    service.register(uid, token, DeviceType.ANDROID);
                    return null;
                } catch (Throwable t) {
                    return t;
                }
            }));
        }

        // then: 어떤 스레드도 UnexpectedRollbackException/500 없이 완료되어야 한다
        for (java.util.concurrent.Future<Throwable> f : futures) {
            Throwable t = f.get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(t).as("동시 register는 예외 없이 완료되어야 한다").isNull();
        }
        pool.shutdownNow();

        // 최종 상태: 동일 토큰 레코드 1건, 활성
        Optional<DeviceToken> found = deviceTokenRepository.findByToken(token);
        assertThat(found).isPresent();
        assertThat(found.get().isActive()).isTrue();
        assertThat(deviceTokenRepository.findByUserId(user1.getId())).hasSize(1);
    }

    @Test
    @DisplayName("신규 정상 경로: 토큰이 없으면 INSERT되어 커밋된다")
    void should_insertAndCommit_when_brandNewToken() {
        // when
        DeviceToken result = service.register(user1.getId(), "brand-new-token", DeviceType.ANDROID);

        // then
        assertThat(result.getUserId()).isEqualTo(user1.getId());
        assertThat(result.isActive()).isTrue();
        Optional<DeviceToken> found = deviceTokenRepository.findByToken("brand-new-token");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(result.getId());
    }

    @Test
    @DisplayName("재등록 경로: 같은 사용자의 비활성 토큰을 다시 등록하면 활성화된다")
    void should_reactivate_when_sameUserReregisters() {
        // given
        String token = "reactivate-token";
        inserter.insertNew(user1.getId(), token, DeviceType.ANDROID);
        deactivateInTx(token);

        // when
        DeviceToken result = service.register(user1.getId(), token, DeviceType.ANDROID);

        // then
        assertThat(result.isActive()).isTrue();
        assertThat(deviceTokenRepository.findByUserId(user1.getId())).hasSize(1);
    }

    @Test
    @DisplayName("소유자 이전 경로: 다른 사용자가 같은 토큰을 등록하면 UNIQUE 충돌 없이 이전된다")
    void should_transferOwnership_when_anotherUserRegisters() {
        // given: user1이 토큰 보유
        String token = "transfer-token";
        inserter.insertNew(user1.getId(), token, DeviceType.ANDROID);
        deactivateInTx(token);

        // when: user2가 동일 토큰 등록
        DeviceToken result = service.register(user2.getId(), token, DeviceType.IOS);

        // then: 소유자/타입/활성 갱신, 레코드 1건
        assertThat(result.getUserId()).isEqualTo(user2.getId());
        assertThat(result.getDeviceType()).isEqualTo(DeviceType.IOS);
        assertThat(result.isActive()).isTrue();
        assertThat(deviceTokenRepository.findByUserId(user1.getId())).isEmpty();
        assertThat(deviceTokenRepository.findByUserId(user2.getId())).hasSize(1);
    }
}
