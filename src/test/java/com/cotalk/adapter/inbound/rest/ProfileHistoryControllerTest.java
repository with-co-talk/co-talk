package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.profile.CreateProfileHistoryRequest;
import com.cotalk.adapter.inbound.rest.dto.profile.UpdateProfileHistoryRequest;
import com.cotalk.domain.entity.ProfileHistory;
import com.cotalk.domain.entity.ProfileHistoryType;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.profile.CreateProfileHistoryUseCase;
import com.cotalk.domain.port.inbound.profile.DeleteProfileHistoryUseCase;
import com.cotalk.domain.port.inbound.profile.GetProfileHistoryUseCase;
import com.cotalk.domain.port.inbound.profile.SetCurrentProfileUseCase;
import com.cotalk.domain.port.inbound.profile.UpdateProfileHistoryUseCase;
import com.cotalk.infrastructure.exception.GlobalExceptionHandler;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import com.cotalk.infrastructure.security.WithMockCustomUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 프로필 이력 컨트롤러 테스트.
 *
 * @author seunggu.lee
 */
@WebMvcTest(ProfileHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RateLimitTestConfiguration.class, GlobalExceptionHandler.class})
class ProfileHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GetProfileHistoryUseCase getProfileHistoryUseCase;

    @MockBean
    private CreateProfileHistoryUseCase createProfileHistoryUseCase;

    @MockBean
    private UpdateProfileHistoryUseCase updateProfileHistoryUseCase;

    @MockBean
    private DeleteProfileHistoryUseCase deleteProfileHistoryUseCase;

    @MockBean
    private SetCurrentProfileUseCase setCurrentProfileUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("프로필 이력 조회 API")
    class GetProfileHistoryApi {

        @Test
        @DisplayName("프로필 이력 조회 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnHistories_when_validRequest() throws Exception {
            // given
            Long userId = 1L;
            List<ProfileHistory> histories = List.of(
                    ProfileHistory.builder()
                            .id(1L)
                            .userId(userId)
                            .type(ProfileHistoryType.AVATAR)
                            .url("https://example.com/avatar1.png")
                            .isPrivate(false)
                            .isCurrent(true)
                            .build(),
                    ProfileHistory.builder()
                            .id(2L)
                            .userId(userId)
                            .type(ProfileHistoryType.AVATAR)
                            .url("https://example.com/avatar2.png")
                            .isPrivate(false)
                            .isCurrent(false)
                            .build()
            );

            given(getProfileHistoryUseCase.getProfileHistory(anyLong(), isNull(), anyLong()))
                    .willReturn(histories);

            // when & then
            mockMvc.perform(get("/api/v1/users/{userId}/profile/history", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.histories").isArray())
                    .andExpect(jsonPath("$.histories.length()").value(2))
                    .andExpect(jsonPath("$.histories[0].id").value(1))
                    .andExpect(jsonPath("$.histories[0].type").value("AVATAR"))
                    .andExpect(jsonPath("$.histories[0].url").value("https://example.com/avatar1.png"))
                    .andExpect(jsonPath("$.histories[0].isCurrent").value(true));
        }

        @Test
        @DisplayName("타입으로 필터링하여 조회 성공")
        @WithMockCustomUser(userId = 1L)
        void should_filterByType_when_typeProvided() throws Exception {
            // given
            Long userId = 1L;
            ProfileHistoryType type = ProfileHistoryType.AVATAR;

            List<ProfileHistory> histories = List.of(
                    ProfileHistory.builder()
                            .id(1L)
                            .userId(userId)
                            .type(ProfileHistoryType.AVATAR)
                            .url("https://example.com/avatar1.png")
                            .isPrivate(false)
                            .isCurrent(true)
                            .build()
            );

            given(getProfileHistoryUseCase.getProfileHistory(anyLong(), any(ProfileHistoryType.class), anyLong()))
                    .willReturn(histories);

            // when & then
            mockMvc.perform(get("/api/v1/users/{userId}/profile/history", userId)
                            .param("type", type.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.histories").isArray())
                    .andExpect(jsonPath("$.histories.length()").value(1))
                    .andExpect(jsonPath("$.histories[0].type").value("AVATAR"));
        }
    }

    @Nested
    @DisplayName("프로필 이력 생성 API")
    class CreateProfileHistoryApi {

        @Test
        @DisplayName("유효한 요청으로 프로필 이력 생성 성공")
        @WithMockCustomUser(userId = 1L)
        void should_createHistory_when_validRequest() throws Exception {
            // given
            Long userId = 1L;
            CreateProfileHistoryRequest request = new CreateProfileHistoryRequest(
                    ProfileHistoryType.AVATAR,
                    "https://example.com/avatar.png",
                    null,
                    false,
                    true
            );

            ProfileHistory created = ProfileHistory.builder()
                    .id(1L)
                    .userId(userId)
                    .type(ProfileHistoryType.AVATAR)
                    .url("https://example.com/avatar.png")
                    .isPrivate(false)
                    .isCurrent(true)
                    .build();

            given(createProfileHistoryUseCase.createProfileHistory(
                    anyLong(), any(ProfileHistoryType.class), anyString(), isNull(), anyBoolean(), anyBoolean()))
                    .willReturn(created);

            // when & then
            mockMvc.perform(post("/api/v1/users/{userId}/profile/history", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.type").value("AVATAR"))
                    .andExpect(jsonPath("$.url").value("https://example.com/avatar.png"))
                    .andExpect(jsonPath("$.isCurrent").value(true));
        }

        @Test
        @DisplayName("다른 사용자의 프로필 이력 생성 시 403 에러")
        @WithMockCustomUser(userId = 1L)
        void should_return403_when_notOwner() throws Exception {
            // given
            Long currentUserId = 1L;
            Long targetUserId = 2L;
            CreateProfileHistoryRequest request = new CreateProfileHistoryRequest(
                    ProfileHistoryType.AVATAR,
                    "https://example.com/avatar.png",
                    null,
                    false,
                    true
            );

            // when & then
            mockMvc.perform(post("/api/v1/users/{userId}/profile/history", targetUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("필수 필드 누락 시 400 에러")
        @WithMockCustomUser(userId = 1L)
        void should_return400_when_invalidRequest() throws Exception {
            // given
            Long userId = 1L;
            String invalidRequest = "{}";

            // when & then
            mockMvc.perform(post("/api/v1/users/{userId}/profile/history", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("프로필 이력 수정 API")
    class UpdateProfileHistoryApi {

        @Test
        @DisplayName("유효한 요청으로 공개 설정 수정 성공")
        @WithMockCustomUser(userId = 1L)
        void should_updatePrivacy_when_validRequest() throws Exception {
            // given
            Long userId = 1L;
            Long historyId = 1L;
            UpdateProfileHistoryRequest request = new UpdateProfileHistoryRequest(true);

            willDoNothing().given(updateProfileHistoryUseCase)
                    .updatePrivacy(anyLong(), anyLong(), anyBoolean());

            // when & then
            mockMvc.perform(put("/api/v1/users/{userId}/profile/history/{historyId}", userId, historyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("프로필 이력이 수정되었습니다."));
        }

        @Test
        @DisplayName("다른 사용자의 프로필 이력 수정 시 403 에러")
        @WithMockCustomUser(userId = 1L)
        void should_return403_when_notOwner() throws Exception {
            // given
            Long currentUserId = 1L;
            Long targetUserId = 2L;
            Long historyId = 1L;
            UpdateProfileHistoryRequest request = new UpdateProfileHistoryRequest(true);

            // when & then
            mockMvc.perform(put("/api/v1/users/{userId}/profile/history/{historyId}", targetUserId, historyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("프로필 이력 삭제 API")
    class DeleteProfileHistoryApi {

        @Test
        @DisplayName("유효한 요청으로 프로필 이력 삭제 성공")
        @WithMockCustomUser(userId = 1L)
        void should_deleteHistory_when_validRequest() throws Exception {
            // given
            Long userId = 1L;
            Long historyId = 1L;

            willDoNothing().given(deleteProfileHistoryUseCase)
                    .deleteProfileHistory(anyLong(), anyLong());

            // when & then
            mockMvc.perform(delete("/api/v1/users/{userId}/profile/history/{historyId}", userId, historyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("프로필 이력이 삭제되었습니다."));
        }

        @Test
        @DisplayName("다른 사용자의 프로필 이력 삭제 시 403 에러")
        @WithMockCustomUser(userId = 1L)
        void should_return403_when_notOwner() throws Exception {
            // given
            Long currentUserId = 1L;
            Long targetUserId = 2L;
            Long historyId = 1L;

            // when & then
            mockMvc.perform(delete("/api/v1/users/{userId}/profile/history/{historyId}", targetUserId, historyId))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("현재 프로필 설정 API")
    class SetCurrentProfileApi {

        @Test
        @DisplayName("유효한 요청으로 현재 프로필 설정 성공")
        @WithMockCustomUser(userId = 1L)
        void should_setAsCurrent_when_validRequest() throws Exception {
            // given
            Long userId = 1L;
            Long historyId = 1L;

            willDoNothing().given(setCurrentProfileUseCase)
                    .setCurrentProfile(anyLong(), anyLong());

            // when & then
            mockMvc.perform(put("/api/v1/users/{userId}/profile/history/{historyId}/current", userId, historyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("현재 프로필로 설정되었습니다."));
        }

        @Test
        @DisplayName("다른 사용자의 프로필을 현재 프로필로 설정 시 403 에러")
        @WithMockCustomUser(userId = 1L)
        void should_return403_when_notOwner() throws Exception {
            // given
            Long currentUserId = 1L;
            Long targetUserId = 2L;
            Long historyId = 1L;

            // when & then
            mockMvc.perform(put("/api/v1/users/{userId}/profile/history/{historyId}/current", targetUserId, historyId))
                    .andExpect(status().isForbidden());
        }
    }
}
