package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.message.AddReactionRequest;
import com.cotalk.adapter.inbound.rest.dto.message.GroupedReactionResponse;
import com.cotalk.adapter.inbound.rest.dto.message.RemoveReactionRequest;
import com.cotalk.application.service.message.GetMessageReactionsService;
import com.cotalk.domain.entity.Emoji;
import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.port.inbound.message.AddMessageReactionUseCase;
import com.cotalk.domain.port.inbound.message.RemoveMessageReactionUseCase;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatReactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RateLimitTestConfiguration.class)
class ChatReactionControllerTest {

    private static final Long TEST_USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AddMessageReactionUseCase addMessageReactionUseCase;

    @MockBean
    private RemoveMessageReactionUseCase removeMessageReactionUseCase;

    @MockBean
    private GetMessageReactionsService getMessageReactionsService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("메시지 반응 추가 API")
    class AddReactionApi {

        @Test
        @DisplayName("유효한 요청으로 반응 추가 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnCreated_when_validReaction() throws Exception {
            // given
            Long messageId = 500L;
            AddReactionRequest request = new AddReactionRequest("thumbsup");

            MessageReaction reaction = MessageReaction.builder()
                    .id(1000L)
                    .messageId(messageId)
                    .userId(TEST_USER_ID)
                    .emoji(Emoji.THUMBS_UP)
                    .build();

            given(addMessageReactionUseCase.addReaction(eq(messageId), eq(TEST_USER_ID), anyString()))
                    .willReturn(reaction);

            // when & then
            mockMvc.perform(post("/api/v1/chat/messages/{messageId}/reactions", messageId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.reactionId").value(1000L))
                    .andExpect(jsonPath("$.messageId").value(messageId))
                    .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                    .andExpect(jsonPath("$.emoji").value("THUMBS_UP"))
                    .andExpect(jsonPath("$.emojiCharacter").value("👍"));
        }
    }

    @Nested
    @DisplayName("메시지 반응 제거 API")
    class RemoveReactionApi {

        @Test
        @DisplayName("유효한 요청으로 반응 제거 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_validRemoval() throws Exception {
            // given
            Long messageId = 500L;
            RemoveReactionRequest request = new RemoveReactionRequest("thumbsup");

            willDoNothing().given(removeMessageReactionUseCase)
                    .removeReaction(eq(messageId), eq(TEST_USER_ID), anyString());

            // when & then
            mockMvc.perform(delete("/api/v1/chat/messages/{messageId}/reactions", messageId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("반응이 제거되었습니다."));
        }
    }

    @Nested
    @DisplayName("메시지 반응 조회 API")
    class GetReactionsApi {

        @Test
        @DisplayName("메시지의 모든 반응 조회 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnReactions_when_validMessageId() throws Exception {
            // given
            Long messageId = 500L;
            List<GroupedReactionResponse> groupedReactions = List.of(
                    GroupedReactionResponse.from(Emoji.THUMBS_UP, List.of(1L, 2L), TEST_USER_ID),
                    GroupedReactionResponse.from(Emoji.HEART, List.of(3L), TEST_USER_ID)
            );

            given(getMessageReactionsService.getGroupedReactions(eq(messageId), eq(TEST_USER_ID)))
                    .willReturn(groupedReactions);

            // when & then
            mockMvc.perform(get("/api/v1/chat/messages/{messageId}/reactions", messageId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].emoji").value("THUMBS_UP"))
                    .andExpect(jsonPath("$[0].emojiCharacter").value("👍"))
                    .andExpect(jsonPath("$[0].emojiName").value("thumbsup"))
                    .andExpect(jsonPath("$[0].count").value(2))
                    .andExpect(jsonPath("$[0].userIds").isArray())
                    .andExpect(jsonPath("$[0].userIds.length()").value(2))
                    .andExpect(jsonPath("$[0].userIds[0]").value(1L))
                    .andExpect(jsonPath("$[0].userIds[1]").value(2L))
                    .andExpect(jsonPath("$[0].currentUserReacted").value(true))
                    .andExpect(jsonPath("$[1].emoji").value("HEART"))
                    .andExpect(jsonPath("$[1].emojiCharacter").value("❤️"))
                    .andExpect(jsonPath("$[1].count").value(1));
        }

        @Test
        @DisplayName("반응이 없을 때 빈 배열 반환")
        @WithMockCustomUser(userId = 1L)
        void should_returnEmptyArray_when_noReactions() throws Exception {
            // given
            Long messageId = 500L;
            given(getMessageReactionsService.getGroupedReactions(eq(messageId), eq(TEST_USER_ID)))
                    .willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/chat/messages/{messageId}/reactions", messageId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }
}
