package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.DeviceToken;
import com.cotalk.domain.port.inbound.RegisterDeviceTokenUseCase;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceController.class)
@AutoConfigureMockMvc(addFilters = false)
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegisterDeviceTokenUseCase registerDeviceTokenUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("디바이스 토큰 등록 API")
    class RegisterDeviceToken {

        @Test
        @DisplayName("유효한 요청으로 디바이스 토큰 등록 성공")
        void should_registerToken_when_validRequest() throws Exception {
            // given
            DeviceController.RegisterDeviceTokenRequest request = 
                    new DeviceController.RegisterDeviceTokenRequest(1L, "fcm-token-123", "ANDROID");

            DeviceToken savedToken = DeviceToken.builder()
                    .id(100L)
                    .userId(1L)
                    .token("fcm-token-123")
                    .deviceType(DeviceToken.DeviceType.ANDROID)
                    .build();

            given(registerDeviceTokenUseCase.register(eq(1L), eq("fcm-token-123"), eq(DeviceToken.DeviceType.ANDROID)))
                    .willReturn(savedToken);

            // when & then
            mockMvc.perform(post("/api/v1/devices/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tokenId").value(100L))
                    .andExpect(jsonPath("$.message").value("디바이스 토큰이 등록되었습니다."));
        }

        @Test
        @DisplayName("iOS 디바이스 토큰 등록 성공")
        void should_registerIosToken_when_iosDevice() throws Exception {
            // given
            DeviceController.RegisterDeviceTokenRequest request = 
                    new DeviceController.RegisterDeviceTokenRequest(1L, "apns-token-123", "IOS");

            DeviceToken savedToken = DeviceToken.builder()
                    .id(100L)
                    .userId(1L)
                    .token("apns-token-123")
                    .deviceType(DeviceToken.DeviceType.IOS)
                    .build();

            given(registerDeviceTokenUseCase.register(eq(1L), eq("apns-token-123"), eq(DeviceToken.DeviceType.IOS)))
                    .willReturn(savedToken);

            // when & then
            mockMvc.perform(post("/api/v1/devices/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tokenId").value(100L));
        }
    }

    @Nested
    @DisplayName("디바이스 토큰 삭제 API")
    class UnregisterDeviceToken {

        @Test
        @DisplayName("디바이스 토큰 삭제 성공")
        void should_deleteToken_when_validRequest() throws Exception {
            // given
            String token = "fcm-token-to-delete";
            willDoNothing().given(registerDeviceTokenUseCase).unregister(token);

            // when & then
            mockMvc.perform(delete("/api/v1/devices/token")
                            .param("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("디바이스 토큰이 삭제되었습니다."));
        }
    }
}
