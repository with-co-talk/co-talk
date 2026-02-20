package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.NotificationSetting;
import com.cotalk.domain.port.inbound.notification.GetNotificationSettingUseCase;
import com.cotalk.domain.port.inbound.notification.UpdateNotificationSettingUseCase;
import com.cotalk.infrastructure.exception.GlobalExceptionHandler;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import com.cotalk.infrastructure.security.WithMockCustomUser;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Autowired
    private ObjectMapper objectMapper;

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
}
