package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.notification.NotificationSettingResponse;
import com.cotalk.adapter.inbound.rest.dto.notification.UpdateNotificationSettingRequest;
import com.cotalk.domain.entity.NotificationSetting;
import com.cotalk.domain.port.inbound.notification.GetNotificationSettingUseCase;
import com.cotalk.domain.port.inbound.notification.UpdateNotificationSettingUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
     * @param userId 사용자 ID
     * @return 알림 설정 정보
     */
    @Operation(summary = "알림 설정 조회", description = "사용자의 알림 설정을 조회합니다.")
    @GetMapping
    public ResponseEntity<NotificationSettingResponse> getNotificationSetting(@RequestParam Long userId) {
        NotificationSetting setting = getNotificationSettingUseCase.getNotificationSetting(userId);
        return ResponseEntity.ok(NotificationSettingResponse.from(setting));
    }

    /**
     * 사용자의 알림 설정을 업데이트합니다.
     *
     * @param userId  사용자 ID
     * @param request 알림 설정 업데이트 요청
     * @return 업데이트된 알림 설정 정보
     */
    @Operation(summary = "알림 설정 업데이트", description = "사용자의 알림 설정을 업데이트합니다.")
    @PutMapping
    public ResponseEntity<NotificationSettingResponse> updateNotificationSetting(
            @RequestParam Long userId,
            @RequestBody UpdateNotificationSettingRequest request) {

        NotificationSetting setting = updateNotificationSettingUseCase.updateNotificationSetting(
                userId,
                request.messageNotification(),
                request.friendRequestNotification(),
                request.groupInviteNotification(),
                request.soundEnabled(),
                request.vibrationEnabled(),
                request.doNotDisturbEnabled(),
                request.doNotDisturbStart(),
                request.doNotDisturbEnd()
        );

        return ResponseEntity.ok(NotificationSettingResponse.from(setting));
    }
}
