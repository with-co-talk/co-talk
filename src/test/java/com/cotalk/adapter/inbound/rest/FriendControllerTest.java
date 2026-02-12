package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.friend.HiddenFriendDto;
import com.cotalk.adapter.inbound.rest.dto.friend.SendFriendRequestRequest;
import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.friend.AcceptFriendRequestUseCase;
import com.cotalk.domain.port.inbound.friend.GetFriendListUseCase;
import com.cotalk.domain.port.inbound.friend.GetHiddenFriendsUseCase;
import com.cotalk.domain.port.inbound.friend.GetReceivedFriendRequestsUseCase;
import com.cotalk.domain.port.inbound.friend.GetSentFriendRequestsUseCase;
import com.cotalk.domain.port.inbound.friend.HideFriendUseCase;
import com.cotalk.domain.port.inbound.friend.RejectFriendRequestUseCase;
import com.cotalk.domain.port.inbound.friend.RemoveFriendUseCase;
import com.cotalk.domain.port.inbound.friend.SendFriendRequestUseCase;
import com.cotalk.domain.port.inbound.friend.UnhideFriendUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FriendController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RateLimitTestConfiguration.class, GlobalExceptionHandler.class})
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
    private GetReceivedFriendRequestsUseCase getReceivedFriendRequestsUseCase;

    @MockBean
    private GetSentFriendRequestsUseCase getSentFriendRequestsUseCase;

    @MockBean
    private HideFriendUseCase hideFriendUseCase;

    @MockBean
    private UnhideFriendUseCase unhideFriendUseCase;

    @MockBean
    private GetHiddenFriendsUseCase getHiddenFriendsUseCase;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("친구 요청 API")
    class SendFriendRequestApi {

        @Test
        @DisplayName("유효한 요청으로 친구 요청 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnCreated_when_validRequest() throws Exception {
            // given
            SendFriendRequestRequest request = new SendFriendRequestRequest(2L);

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
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long requestId = 100L;
            Long userId = 1L;

            given(acceptFriendRequestUseCase.acceptFriendRequest(anyLong(), anyLong())).willReturn(200L);

            // when & then
            mockMvc.perform(post("/api/v1/friends/requests/{requestId}/accept", requestId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("친구 요청을 수락했습니다."));
        }
    }

    @Nested
    @DisplayName("친구 목록 조회 API")
    class GetFriendListApi {

        @Test
        @DisplayName("친구 목록 조회 성공 - 페이지네이션 메타데이터 포함")
        @WithMockCustomUser(userId = 1L)
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

            Page<User> friendPage = new PageImpl<>(friends, PageRequest.of(0, 20), 2);
            given(getFriendListUseCase.getFriendList(eq(userId), any(Pageable.class))).willReturn(friendPage);

            // when & then
            mockMvc.perform(get("/api/v1/friends")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.friends").isArray())
                    .andExpect(jsonPath("$.friends.length()").value(2))
                    .andExpect(jsonPath("$.friends[0].user.nickname").value("친구1"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @DisplayName("페이지네이션 파라미터로 DB 레벨 페이지네이션 동작")
        @WithMockCustomUser(userId = 1L)
        void should_paginateAtDbLevel_when_pageAndSizeProvided() throws Exception {
            // given
            Long userId = 1L;
            User friend = User.builder()
                    .id(22L)
                    .email("friend22@example.com")
                    .nickname("친구22")
                    .passwordHash("hash")
                    .build();

            Page<User> friendPage = new PageImpl<>(List.of(friend), PageRequest.of(2, 5), 15);
            given(getFriendListUseCase.getFriendList(eq(userId), any(Pageable.class))).willReturn(friendPage);

            // when & then
            mockMvc.perform(get("/api/v1/friends")
                            .param("page", "2")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.friends.length()").value(1))
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.size").value(5))
                    .andExpect(jsonPath("$.totalElements").value(15))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }
    }

    @Nested
    @DisplayName("친구 요청 거절 API")
    class RejectFriendRequestApi {

        @Test
        @DisplayName("유효한 요청으로 친구 요청 거절 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long requestId = 100L;
            Long userId = 1L;

            willDoNothing().given(rejectFriendRequestUseCase).rejectFriendRequest(anyLong(), anyLong());

            // when & then
            mockMvc.perform(post("/api/v1/friends/requests/{requestId}/reject", requestId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("친구 요청을 거절했습니다."));
        }
    }

    @Nested
    @DisplayName("친구 삭제 API")
    class RemoveFriendApi {

        @Test
        @DisplayName("유효한 요청으로 친구 삭제 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_validRequest() throws Exception {
            // given
            Long userId = 1L;
            Long friendId = 2L;

            willDoNothing().given(removeFriendUseCase).removeFriend(anyLong(), anyLong());

            // when & then
            mockMvc.perform(delete("/api/v1/friends/{friendId}", friendId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("친구가 삭제되었습니다."));
        }
    }

    @Nested
    @DisplayName("받은 친구 요청 목록 조회 API")
    class GetReceivedFriendRequestsApi {

        @Test
        @DisplayName("받은 친구 요청 목록 조회 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnReceivedFriendRequests_when_validUserId() throws Exception {
            // given
            Long userId = 1L;

            User requester = User.builder()
                    .id(2L)
                    .email("requester@example.com")
                    .nickname("요청자")
                    .passwordHash("hash")
                    .build();

            User receiver = User.builder()
                    .id(userId)
                    .email("receiver@example.com")
                    .nickname("수신자")
                    .passwordHash("hash")
                    .build();

            FriendRequest request = FriendRequest.builder()
                    .id(100L)
                    .requesterId(2L)
                    .receiverId(userId)
                    .status(FriendRequest.RequestStatus.PENDING)
                    .build();
            
            // Reflection을 사용하여 createdAt 설정
            try {
                java.lang.reflect.Field createdAtField = com.cotalk.domain.entity.BaseEntity.class.getDeclaredField("createdAt");
                createdAtField.setAccessible(true);
                createdAtField.set(request, LocalDateTime.now());
            } catch (Exception e) {
                throw new RuntimeException("Failed to set createdAt", e);
            }

            given(getReceivedFriendRequestsUseCase.getReceivedFriendRequests(userId))
                    .willReturn(List.of(request));
            given(userRepository.findAllById(any())).willReturn(List.of(requester));
            given(userRepository.findById(userId)).willReturn(java.util.Optional.of(receiver));

            // when & then
            mockMvc.perform(get("/api/v1/friends/requests/received"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requests").isArray())
                    .andExpect(jsonPath("$.requests.length()").value(1))
                    .andExpect(jsonPath("$.requests[0].id").value(100L))
                    .andExpect(jsonPath("$.requests[0].requester.nickname").value("요청자"))
                    .andExpect(jsonPath("$.requests[0].receiver.nickname").value("수신자"))
                    .andExpect(jsonPath("$.requests[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("받은 친구 요청이 없을 때 빈 리스트 반환")
        @WithMockCustomUser(userId = 1L)
        void should_returnEmptyList_when_noReceivedRequests() throws Exception {
            // given
            Long userId = 1L;
            given(getReceivedFriendRequestsUseCase.getReceivedFriendRequests(userId))
                    .willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/friends/requests/received"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requests").isArray())
                    .andExpect(jsonPath("$.requests.length()").value(0));
        }
    }

    @Nested
    @DisplayName("보낸 친구 요청 목록 조회 API")
    class GetSentFriendRequestsApi {

        @Test
        @DisplayName("보낸 친구 요청 목록 조회 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnSentFriendRequests_when_validUserId() throws Exception {
            // given
            Long userId = 1L;

            User requester = User.builder()
                    .id(userId)
                    .email("requester@example.com")
                    .nickname("요청자")
                    .passwordHash("hash")
                    .build();

            User receiver = User.builder()
                    .id(2L)
                    .email("receiver@example.com")
                    .nickname("수신자")
                    .passwordHash("hash")
                    .build();

            FriendRequest request = FriendRequest.builder()
                    .id(100L)
                    .requesterId(userId)
                    .receiverId(2L)
                    .status(FriendRequest.RequestStatus.PENDING)
                    .build();

            // Reflection을 사용하여 createdAt 설정
            try {
                java.lang.reflect.Field createdAtField = com.cotalk.domain.entity.BaseEntity.class.getDeclaredField("createdAt");
                createdAtField.setAccessible(true);
                createdAtField.set(request, LocalDateTime.now());
            } catch (Exception e) {
                throw new RuntimeException("Failed to set createdAt", e);
            }

            given(getSentFriendRequestsUseCase.getSentFriendRequests(userId))
                    .willReturn(List.of(request));
            given(userRepository.findAllById(any())).willReturn(List.of(receiver));
            given(userRepository.findById(userId)).willReturn(java.util.Optional.of(requester));

            // when & then
            mockMvc.perform(get("/api/v1/friends/requests/sent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requests").isArray())
                    .andExpect(jsonPath("$.requests.length()").value(1))
                    .andExpect(jsonPath("$.requests[0].id").value(100L))
                    .andExpect(jsonPath("$.requests[0].requester.nickname").value("요청자"))
                    .andExpect(jsonPath("$.requests[0].receiver.nickname").value("수신자"))
                    .andExpect(jsonPath("$.requests[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("보낸 친구 요청이 없을 때 빈 리스트 반환")
        @WithMockCustomUser(userId = 1L)
        void should_returnEmptyList_when_noSentRequests() throws Exception {
            // given
            Long userId = 1L;
            given(getSentFriendRequestsUseCase.getSentFriendRequests(userId))
                    .willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/friends/requests/sent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requests").isArray())
                    .andExpect(jsonPath("$.requests.length()").value(0));
        }
    }

    @Nested
    @DisplayName("숨김 친구 API")
    class HiddenFriendApi {

        @Test
        @DisplayName("친구 숨기기 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_hideFriendSuccessfully() throws Exception {
            // given
            Long userId = 1L;
            Long friendId = 2L;

            willDoNothing().given(hideFriendUseCase).hideFriend(anyLong(), anyLong());

            // when & then
            mockMvc.perform(post("/api/v1/friends/{friendId}/hide", friendId))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("숨김 친구 해제 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_unhideFriendSuccessfully() throws Exception {
            // given
            Long userId = 1L;
            Long friendId = 2L;

            willDoNothing().given(unhideFriendUseCase).unhideFriend(anyLong(), anyLong());

            // when & then
            mockMvc.perform(delete("/api/v1/friends/{friendId}/hide", friendId))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("숨김 친구 목록 조회 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnHiddenFriendList_when_validUserId() throws Exception {
            // given
            Long userId = 1L;
            HiddenFriendDto dto = HiddenFriendDto.builder()
                    .id(1L)
                    .friendId(2L)
                    .nickname("숨긴친구")
                    .profileImageUrl("https://example.com/profile.jpg")
                    .hiddenAt(LocalDateTime.now())
                    .build();

            given(getHiddenFriendsUseCase.getHiddenFriends(userId))
                    .willReturn(List.of(dto));

            // when & then
            mockMvc.perform(get("/api/v1/friends/hidden"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.friends").isArray())
                    .andExpect(jsonPath("$.friends.length()").value(1))
                    .andExpect(jsonPath("$.friends[0].friendId").value(2))
                    .andExpect(jsonPath("$.friends[0].nickname").value("숨긴친구"));
        }

        @Test
        @DisplayName("숨김 친구가 없을 때 빈 리스트 반환")
        @WithMockCustomUser(userId = 1L)
        void should_returnEmptyList_when_noHiddenFriends() throws Exception {
            // given
            Long userId = 1L;
            given(getHiddenFriendsUseCase.getHiddenFriends(userId))
                    .willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/friends/hidden"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.friends").isArray())
                    .andExpect(jsonPath("$.friends.length()").value(0));
        }
    }
}
