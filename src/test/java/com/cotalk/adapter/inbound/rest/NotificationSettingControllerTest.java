package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.NotificationSetting;
import com.cotalk.domain.port.inbound.notification.GetNotificationSettingUseCase;
import com.cotalk.domain.port.inbound.notification.UpdateNotificationSettingUseCase;
import com.cotalk.infrastructure.exception.GlobalExceptionHandler;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import com.cotalk.infrastructure.security.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationSettingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RateLimitTestConfiguration.class, GlobalExceptionHandler.class})
class NotificationSettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetNotificationSettingUseCase getNotificationSettingUseCase;

    @MockitoBean
    private UpdateNotificationSettingUseCase updateNotificationSettingUseCase;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("알림 설정 조회 성공")
    @WithMockCustomUser(userId = 100L)
    void should_returnNotificationSetting_when_validUserId() throws Exception {
        // given
        Long userId = 100L;
        NotificationSetting setting = NotificationSetting.builder()
                .id(1L)
                .userId(userId)
                .messageNotification(true)
                .friendRequestNotification(true)
                .groupInviteNotification(true)
                .soundEnabled(true)
                .vibrationEnabled(true)
                .doNotDisturbEnabled(false)
                .build();

        given(getNotificationSettingUseCase.getNotificationSetting(userId))
                .willReturn(setting);

        // when & then
        mockMvc.perform(get("/api/v1/notifications/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.messageNotification").value(true))
                .andExpect(jsonPath("$.friendRequestNotification").value(true))
                .andExpect(jsonPath("$.soundEnabled").value(true));
    }

    @Test
    @DisplayName("알림 설정 업데이트 성공")
    @WithMockCustomUser(userId = 100L)
    void should_updateNotificationSetting_when_validRequest() throws Exception {
        // given
        Long userId = 100L;
        NotificationSetting updatedSetting = NotificationSetting.builder()
                .id(1L)
                .userId(userId)
                .messageNotification(false)
                .friendRequestNotification(false)
                .groupInviteNotification(true)
                .soundEnabled(false)
                .vibrationEnabled(true)
                .doNotDisturbEnabled(true)
                .doNotDisturbStart("22:00")
                .doNotDisturbEnd("07:00")
                .build();

        given(updateNotificationSettingUseCase.updateNotificationSetting(
                eq(userId), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(updatedSetting);

        String requestBody = """
                {
                    "messageNotification": false,
                    "friendRequestNotification": false,
                    "soundEnabled": false,
                    "doNotDisturbEnabled": true,
                    "doNotDisturbStart": "22:00",
                    "doNotDisturbEnd": "07:00"
                }
                """;

        // when & then
        mockMvc.perform(put("/api/v1/notifications/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.messageNotification").value(false))
                .andExpect(jsonPath("$.doNotDisturbEnabled").value(true))
                .andExpect(jsonPath("$.doNotDisturbStart").value("22:00"));
    }

    @Test
    @DisplayName("방해 금지 시간이 HH:mm 형식이 아니면 400을 반환한다")
    @WithMockCustomUser(userId = 100L)
    void should_return400_when_doNotDisturbTimeFormatInvalid() throws Exception {
        // 저장은 통과하고 푸시 발송 경로의 LocalTime.parse()에서
        // 지연 폭발(500)하던 입력을 저장 시점에 거부해야 한다.
        String requestBody = """
                {
                    "doNotDisturbEnabled": true,
                    "doNotDisturbStart": "25:99",
                    "doNotDisturbEnd": "07:00"
                }
                """;

        mockMvc.perform(put("/api/v1/notifications/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("방해 금지 시간에 콜론 없는 값이 오면 400을 반환한다")
    @WithMockCustomUser(userId = 100L)
    void should_return400_when_doNotDisturbTimeMissingColon() throws Exception {
        String requestBody = """
                {
                    "doNotDisturbEnabled": true,
                    "doNotDisturbStart": "0900",
                    "doNotDisturbEnd": "07:00"
                }
                """;

        mockMvc.perform(put("/api/v1/notifications/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("미리보기 모드에 허용되지 않은 값이 오면 400을 반환한다")
    @WithMockCustomUser(userId = 100L)
    void should_return400_when_previewModeInvalid() throws Exception {
        String requestBody = """
                {
                    "notificationPreviewMode": "EVERYTHING"
                }
                """;

        mockMvc.perform(put("/api/v1/notifications/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("허용된 미리보기 모드는 정상 처리된다")
    @WithMockCustomUser(userId = 100L)
    void should_updateSetting_when_previewModeValid() throws Exception {
        // given
        Long userId = 100L;
        NotificationSetting updatedSetting = NotificationSetting.builder()
                .id(1L)
                .userId(userId)
                .notificationPreviewMode("NAME_ONLY")
                .build();

        given(updateNotificationSettingUseCase.updateNotificationSetting(
                eq(userId), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(updatedSetting);

        String requestBody = """
                {
                    "notificationPreviewMode": "NAME_ONLY"
                }
                """;

        // when & then
        mockMvc.perform(put("/api/v1/notifications/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }
}
