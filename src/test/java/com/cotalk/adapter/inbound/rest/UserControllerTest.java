package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.SearchUserUseCase;
import com.cotalk.domain.port.inbound.UpdateProfileUseCase;
import com.cotalk.domain.port.inbound.UpdateUserOnlineStatusUseCase;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SearchUserUseCase searchUserUseCase;

    @MockBean
    private UpdateProfileUseCase updateProfileUseCase;

    @MockBean
    private UpdateUserOnlineStatusUseCase updateUserOnlineStatusUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("사용자 검색 API")
    class SearchUserApi {

        @Test
        @DisplayName("닉네임으로 사용자 검색 성공 - avatarUrl 포함")
        void should_returnUsers_when_searchByNickname() throws Exception {
            // given
            List<User> users = List.of(
                    User.builder()
                            .id(1L)
                            .email("user1@example.com")
                            .nickname("테스트유저1")
                            .passwordHash("hash")
                            .avatarUrl("https://example.com/avatar1.png")
                            .build(),
                    User.builder()
                            .id(2L)
                            .email("user2@example.com")
                            .nickname("테스트유저2")
                            .passwordHash("hash")
                            .avatarUrl("https://example.com/avatar2.png")
                            .build()
            );

            given(searchUserUseCase.searchByNickname(anyString())).willReturn(users);

            // when & then
            mockMvc.perform(get("/api/v1/users/search")
                            .param("nickname", "테스트"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users").isArray())
                    .andExpect(jsonPath("$.users.length()").value(2))
                    .andExpect(jsonPath("$.users[0].nickname").value("테스트유저1"))
                    .andExpect(jsonPath("$.users[0].avatarUrl").value("https://example.com/avatar1.png"))
                    .andExpect(jsonPath("$.users[1].avatarUrl").value("https://example.com/avatar2.png"));
        }

        @Test
        @DisplayName("검색 결과가 없을 때 빈 배열 반환")
        void should_returnEmptyArray_when_noResults() throws Exception {
            // given
            given(searchUserUseCase.searchByNickname(anyString())).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/users/search")
                            .param("nickname", "없는유저"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users").isArray())
                    .andExpect(jsonPath("$.users.length()").value(0));
        }
    }

    @Nested
    @DisplayName("프로필 수정 API")
    class UpdateProfileApi {

        @Test
        @DisplayName("유효한 요청으로 프로필 수정 성공")
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long userId = 1L;
            UserController.UpdateProfileRequest request = new UserController.UpdateProfileRequest(
                    "새닉네임", "https://example.com/avatar.png");

            willDoNothing().given(updateProfileUseCase).updateProfile(anyLong(), anyString(), anyString());

            // when & then
            mockMvc.perform(put("/api/v1/users/{userId}/profile", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("프로필이 수정되었습니다."));
        }

        @Test
        @DisplayName("닉네임만 수정")
        void should_returnOk_when_updateNicknameOnly() throws Exception {
            // given
            Long userId = 1L;
            UserController.UpdateProfileRequest request = new UserController.UpdateProfileRequest(
                    "새닉네임", null);

            willDoNothing().given(updateProfileUseCase).updateProfile(anyLong(), anyString(), isNull());

            // when & then
            mockMvc.perform(put("/api/v1/users/{userId}/profile", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("프로필이 수정되었습니다."));
        }
    }
}
