package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.notification.NotificationSettingResponse;
import com.cotalk.adapter.inbound.rest.dto.notification.UpdateNotificationSettingRequest;
import com.cotalk.domain.entity.NotificationSetting;
import com.cotalk.domain.port.inbound.notification.GetNotificationSettingUseCase;
import com.cotalk.domain.port.inbound.notification.UpdateNotificationSettingUseCase;
import com.cotalk.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 설정 관리를 위한 REST 컨트롤러.
 * <p>
 * 사용자의 알림 설정 조회 및 업데이트 기능을 제공합니다.
 *
 * @author seunggu.lee
 */
@RestController
@RequestMapping("/api/v1/notifications/settings")
@RequiredArgsConstructor
@Tag(name = "알림 설정", description = "알림 설정 관리 API")
public class NotificationSettingController {

    private final GetNotificationSettingUseCase getNotificationSettingUseCase;
    private final UpdateNotificationSettingUseCase updateNotificationSettingUseCase;

    /**
     * 사용자의 알림 설정을 조회합니다.
     *
     * @param principal 인증된 사용자 정보
     * @return 알림 설정 정보
     */
    @Operation(summary = "알림 설정 조회", description = "사용자의 알림 설정을 조회합니다.")
    @GetMapping
    public ResponseEntity<NotificationSettingResponse> getNotificationSetting(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        NotificationSetting setting = getNotificationSettingUseCase.getNotificationSetting(principal.getUserId());
        return ResponseEntity.ok(NotificationSettingResponse.from(setting));
    }

    /**
     * 사용자의 알림 설정을 업데이트합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param request   알림 설정 업데이트 요청
     * @return 업데이트된 알림 설정 정보
     */
    @Operation(summary = "알림 설정 업데이트", description = "사용자의 알림 설정을 업데이트합니다.")
    @PutMapping
    public ResponseEntity<NotificationSettingResponse> updateNotificationSetting(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody UpdateNotificationSettingRequest request) {

        NotificationSetting setting = updateNotificationSettingUseCase.updateNotificationSetting(
                principal.getUserId(),
                request.messageNotification(),
                request.friendRequestNotification(),
                request.groupInviteNotification(),
                request.notificationPreviewMode(),
                request.soundEnabled(),
                request.vibrationEnabled(),
                request.doNotDisturbEnabled(),
                request.doNotDisturbStart(),
                request.doNotDisturbEnd()
        );

        return ResponseEntity.ok(NotificationSettingResponse.from(setting));
    }
}
