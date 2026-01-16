package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.inbound.RegisterDeviceTokenUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Tag(name = "디바이스", description = "푸시 알림용 디바이스 토큰 관리 API")
public class DeviceController {

    private final RegisterDeviceTokenUseCase registerDeviceTokenUseCase;

    @Operation(summary = "디바이스 토큰 등록", description = "푸시 알림을 위한 FCM/APNs 토큰을 등록합니다.")
    @PostMapping("/token")
    public ResponseEntity<RegisterDeviceTokenResponse> registerDeviceToken(
            @Valid @RequestBody RegisterDeviceTokenRequest request) {
        
        DeviceToken.DeviceType deviceType = DeviceToken.DeviceType.valueOf(request.deviceType().toUpperCase());
        DeviceToken savedToken = registerDeviceTokenUseCase.register(
                request.userId(), 
                request.token(), 
                deviceType
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterDeviceTokenResponse(savedToken.getId(), "디바이스 토큰이 등록되었습니다."));
    }

    @Operation(summary = "디바이스 토큰 삭제", description = "로그아웃 시 디바이스 토큰을 삭제합니다.")
    @DeleteMapping("/token")
    public ResponseEntity<DeleteDeviceTokenResponse> unregisterDeviceToken(@RequestParam String token) {
        registerDeviceTokenUseCase.unregister(token);
        return ResponseEntity.ok(new DeleteDeviceTokenResponse("디바이스 토큰이 삭제되었습니다."));
    }

    // Request DTOs
    public record RegisterDeviceTokenRequest(
            @NotNull(message = "사용자 ID는 필수입니다.")
            Long userId,

            @NotBlank(message = "디바이스 토큰은 필수입니다.")
            String token,

            @NotBlank(message = "디바이스 타입은 필수입니다. (ANDROID, IOS, WEB)")
            String deviceType
    ) {}

    // Response DTOs
    public record RegisterDeviceTokenResponse(Long tokenId, String message) {}
    public record DeleteDeviceTokenResponse(String message) {}
}
