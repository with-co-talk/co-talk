package com.cotalk.adapter.outbound.persistence;

import com.cotalk.adapter.outbound.persistence.notification.DeviceTokenRepositoryAdapter;
import com.cotalk.adapter.outbound.persistence.mapper.UserMapper;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DeviceTokenRepositoryAdapter 테스트.
 *
 * @author seunggu.lee
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({DeviceTokenRepositoryAdapter.class, UserRepositoryAdapter.class, UserMapper.class, JpaAuditingConfig.class})
@DisplayName("DeviceTokenRepositoryAdapter")
class DeviceTokenRepositoryAdapterTest {

    @Autowired
    private DeviceTokenRepositoryAdapter deviceTokenRepository;

    @Autowired
    private UserRepositoryAdapter userRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(User.builder()
                .id(1L)
                .email(new Email("user1@example.com"))
                .passwordHash("hash")
                .nickname("user1")
                .build());

        user2 = userRepository.save(User.builder()
                .id(2L)
                .email(new Email("user2@example.com"))
                .passwordHash("hash")
                .nickname("user2")
                .build());
    }

    @Nested
    @DisplayName("저장 시")
    class Save {

        @Test
        @DisplayName("디바이스 토큰을 저장한다")
        void should_saveDeviceToken_when_tokenProvided() {
            // given
            DeviceToken token = DeviceToken.builder()
                    .id(100L)
                    .userId(user1.getId())
                    .token("fcm-token-123")
                    .deviceType(DeviceType.ANDROID)
                    .build();

            // when
            DeviceToken saved = deviceTokenRepository.save(token);

            // then
            assertThat(saved.getId()).isEqualTo(100L);
            assertThat(saved.getToken()).isEqualTo("fcm-token-123");
            assertThat(saved.getDeviceType()).isEqualTo(DeviceType.ANDROID);
        }
    }

    @Nested
    @DisplayName("조회 시")
    class Find {

        @Test
        @DisplayName("ID로 디바이스 토큰을 조회한다")
        void should_findToken_when_idProvided() {
            // given
            deviceTokenRepository.save(DeviceToken.builder()
                    .id(100L)
                    .userId(user1.getId())
                    .token("fcm-token-123")
                    .deviceType(DeviceType.IOS)
                    .build());

            // when
            Optional<DeviceToken> found = deviceTokenRepository.findById(100L);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getToken()).isEqualTo("fcm-token-123");
        }

        @Test
        @DisplayName("토큰 값으로 디바이스 토큰을 조회한다")
        void should_findToken_when_tokenValueProvided() {
            // given
            deviceTokenRepository.save(DeviceToken.builder()
                    .id(100L)
                    .userId(user1.getId())
                    .token("unique-fcm-token")
                    .deviceType(DeviceType.ANDROID)
                    .build());

            // when
            Optional<DeviceToken> found = deviceTokenRepository.findByToken("unique-fcm-token");

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getUserId()).isEqualTo(user1.getId());
        }

        @Test
        @DisplayName("사용자 ID로 디바이스 토큰 목록을 조회한다")
        void should_findTokens_when_userIdProvided() {
            // given
            deviceTokenRepository.save(DeviceToken.builder()
                    .id(100L)
                    .userId(user1.getId())
                    .token("token-1")
                    .deviceType(DeviceType.ANDROID)
                    .build());
            deviceTokenRepository.save(DeviceToken.builder()
                    .id(101L)
                    .userId(user1.getId())
                    .token("token-2")
                    .deviceType(DeviceType.IOS)
                    .build());

            // when
            List<DeviceToken> tokens = deviceTokenRepository.findByUserId(user1.getId());

            // then
            assertThat(tokens).hasSize(2);
        }

        @Test
        @DisplayName("사용자 ID로 활성화된 디바이스 토큰만 조회한다")
        void should_findActiveTokens_when_userIdProvided() {
            // given
            DeviceToken activeToken = DeviceToken.builder()
                    .id(100L)
                    .userId(user1.getId())
                    .token("active-token")
                    .deviceType(DeviceType.ANDROID)
                    .build();
            DeviceToken inactiveToken = DeviceToken.builder()
                    .id(101L)
                    .userId(user1.getId())
                    .token("inactive-token")
                    .deviceType(DeviceType.IOS)
                    .build();
            inactiveToken.deactivate();

            deviceTokenRepository.save(activeToken);
            deviceTokenRepository.save(inactiveToken);

            // when
            List<DeviceToken> activeTokens = deviceTokenRepository.findActiveByUserId(user1.getId());

            // then
            assertThat(activeTokens).hasSize(1);
            assertThat(activeTokens.get(0).getToken()).isEqualTo("active-token");
        }

        @Test
        @DisplayName("여러 사용자 ID로 활성화된 디바이스 토큰을 조회한다")
        void should_findActiveTokens_when_multipleUserIdsProvided() {
            // given
            deviceTokenRepository.save(DeviceToken.builder()
                    .id(100L)
                    .userId(user1.getId())
                    .token("user1-token")
                    .deviceType(DeviceType.ANDROID)
                    .build());
            deviceTokenRepository.save(DeviceToken.builder()
                    .id(101L)
                    .userId(user2.getId())
                    .token("user2-token")
                    .deviceType(DeviceType.IOS)
                    .build());

            // when
            List<DeviceToken> tokens = deviceTokenRepository.findActiveByUserIds(
                    List.of(user1.getId(), user2.getId()));

            // then
            assertThat(tokens).hasSize(2);
        }
    }

    @Nested
    @DisplayName("토큰 소유자 이전 시 (RegisterDeviceTokenService 통합)")
    class TransferOwnership {

        @Test
        @DisplayName("다른 사용자가 같은 토큰을 등록해도 UNIQUE 충돌 없이 소유자가 이전되고 활성화된다")
        void should_transferOwnershipWithoutUniqueViolation_when_sameTokenRegisteredByAnotherUser() {
            // given: user1이 토큰을 비활성 상태로 보유
            RegisterDeviceTokenService service = new RegisterDeviceTokenService(
                    deviceTokenRepository,
                    new DeviceTokenInserter(deviceTokenRepository, new SnowflakeIdGenerator(0, 0)));
            String token = "shared-fcm-token";

            DeviceToken existing = DeviceToken.builder()
                    .id(500L)
                    .userId(user1.getId())
                    .token(token)
                    .deviceType(DeviceType.ANDROID)
                    .build();
            existing.deactivate();
            deviceTokenRepository.save(existing);

            // when: user2가 동일 토큰을 등록 (이전 케이스)
            // delete+insert 였다면 flush 순서에 따라 UNIQUE 제약 위반이 발생할 수 있음
            DeviceToken result = service.register(user2.getId(), token, DeviceType.IOS);

            // then: 예외 없이 소유자/타입/활성 상태가 갱신되고, 동일 토큰의 레코드는 1개만 존재
            assertThat(result.getUserId()).isEqualTo(user2.getId());
            assertThat(result.getDeviceType()).isEqualTo(DeviceType.IOS);
            assertThat(result.isActive()).isTrue();

            Optional<DeviceToken> found = deviceTokenRepository.findByToken(token);
            assertThat(found).isPresent();
            assertThat(found.get().getUserId()).isEqualTo(user2.getId());
            assertThat(found.get().isActive()).isTrue();
            assertThat(deviceTokenRepository.findByUserId(user1.getId())).isEmpty();
            assertThat(deviceTokenRepository.findByUserId(user2.getId())).hasSize(1);
        }
    }

    @Nested
    @DisplayName("삭제 시")
    class Delete {

        @Test
        @DisplayName("토큰 값으로 디바이스 토큰을 삭제한다")
        void should_deleteToken_when_tokenValueProvided() {
            // given
            deviceTokenRepository.save(DeviceToken.builder()
                    .id(100L)
                    .userId(user1.getId())
                    .token("token-to-delete")
                    .deviceType(DeviceType.ANDROID)
                    .build());

            // when
            deviceTokenRepository.deleteByToken("token-to-delete");

            // then
            assertThat(deviceTokenRepository.findByToken("token-to-delete")).isEmpty();
        }

        @Test
        @DisplayName("사용자 ID로 모든 디바이스 토큰을 삭제한다")
        void should_deleteAllTokens_when_userIdProvided() {
            // given
            deviceTokenRepository.save(DeviceToken.builder()
                    .id(100L)
                    .userId(user1.getId())
                    .token("token-1")
                    .deviceType(DeviceType.ANDROID)
                    .build());
            deviceTokenRepository.save(DeviceToken.builder()
                    .id(101L)
                    .userId(user1.getId())
                    .token("token-2")
                    .deviceType(DeviceType.IOS)
                    .build());

            // when
            deviceTokenRepository.deleteByUserId(user1.getId());

            // then
            assertThat(deviceTokenRepository.findByUserId(user1.getId())).isEmpty();
        }
    }
}
