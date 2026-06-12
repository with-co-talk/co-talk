package com.cotalk.application.service.notification;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegisterDeviceTokenServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private SnowflakeIdGenerator idGenerator;

    private RegisterDeviceTokenService registerDeviceTokenService;

    @BeforeEach
    void setUp() {
        registerDeviceTokenService = new RegisterDeviceTokenService(deviceTokenRepository, idGenerator);
    }

    @Nested
    @DisplayName("디바이스 토큰 등록")
    class Register {

        @Test
        @DisplayName("새 토큰 등록 성공")
        void should_registerNewToken_when_tokenNotExists() {
            // given
            Long userId = 1L;
            String token = "new-fcm-token";
            DeviceToken.DeviceType deviceType = DeviceToken.DeviceType.ANDROID;

            given(deviceTokenRepository.findByToken(token)).willReturn(Optional.empty());
            given(idGenerator.nextId()).willReturn(100L);
            given(deviceTokenRepository.save(any(DeviceToken.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            DeviceToken result = registerDeviceTokenService.register(userId, token, deviceType);

            // then
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getToken()).isEqualTo(token);
            assertThat(result.getDeviceType()).isEqualTo(deviceType);
            assertThat(result.isActive()).isTrue();
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

            // then
            assertThat(result.isActive()).isTrue();
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
            // 새 엔티티를 만들지 않으므로 ID 생성도 호출되지 않는다 (동일 레코드 재사용)
            verify(idGenerator, org.mockito.Mockito.never()).nextId();
            // 기존 레코드의 ID를 유지한 채 소유자/타입/활성 상태만 갱신
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getUserId()).isEqualTo(newUserId);
            assertThat(result.getToken()).isEqualTo(token);
            assertThat(result.getDeviceType()).isEqualTo(newDeviceType);
            assertThat(result.isActive()).isTrue();
            verify(deviceTokenRepository).save(existingToken);
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
