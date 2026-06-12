package com.cotalk.application.service.notification;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * {@link RegisterDeviceTokenService} 단위 테스트.
 *
 * <p>이 단위 테스트는 mock 기반으로 <b>라우팅/위임 로직</b>(기존 토큰 분기, 신규 INSERT
 * 위임, UNIQUE 위반 시 폴백 경로 선택)만 검증한다. 신규 INSERT는 별도 빈
 * {@link DeviceTokenInserter}({@code REQUIRES_NEW})에 위임되므로, 실제 트랜잭션 거동
 * (REQUIRES_NEW 경계에서의 위반 롤백 → 바깥 트랜잭션 비오염 → 폴백 UPDATE 커밋 성공)은
 * 실제 H2 UNIQUE 제약을 사용하는 {@code RegisterDeviceTokenServiceConcurrencyTest}
 * ({@code @DataJpaTest})에서 검증한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class RegisterDeviceTokenServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private DeviceTokenInserter deviceTokenInserter;

    private RegisterDeviceTokenService registerDeviceTokenService;

    @BeforeEach
    void setUp() {
        registerDeviceTokenService = new RegisterDeviceTokenService(deviceTokenRepository, deviceTokenInserter);
    }

    @Nested
    @DisplayName("디바이스 토큰 등록")
    class Register {

        @Test
        @DisplayName("새 토큰 등록은 DeviceTokenInserter(REQUIRES_NEW)에 위임한다")
        void should_delegateToInserter_when_tokenNotExists() {
            // given
            Long userId = 1L;
            String token = "new-fcm-token";
            DeviceToken.DeviceType deviceType = DeviceToken.DeviceType.ANDROID;

            DeviceToken inserted = DeviceToken.builder()
                    .id(100L)
                    .userId(userId)
                    .token(token)
                    .deviceType(deviceType)
                    .build();

            given(deviceTokenRepository.findByToken(token)).willReturn(Optional.empty());
            given(deviceTokenInserter.insertNew(userId, token, deviceType)).willReturn(inserted);

            // when
            DeviceToken result = registerDeviceTokenService.register(userId, token, deviceType);

            // then: 신규 INSERT는 inserter에 위임되고, 서비스가 직접 save()로 INSERT하지 않는다
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getToken()).isEqualTo(token);
            verify(deviceTokenInserter).insertNew(userId, token, deviceType);
            verify(deviceTokenRepository, org.mockito.Mockito.never()).save(any(DeviceToken.class));
        }

        @Test
        @DisplayName("기존 토큰 업데이트 - 같은 사용자")
        void should_updateToken_when_tokenExistsForSameUser() {
            // given
            Long userId = 1L;
            String token = "existing-fcm-token";
            DeviceToken.DeviceType deviceType = DeviceToken.DeviceType.ANDROID;

            DeviceToken existingToken = DeviceToken.builder()
                    .id(100L)
                    .userId(userId)
                    .token(token)
                    .deviceType(deviceType)
                    .build();
            existingToken.deactivate();

            given(deviceTokenRepository.findByToken(token)).willReturn(Optional.of(existingToken));
            given(deviceTokenRepository.save(any(DeviceToken.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            DeviceToken result = registerDeviceTokenService.register(userId, token, deviceType);

            // then: 기존 레코드 재사용 경로는 inserter를 거치지 않는다
            assertThat(result.isActive()).isTrue();
            verify(deviceTokenInserter, org.mockito.Mockito.never()).insertNew(any(), any(), any());
        }

        @Test
        @DisplayName("토큰이 다른 사용자에게 있으면 delete+insert 없이 기존 레코드를 UPDATE하여 새 사용자로 이전")
        void should_transferToken_when_tokenBelongsToAnotherUser() {
            // given
            Long newUserId = 2L;
            String token = "existing-fcm-token";
            DeviceToken.DeviceType newDeviceType = DeviceToken.DeviceType.IOS;

            DeviceToken existingToken = DeviceToken.builder()
                    .id(100L)
                    .userId(1L)  // 다른 사용자
                    .token(token)
                    .deviceType(DeviceToken.DeviceType.ANDROID)
                    .build();
            existingToken.deactivate();

            given(deviceTokenRepository.findByToken(token)).willReturn(Optional.of(existingToken));
            given(deviceTokenRepository.save(any(DeviceToken.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            DeviceToken result = registerDeviceTokenService.register(newUserId, token, newDeviceType);

            // then: UNIQUE 충돌을 유발할 수 있는 delete는 호출되지 않아야 한다
            verify(deviceTokenRepository, org.mockito.Mockito.never()).deleteByToken(token);
            verify(deviceTokenInserter, org.mockito.Mockito.never()).insertNew(any(), any(), any());
            // 기존 레코드의 ID를 유지한 채 소유자/타입/활성 상태만 갱신
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getUserId()).isEqualTo(newUserId);
            assertThat(result.getToken()).isEqualTo(token);
            assertThat(result.getDeviceType()).isEqualTo(newDeviceType);
            assertThat(result.isActive()).isTrue();
            verify(deviceTokenRepository).save(existingToken);
        }

        @Test
        @DisplayName("신규 INSERT가 UNIQUE 위반 시 재조회 후 같은 사용자로 UPDATE 폴백 경로를 탄다")
        void should_routeToReactivateFallback_when_inserterThrowsForSameUser() {
            // given: inserter(REQUIRES_NEW)가 UNIQUE 위반을 던지면(독립 트랜잭션에서 롤백됨),
            //        바깥 트랜잭션은 깨끗하므로 재조회 후 UPDATE 폴백을 수행
            Long userId = 1L;
            String token = "racing-fcm-token";
            DeviceToken.DeviceType deviceType = DeviceToken.DeviceType.ANDROID;

            DeviceToken concurrentlyInserted = DeviceToken.builder()
                    .id(200L)
                    .userId(userId)
                    .token(token)
                    .deviceType(deviceType)
                    .build();
            concurrentlyInserted.deactivate();

            given(deviceTokenRepository.findByToken(token))
                    .willReturn(Optional.empty())                    // 최초 조회: 신규로 판단
                    .willReturn(Optional.of(concurrentlyInserted));  // 폴백 재조회: 경쟁 레코드 발견
            given(deviceTokenInserter.insertNew(userId, token, deviceType))
                    .willThrow(new DataIntegrityViolationException("duplicate token"));
            given(deviceTokenRepository.save(any(DeviceToken.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            DeviceToken result = registerDeviceTokenService.register(userId, token, deviceType);

            // then: 새 ID로 INSERT가 아닌, 기존 경쟁 레코드(200L)를 UPDATE하여 반환
            assertThat(result.getId()).isEqualTo(200L);
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.isActive()).isTrue();
            verify(deviceTokenRepository, org.mockito.Mockito.times(2)).findByToken(token);
            verify(deviceTokenRepository).save(concurrentlyInserted);
        }

        @Test
        @DisplayName("신규 INSERT가 UNIQUE 위반 시 다른 사용자 레코드를 소유자 이전 UPDATE 폴백 경로를 탄다")
        void should_routeToTransferFallback_when_inserterThrowsForAnotherUser() {
            // given: 다른 사용자가 먼저 같은 토큰을 INSERT한 상태에서 우리 INSERT가 UNIQUE 위반
            Long newUserId = 2L;
            String token = "racing-fcm-token";
            DeviceToken.DeviceType newDeviceType = DeviceToken.DeviceType.IOS;

            DeviceToken concurrentlyInserted = DeviceToken.builder()
                    .id(300L)
                    .userId(1L) // 다른 사용자
                    .token(token)
                    .deviceType(DeviceToken.DeviceType.ANDROID)
                    .build();

            given(deviceTokenRepository.findByToken(token))
                    .willReturn(Optional.empty())
                    .willReturn(Optional.of(concurrentlyInserted));
            given(deviceTokenInserter.insertNew(newUserId, token, newDeviceType))
                    .willThrow(new DataIntegrityViolationException("duplicate token"));
            given(deviceTokenRepository.save(any(DeviceToken.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            DeviceToken result = registerDeviceTokenService.register(newUserId, token, newDeviceType);

            // then: 경쟁 레코드의 소유자를 우리 사용자로 이전(UPDATE)
            assertThat(result.getId()).isEqualTo(300L);
            assertThat(result.getUserId()).isEqualTo(newUserId);
            assertThat(result.getDeviceType()).isEqualTo(newDeviceType);
            assertThat(result.isActive()).isTrue();
            verify(deviceTokenRepository, org.mockito.Mockito.never()).deleteByToken(token);
            verify(deviceTokenRepository).save(concurrentlyInserted);
        }
    }

    @Nested
    @DisplayName("디바이스 토큰 삭제")
    class Unregister {

        @Test
        @DisplayName("토큰 삭제 성공 - 소유자 일치")
        void should_deleteToken_when_unregister() {
            // given
            Long userId = 1L;
            String token = "fcm-token-to-delete";
            DeviceToken existingToken = DeviceToken.builder()
                    .id(100L)
                    .userId(userId)
                    .token(token)
                    .deviceType(DeviceToken.DeviceType.ANDROID)
                    .build();
            given(deviceTokenRepository.findByToken(token)).willReturn(Optional.of(existingToken));

            // when
            registerDeviceTokenService.unregister(userId, token);

            // then
            verify(deviceTokenRepository).deleteByToken(token);
        }

        @Test
        @DisplayName("다른 사용자의 토큰 삭제 시도 시 무시")
        void should_ignore_when_unregisterOtherUsersToken() {
            // given
            Long requesterId = 2L;
            String token = "fcm-token-to-delete";
            DeviceToken existingToken = DeviceToken.builder()
                    .id(100L)
                    .userId(1L)
                    .token(token)
                    .deviceType(DeviceToken.DeviceType.ANDROID)
                    .build();
            given(deviceTokenRepository.findByToken(token)).willReturn(Optional.of(existingToken));

            // when
            registerDeviceTokenService.unregister(requesterId, token);

            // then
            verify(deviceTokenRepository, org.mockito.Mockito.never()).deleteByToken(token);
        }
    }
}
