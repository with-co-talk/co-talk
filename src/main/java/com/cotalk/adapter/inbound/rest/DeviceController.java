package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.auth.RegisterDeviceTokenRequest;
import com.cotalk.adapter.inbound.rest.dto.auth.RegisterDeviceTokenResponse;
import com.cotalk.adapter.inbound.rest.dto.common.MessageResponse;
import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.inbound.notification.RegisterDeviceTokenUseCase;
import com.cotalk.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 디바이스 토큰 관리를 위한 REST 컨트롤러.
 * 푸시 알림을 위한 FCM/APNs 토큰 등록 및 삭제 기능을 제공한다.
 *
 * @author seunggu.lee
 */
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Tag(name = "디바이스", description = "푸시 알림용 디바이스 토큰 관리 API")
public class DeviceController {

    private final RegisterDeviceTokenUseCase registerDeviceTokenUseCase;

    /**
     * 푸시 알림을 위한 FCM/APNs 토큰을 등록한다.
     *
     * @param principal 인증된 사용자 정보
     * @param request   디바이스 토큰 등록 요청 정보 (토큰, 디바이스 타입)
     * @return 등록된 토큰 ID와 성공 메시지
     */
    @Operation(summary = "디바이스 토큰 등록", description = "푸시 알림을 위한 FCM/APNs 토큰을 등록합니다.")
    @PostMapping("/token")
    public ResponseEntity<RegisterDeviceTokenResponse> registerDeviceToken(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody RegisterDeviceTokenRequest request) {

        DeviceToken.DeviceType deviceType = DeviceToken.DeviceType.valueOf(request.deviceType().toUpperCase());
        DeviceToken savedToken = registerDeviceTokenUseCase.register(
                principal.getUserId(),
                request.token(),
                deviceType
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RegisterDeviceTokenResponse.of(savedToken.getId(), "디바이스 토큰이 등록되었습니다."));
    }

    /**
     * 로그아웃 시 디바이스 토큰을 삭제한다.
     *
     * @param token 삭제할 디바이스 토큰
     * @return 삭제 완료 메시지
     */
    @Operation(summary = "디바이스 토큰 삭제", description = "로그아웃 시 디바이스 토큰을 삭제합니다.")
    @DeleteMapping("/token")
    public ResponseEntity<MessageResponse> unregisterDeviceToken(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam String token) {
        registerDeviceTokenUseCase.unregister(principal.getUserId(), token);
        return ResponseEntity.ok(MessageResponse.of("디바이스 토큰이 삭제되었습니다."));
    }
}
