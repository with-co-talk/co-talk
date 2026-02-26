package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.user.UpdateOnlineStatusRequest;
import com.cotalk.adapter.inbound.rest.dto.user.UpdateProfileRequest;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.Email;
import com.cotalk.domain.entity.User.OnlineStatus;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.user.GetUserUseCase;
import com.cotalk.domain.port.inbound.user.SearchUserUseCase;
import com.cotalk.domain.port.inbound.user.UpdateProfileUseCase;
import com.cotalk.domain.port.inbound.user.UpdateUserOnlineStatusUseCase;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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
@Import({RateLimitTestConfiguration.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GetUserUseCase getUserUseCase;

    @MockitoBean
    private SearchUserUseCase searchUserUseCase;

    @MockitoBean
    private UpdateProfileUseCase updateProfileUseCase;

    @MockitoBean
    private UpdateUserOnlineStatusUseCase updateUserOnlineStatusUseCase;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
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
                            .email(new Email("user1@example.com"))
                            .nickname("테스트유저1")
                            .passwordHash("hash")
                            .avatarUrl("https://example.com/avatar1.png")
                            .build(),
                    User.builder()
                            .id(2L)
                            .email(new Email("user2@example.com"))
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
                            .email(new Email("user1@example.com"))
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
                            .email(new Email("user1@example.com"))
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

        @Test
        @DisplayName("query가 빈 문자열일 때 nickname 사용")
        void should_useNickname_when_queryIsBlank() throws Exception {
            // given
            List<User> users = List.of(
                    User.builder()
                            .id(1L)
                            .email(new Email("user1@example.com"))
                            .nickname("테스트유저1")
                            .passwordHash("hash")
                            .build()
            );

            given(searchUserUseCase.searchByNickname("nickname값")).willReturn(users);

            // when & then
            mockMvc.perform(get("/api/v1/users/search")
                            .param("query", "   ")
                            .param("nickname", "nickname값"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users.length()").value(1));
        }

        @Test
        @DisplayName("nickname이 빈 문자열일 때 400 에러 반환")
        void should_returnBadRequest_when_nicknameIsBlank() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/users/search")
                            .param("nickname", "   "))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("내 정보 조회 API")
    class GetCurrentUserApi {

        @Test
        @DisplayName("현재 로그인한 사용자 정보 조회 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnCurrentUser_when_validPrincipal() throws Exception {
            // given
            Long userId = 1L;
            User user = User.builder()
                    .id(userId)
                    .email(new Email("test@example.com"))
                    .nickname("테스트유저")
                    .passwordHash("hash")
                    .avatarUrl("https://example.com/avatar.png")
                    .onlineStatus(OnlineStatus.ONLINE)
                    .lastActiveAt(LocalDateTime.now())
                    .build();

            given(getUserUseCase.getUserById(userId)).willReturn(user);

            // when & then
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.nickname").value("테스트유저"))
                    .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.png"))
                    .andExpect(jsonPath("$.onlineStatus").value("ONLINE"));
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 404 에러")
        @WithMockCustomUser(userId = 999L)
        void should_returnNotFound_when_userNotFound() throws Exception {
            // given
            Long userId = 999L;
            given(getUserUseCase.getUserById(userId)).willThrow(new UserNotFoundException(userId));

            // when & then
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("프로필 수정 API")
    class UpdateProfileApi {

        @Test
        @DisplayName("유효한 요청으로 프로필 수정 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long userId = 1L;
            UpdateProfileRequest request = new UpdateProfileRequest(
                    "새닉네임", "새상태메시지", "https://example.com/avatar.png");

            willDoNothing().given(updateProfileUseCase).updateProfile(anyLong(), anyString(), anyString(), anyString());

            // when & then
            mockMvc.perform(put("/api/v1/users/{userId}/profile", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("프로필이 수정되었습니다."));
        }

        @Test
        @DisplayName("닉네임만 수정")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_updateNicknameOnly() throws Exception {
            // given
            Long userId = 1L;
            UpdateProfileRequest request = new UpdateProfileRequest(
                    "새닉네임", null, null);

            willDoNothing().given(updateProfileUseCase).updateProfile(anyLong(), anyString(), isNull(), isNull());

            // when & then
            mockMvc.perform(put("/api/v1/users/{userId}/profile", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("프로필이 수정되었습니다."));
        }

        @Test
        @DisplayName("다른 사용자의 프로필 수정 시 403 에러")
        @WithMockCustomUser(userId = 1L)
        void should_returnForbidden_when_updateOtherUserProfile() throws Exception {
            // given
            Long targetUserId = 2L;
            UpdateProfileRequest request = new UpdateProfileRequest(
                    "새닉네임", "새상태메시지", "https://example.com/avatar.png");

            // when & then
            mockMvc.perform(put("/api/v1/users/{userId}/profile", targetUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("온라인 상태 업데이트 API")
    class UpdateOnlineStatusApi {

        @Test
        @DisplayName("유효한 요청으로 온라인 상태 업데이트 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long userId = 1L;
            UpdateOnlineStatusRequest request = new UpdateOnlineStatusRequest(OnlineStatus.ONLINE);

            willDoNothing().given(updateUserOnlineStatusUseCase).updateOnlineStatus(anyLong(), any(OnlineStatus.class));

            // when & then
            mockMvc.perform(put("/api/v1/users/{userId}/online-status", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("온라인 상태가 업데이트되었습니다."));
        }

        @Test
        @DisplayName("다른 사용자의 온라인 상태 업데이트 시 403 에러")
        @WithMockCustomUser(userId = 1L)
        void should_returnForbidden_when_updateOtherUserOnlineStatus() throws Exception {
            // given
            Long targetUserId = 2L;
            UpdateOnlineStatusRequest request = new UpdateOnlineStatusRequest(OnlineStatus.ONLINE);

            // when & then
            mockMvc.perform(put("/api/v1/users/{userId}/online-status", targetUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("마지막 접속 시간 업데이트 API")
    class UpdateLastActiveApi {

        @Test
        @DisplayName("유효한 요청으로 마지막 접속 시간 업데이트 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long userId = 1L;

            willDoNothing().given(updateUserOnlineStatusUseCase).updateLastActiveAt(anyLong());

            // when & then
            mockMvc.perform(put("/api/v1/users/{userId}/last-active", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("마지막 접속 시간이 업데이트되었습니다."));
        }

        @Test
        @DisplayName("다른 사용자의 마지막 접속 시간 업데이트 시 403 에러")
        @WithMockCustomUser(userId = 1L)
        void should_returnForbidden_when_updateOtherUserLastActive() throws Exception {
            // given
            Long targetUserId = 2L;

            // when & then
            mockMvc.perform(put("/api/v1/users/{userId}/last-active", targetUserId))
                    .andExpect(status().isForbidden());
        }
    }
}
