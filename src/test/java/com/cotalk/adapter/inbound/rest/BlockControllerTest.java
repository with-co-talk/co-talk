package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.BlockNotFoundException;
import com.cotalk.domain.exception.InvalidBlockException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.friend.BlockUserUseCase;
import com.cotalk.domain.port.inbound.friend.GetBlockedUsersUseCase;
import com.cotalk.domain.port.inbound.friend.UnblockUserUseCase;
import com.cotalk.infrastructure.exception.GlobalExceptionHandler;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
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

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BlockController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RateLimitTestConfiguration.class, GlobalExceptionHandler.class})
class BlockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlockUserUseCase blockUserUseCase;

    @MockBean
    private UnblockUserUseCase unblockUserUseCase;

    @MockBean
    private GetBlockedUsersUseCase getBlockedUsersUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("사용자 차단 API")
    class BlockUserTests {

        @Test
        @DisplayName("사용자 차단 성공")
        void should_returnCreated_when_blockUserSuccess() throws Exception {
            // given
            Long blockerId = 1L;
            Long blockedId = 2L;

            willDoNothing().given(blockUserUseCase).blockUser(eq(blockerId), eq(blockedId));

            String requestBody = """
                    {
                        "blockerId": 1,
                        "blockedId": 2
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/blocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("사용자를 차단했습니다."));
        }

        @Test
        @DisplayName("자기 자신 차단 시 400 에러")
        void should_returnBadRequest_when_blockSelf() throws Exception {
            // given
            Long userId = 1L;

            willThrow(new InvalidBlockException("자기 자신을 차단할 수 없습니다."))
                    .given(blockUserUseCase).blockUser(eq(userId), eq(userId));

            String requestBody = """
                    {
                        "blockerId": 1,
                        "blockedId": 1
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/blocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("이미 차단된 사용자 재차단 시 400 에러")
        void should_returnBadRequest_when_alreadyBlocked() throws Exception {
            // given
            Long blockerId = 1L;
            Long blockedId = 2L;

            willThrow(new InvalidBlockException("이미 차단된 사용자입니다."))
                    .given(blockUserUseCase).blockUser(eq(blockerId), eq(blockedId));

            String requestBody = """
                    {
                        "blockerId": 1,
                        "blockedId": 2
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/blocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 사용자 차단 시 404 에러")
        void should_returnNotFound_when_userNotFound() throws Exception {
            // given
            Long blockerId = 1L;
            Long blockedId = 999L;

            willThrow(new UserNotFoundException("사용자를 찾을 수 없습니다."))
                    .given(blockUserUseCase).blockUser(eq(blockerId), eq(blockedId));

            String requestBody = """
                    {
                        "blockerId": 1,
                        "blockedId": 999
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/blocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("blockerId 누락 시 400 에러")
        void should_returnBadRequest_when_blockerIdMissing() throws Exception {
            // given
            String requestBody = """
                    {
                        "blockedId": 2
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/blocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("blockedId 누락 시 400 에러")
        void should_returnBadRequest_when_blockedIdMissing() throws Exception {
            // given
            String requestBody = """
                    {
                        "blockerId": 1
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/blocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("빈 요청 body 시 400 에러")
        void should_returnBadRequest_when_emptyBody() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/blocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("차단 해제 API")
    class UnblockUserTests {

        @Test
        @DisplayName("차단 해제 성공")
        void should_returnOk_when_unblockSuccess() throws Exception {
            // given
            Long blockerId = 1L;
            Long blockedId = 2L;

            willDoNothing().given(unblockUserUseCase).unblockUser(eq(blockerId), eq(blockedId));

            // when & then
            mockMvc.perform(delete("/api/v1/blocks")
                            .param("blockerId", blockerId.toString())
                            .param("blockedId", blockedId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("차단을 해제했습니다."));
        }

        @Test
        @DisplayName("차단되지 않은 사용자 해제 시 404 에러 (BlockNotFoundException은 ResourceNotFoundException)")
        void should_returnNotFound_when_blockNotFound() throws Exception {
            // given
            Long blockerId = 1L;
            Long blockedId = 2L;

            willThrow(new BlockNotFoundException("차단 기록을 찾을 수 없습니다."))
                    .given(unblockUserUseCase).unblockUser(eq(blockerId), eq(blockedId));

            // when & then
            mockMvc.perform(delete("/api/v1/blocks")
                            .param("blockerId", blockerId.toString())
                            .param("blockedId", blockedId.toString()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("blockerId 파라미터 누락 시 400 에러")
        void should_returnBadRequest_when_blockerIdParamMissing() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/blocks")
                            .param("blockedId", "2"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("blockedId 파라미터 누락 시 400 에러")
        void should_returnBadRequest_when_blockedIdParamMissing() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/blocks")
                            .param("blockerId", "1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("잘못된 타입의 blockerId 시 500 에러 (타입 변환 실패)")
        void should_returnInternalError_when_invalidBlockerIdType() throws Exception {
            // when & then - Spring이 Long 변환 실패 시 내부 오류 발생
            mockMvc.perform(delete("/api/v1/blocks")
                            .param("blockerId", "invalid")
                            .param("blockedId", "2"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("차단 목록 조회 API")
    class GetBlockedUsersTests {

        @Test
        @DisplayName("차단 목록 조회 성공")
        void should_returnBlockedUsers_when_getBlockedUsersSuccess() throws Exception {
            // given
            Long userId = 1L;

            User blockedUser1 = User.builder()
                    .id(2L)
                    .nickname("user2")
                    .avatarUrl("https://example.com/avatar2.png")
                    .build();

            User blockedUser2 = User.builder()
                    .id(3L)
                    .nickname("user3")
                    .avatarUrl("https://example.com/avatar3.png")
                    .build();

            given(getBlockedUsersUseCase.getBlockedUsers(eq(userId)))
                    .willReturn(List.of(blockedUser1, blockedUser2));

            // when & then
            mockMvc.perform(get("/api/v1/blocks")
                            .param("userId", userId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.blockedUsers").isArray())
                    .andExpect(jsonPath("$.blockedUsers.length()").value(2))
                    .andExpect(jsonPath("$.blockedUsers[0].id").value(2))
                    .andExpect(jsonPath("$.blockedUsers[0].nickname").value("user2"))
                    .andExpect(jsonPath("$.blockedUsers[0].avatarUrl").value("https://example.com/avatar2.png"))
                    .andExpect(jsonPath("$.blockedUsers[1].id").value(3))
                    .andExpect(jsonPath("$.blockedUsers[1].nickname").value("user3"));
        }

        @Test
        @DisplayName("차단 목록 빈 경우 빈 배열 반환")
        void should_returnEmptyList_when_noBlockedUsers() throws Exception {
            // given
            Long userId = 1L;

            given(getBlockedUsersUseCase.getBlockedUsers(eq(userId)))
                    .willReturn(Collections.emptyList());

            // when & then
            mockMvc.perform(get("/api/v1/blocks")
                            .param("userId", userId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.blockedUsers").isArray())
                    .andExpect(jsonPath("$.blockedUsers.length()").value(0));
        }

        @Test
        @DisplayName("userId 파라미터 누락 시 400 에러")
        void should_returnBadRequest_when_userIdMissing() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/blocks"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 404 에러")
        void should_returnNotFound_when_userNotFoundForBlockedList() throws Exception {
            // given
            Long userId = 999L;

            given(getBlockedUsersUseCase.getBlockedUsers(eq(userId)))
                    .willThrow(new UserNotFoundException("사용자를 찾을 수 없습니다."));

            // when & then
            mockMvc.perform(get("/api/v1/blocks")
                            .param("userId", userId.toString()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("잘못된 타입의 userId 시 500 에러 (타입 변환 실패)")
        void should_returnInternalError_when_invalidUserIdType() throws Exception {
            // when & then - Spring이 Long 변환 실패 시 내부 오류 발생
            mockMvc.perform(get("/api/v1/blocks")
                            .param("userId", "not-a-number"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("avatarUrl이 null인 사용자도 정상 조회")
        void should_returnBlockedUsers_when_avatarUrlIsNull() throws Exception {
            // given
            Long userId = 1L;

            User blockedUser = User.builder()
                    .id(2L)
                    .nickname("user2")
                    .avatarUrl(null)
                    .build();

            given(getBlockedUsersUseCase.getBlockedUsers(eq(userId)))
                    .willReturn(List.of(blockedUser));

            // when & then
            mockMvc.perform(get("/api/v1/blocks")
                            .param("userId", userId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.blockedUsers[0].id").value(2))
                    .andExpect(jsonPath("$.blockedUsers[0].nickname").value("user2"))
                    .andExpect(jsonPath("$.blockedUsers[0].avatarUrl").isEmpty());
        }
    }
}
