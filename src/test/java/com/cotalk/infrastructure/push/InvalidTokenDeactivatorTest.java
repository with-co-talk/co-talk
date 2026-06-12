package com.cotalk.infrastructure.push;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.outbound.DeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * InvalidTokenDeactivator 테스트.
 *
 * <p>FCM 전송 실패 토큰의 비활성화(DB 쓰기)가 FCM 전송과 분리된 컴포넌트에서
 * 수행되는지 검증한다. 트랜잭션 적용 자체는 별도 빈으로 분리하여 self-invocation
 * 프록시 함정을 회피했으며, 본 단위 테스트는 비활성화 로직의 동작을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvalidTokenDeactivator")
class InvalidTokenDeactivatorTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    private InvalidTokenDeactivator deactivator;

    @BeforeEach
    void setUp() {
        deactivator = new InvalidTokenDeactivator(deviceTokenRepository);
    }

    @Test
    @DisplayName("존재하는 토큰을 비활성화하고 저장한다")
    void should_deactivateAndSave_when_tokenExists() {
        // given
        String token = "invalid-token";
        DeviceToken deviceToken = DeviceToken.builder()
                .id(1L)
                .userId(1L)
                .token(token)
                .deviceType(DeviceToken.DeviceType.ANDROID)
                .active(true)
                .build();
        given(deviceTokenRepository.findByToken(token)).willReturn(Optional.of(deviceToken));

        // when
        deactivator.deactivateToken(token);

        // then
        verify(deviceTokenRepository).save(deviceToken);
        assertThat(deviceToken.isActive()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 토큰은 저장을 건너뛴다")
    void should_skipSave_when_tokenNotFound() {
        // given
        String token = "unknown-token";
        given(deviceTokenRepository.findByToken(token)).willReturn(Optional.empty());

        // when
        deactivator.deactivateToken(token);

        // then
        verify(deviceTokenRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("여러 토큰을 비활성화한다")
    void should_deactivateMultiple_when_tokensProvided() {
        // given
        DeviceToken token1 = DeviceToken.builder()
                .id(1L).userId(1L).token("t1")
                .deviceType(DeviceToken.DeviceType.ANDROID).active(true).build();
        DeviceToken token2 = DeviceToken.builder()
                .id(2L).userId(2L).token("t2")
                .deviceType(DeviceToken.DeviceType.IOS).active(true).build();
        given(deviceTokenRepository.findByToken("t1")).willReturn(Optional.of(token1));
        given(deviceTokenRepository.findByToken("t2")).willReturn(Optional.of(token2));

        // when
        deactivator.deactivateTokens(List.of("t1", "t2"));

        // then
        verify(deviceTokenRepository).save(token1);
        verify(deviceTokenRepository).save(token2);
        assertThat(token1.isActive()).isFalse();
        assertThat(token2.isActive()).isFalse();
    }

    @Test
    @DisplayName("목록 중 존재하지 않는 토큰은 건너뛴다")
    void should_skipMissingTokens_when_someTokensNotFound() {
        // given
        DeviceToken token1 = DeviceToken.builder()
                .id(1L).userId(1L).token("t1")
                .deviceType(DeviceToken.DeviceType.ANDROID).active(true).build();
        given(deviceTokenRepository.findByToken("t1")).willReturn(Optional.of(token1));
        given(deviceTokenRepository.findByToken("missing")).willReturn(Optional.empty());

        // when
        deactivator.deactivateTokens(List.of("t1", "missing"));

        // then
        verify(deviceTokenRepository).save(token1);
        verify(deviceTokenRepository, never()).save(org.mockito.ArgumentMatchers.argThat(
                dt -> dt != null && "missing".equals(dt.getToken())));
    }
}
