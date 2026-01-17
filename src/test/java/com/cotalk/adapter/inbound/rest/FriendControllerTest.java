package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.friend.SendFriendRequestRequest;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.friend.AcceptFriendRequestUseCase;
import com.cotalk.domain.port.inbound.friend.GetFriendListUseCase;
import com.cotalk.domain.port.inbound.friend.RejectFriendRequestUseCase;
import com.cotalk.domain.port.inbound.friend.RemoveFriendUseCase;
import com.cotalk.domain.port.inbound.friend.SendFriendRequestUseCase;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FriendController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RateLimitTestConfiguration.class)
class FriendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SendFriendRequestUseCase sendFriendRequestUseCase;

    @MockBean
    private AcceptFriendRequestUseCase acceptFriendRequestUseCase;

    @MockBean
    private GetFriendListUseCase getFriendListUseCase;

    @MockBean
    private RejectFriendRequestUseCase rejectFriendRequestUseCase;

    @MockBean
    private RemoveFriendUseCase removeFriendUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("친구 요청 API")
    class SendFriendRequestApi {

        @Test
        @DisplayName("유효한 요청으로 친구 요청 성공")
        void should_returnCreated_when_validRequest() throws Exception {
            // given
            SendFriendRequestRequest request = new SendFriendRequestRequest(1L, 2L);

            given(sendFriendRequestUseCase.sendFriendRequest(anyLong(), anyLong())).willReturn(100L);

            // when & then
            mockMvc.perform(post("/api/v1/friends/requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.requestId").value(100L))
                    .andExpect(jsonPath("$.message").value("친구 요청이 전송되었습니다."));
        }
    }

    @Nested
    @DisplayName("친구 요청 수락 API")
    class AcceptFriendRequestApi {

        @Test
        @DisplayName("유효한 요청으로 친구 요청 수락 성공")
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long requestId = 100L;
            Long userId = 2L;

            given(acceptFriendRequestUseCase.acceptFriendRequest(anyLong(), anyLong())).willReturn(200L);

            // when & then
            mockMvc.perform(post("/api/v1/friends/requests/{requestId}/accept", requestId)
                            .param("userId", String.valueOf(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("친구 요청을 수락했습니다."));
        }
    }

    @Nested
    @DisplayName("친구 목록 조회 API")
    class GetFriendListApi {

        @Test
        @DisplayName("친구 목록 조회 성공")
        void should_returnFriendList_when_validUserId() throws Exception {
            // given
            Long userId = 1L;
            List<User> friends = List.of(
                    User.builder()
                            .id(2L)
                            .email("friend1@example.com")
                            .nickname("친구1")
                            .passwordHash("hash")
                            .build(),
                    User.builder()
                            .id(3L)
                            .email("friend2@example.com")
                            .nickname("친구2")
                            .passwordHash("hash")
                            .build()
            );

            given(getFriendListUseCase.getFriendList(userId)).willReturn(friends);

            // when & then
            mockMvc.perform(get("/api/v1/friends")
                            .param("userId", String.valueOf(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.friends").isArray())
                    .andExpect(jsonPath("$.friends.length()").value(2))
                    .andExpect(jsonPath("$.friends[0].nickname").value("친구1"));
        }
    }

    @Nested
    @DisplayName("친구 요청 거절 API")
    class RejectFriendRequestApi {

        @Test
        @DisplayName("유효한 요청으로 친구 요청 거절 성공")
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long requestId = 100L;
            Long userId = 2L;

            willDoNothing().given(rejectFriendRequestUseCase).rejectFriendRequest(anyLong(), anyLong());

            // when & then
            mockMvc.perform(post("/api/v1/friends/requests/{requestId}/reject", requestId)
                            .param("userId", String.valueOf(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("친구 요청을 거절했습니다."));
        }
    }

    @Nested
    @DisplayName("친구 삭제 API")
    class RemoveFriendApi {

        @Test
        @DisplayName("유효한 요청으로 친구 삭제 성공")
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long userId = 1L;
            Long friendId = 2L;

            willDoNothing().given(removeFriendUseCase).removeFriend(anyLong(), anyLong());

            // when & then
            mockMvc.perform(delete("/api/v1/friends/{friendId}", friendId)
                            .param("userId", String.valueOf(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("친구가 삭제되었습니다."));
        }
    }
}
