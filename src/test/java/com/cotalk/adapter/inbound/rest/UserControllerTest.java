package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.user.UpdateProfileRequest;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.user.SearchUserUseCase;
import com.cotalk.domain.port.inbound.user.UpdateProfileUseCase;
import com.cotalk.domain.port.inbound.user.UpdateUserOnlineStatusUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import com.cotalk.infrastructure.security.SecurityContextHelper;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RateLimitTestConfiguration.class)
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
    private UserRepository userRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private SecurityContextHelper securityContextHelper;

    @BeforeEach
    void setUp() {
        // 기본적으로 userId 1L로 인증된 사용자 설정
        given(securityContextHelper.getCurrentUserId()).willReturn(1L);
    }

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
        @DisplayName("query 파라미터로 사용자 검색 성공")
        void should_returnUsers_when_searchByQuery() throws Exception {
            // given
            List<User> users = List.of(
                    User.builder()
                            .id(1L)
                            .email("user1@example.com")
                            .nickname("테스트유저1")
                            .passwordHash("hash")
                            .avatarUrl("https://example.com/avatar1.png")
                            .build()
            );

            given(searchUserUseCase.searchByNickname(anyString())).willReturn(users);

            // when & then
            mockMvc.perform(get("/api/v1/users/search")
                            .param("query", "테스트"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users").isArray())
                    .andExpect(jsonPath("$.users.length()").value(1))
                    .andExpect(jsonPath("$.users[0].nickname").value("테스트유저1"));
        }

        @Test
        @DisplayName("query 파라미터가 nickname보다 우선순위가 높음")
        void should_useQuery_when_bothQueryAndNicknameProvided() throws Exception {
            // given
            List<User> users = List.of(
                    User.builder()
                            .id(1L)
                            .email("user1@example.com")
                            .nickname("테스트유저1")
                            .passwordHash("hash")
                            .avatarUrl("https://example.com/avatar1.png")
                            .build()
            );

            given(searchUserUseCase.searchByNickname("query값")).willReturn(users);

            // when & then
            mockMvc.perform(get("/api/v1/users/search")
                            .param("query", "query값")
                            .param("nickname", "nickname값"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users").isArray())
                    .andExpect(jsonPath("$.users.length()").value(1));
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

        @Test
        @DisplayName("query와 nickname 모두 없을 때 400 에러 반환")
        void should_returnBadRequest_when_noParameters() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/users/search"))
                    .andExpect(status().isBadRequest());
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
            UpdateProfileRequest request = new UpdateProfileRequest(
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
            UpdateProfileRequest request = new UpdateProfileRequest(
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
