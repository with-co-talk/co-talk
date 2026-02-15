package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.model.HiddenFriendInfo;
import com.cotalk.domain.port.inbound.friend.GetHiddenFriendsUseCase;
import com.cotalk.domain.port.inbound.friend.HideFriendUseCase;
import com.cotalk.domain.port.inbound.friend.UnhideFriendUseCase;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import com.cotalk.infrastructure.security.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 숨긴 친구 컨트롤러 단위 테스트.
 * <p>
 * 친구 숨김, 숨김 해제, 숨긴 친구 목록 조회 엔드포인트를 테스트한다.
 *
 * @author seunggu.lee
 */
@WebMvcTest(HiddenFriendController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RateLimitTestConfiguration.class)
class HiddenFriendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HideFriendUseCase hideFriendUseCase;

    @MockBean
    private UnhideFriendUseCase unhideFriendUseCase;

    @MockBean
    private GetHiddenFriendsUseCase getHiddenFriendsUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("친구 숨김 API")
    class HideFriendApi {

        @Test
        @DisplayName("친구 숨김 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_hideFriendSuccess() throws Exception {
            // given
            Long userId = 1L;
            Long friendId = 2L;
            willDoNothing().given(hideFriendUseCase).hideFriend(userId, friendId);

            // when & then
            mockMvc.perform(post("/api/v1/friends/{friendId}/hide", friendId))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("친구 숨김 해제 API")
    class UnhideFriendApi {

        @Test
        @DisplayName("친구 숨김 해제 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_unhideFriendSuccess() throws Exception {
            // given
            Long userId = 1L;
            Long friendId = 2L;
            willDoNothing().given(unhideFriendUseCase).unhideFriend(userId, friendId);

            // when & then
            mockMvc.perform(delete("/api/v1/friends/{friendId}/hide", friendId))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("숨긴 친구 목록 조회 API")
    class GetHiddenFriendsApi {

        @Test
        @DisplayName("숨긴 친구 목록 조회 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_getHiddenFriendsSuccess() throws Exception {
            // given
            Long userId = 1L;
            LocalDateTime now = LocalDateTime.now();
            List<HiddenFriendInfo> hiddenFriends = List.of(
                    new HiddenFriendInfo(1L, 2L, "친구1", "https://example.com/profile1.png", now),
                    new HiddenFriendInfo(2L, 3L, "친구2", "https://example.com/profile2.png", now)
            );

            given(getHiddenFriendsUseCase.getHiddenFriends(anyLong())).willReturn(hiddenFriends);

            // when & then
            mockMvc.perform(get("/api/v1/friends/hidden")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.friends").isArray())
                    .andExpect(jsonPath("$.friends.length()").value(2))
                    .andExpect(jsonPath("$.friends[0].friendId").value(2L))
                    .andExpect(jsonPath("$.friends[0].nickname").value("친구1"))
                    .andExpect(jsonPath("$.friends[1].friendId").value(3L))
                    .andExpect(jsonPath("$.friends[1].nickname").value("친구2"));
        }

        @Test
        @DisplayName("숨긴 친구가 없을 때 빈 배열 반환")
        @WithMockCustomUser(userId = 1L)
        void should_returnEmptyArray_when_noHiddenFriends() throws Exception {
            // given
            Long userId = 1L;
            given(getHiddenFriendsUseCase.getHiddenFriends(anyLong())).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/friends/hidden")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.friends").isArray())
                    .andExpect(jsonPath("$.friends.length()").value(0));
        }

        @Test
        @DisplayName("페이징 처리 - 두 번째 페이지 조회")
        @WithMockCustomUser(userId = 1L)
        void should_returnPaginatedResult_when_requestSecondPage() throws Exception {
            // given
            Long userId = 1L;
            LocalDateTime now = LocalDateTime.now();
            // 첫 번째 페이지 20개 + 두 번째 페이지 3개 시뮬레이션
            List<HiddenFriendInfo> allHiddenFriends = List.of(
                    new HiddenFriendInfo(21L, 41L, "친구21", null, now),
                    new HiddenFriendInfo(22L, 42L, "친구22", null, now),
                    new HiddenFriendInfo(23L, 43L, "친구23", null, now)
            );

            given(getHiddenFriendsUseCase.getHiddenFriends(anyLong())).willReturn(allHiddenFriends);

            // when & then
            mockMvc.perform(get("/api/v1/friends/hidden")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.friends").isArray())
                    .andExpect(jsonPath("$.friends.length()").value(3));
        }
    }
}
