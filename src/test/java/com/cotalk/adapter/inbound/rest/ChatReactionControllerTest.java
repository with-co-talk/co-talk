package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.port.inbound.AddMessageReactionUseCase;
import com.cotalk.domain.port.inbound.GetMessageReactionsUseCase;
import com.cotalk.domain.port.inbound.RemoveMessageReactionUseCase;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatReactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RateLimitTestConfiguration.class)
class ChatReactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AddMessageReactionUseCase addMessageReactionUseCase;

    @MockBean
    private RemoveMessageReactionUseCase removeMessageReactionUseCase;

    @MockBean
    private GetMessageReactionsUseCase getMessageReactionsUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("메시지 반응 추가 API")
    class AddReactionApi {

        @Test
        @DisplayName("유효한 요청으로 반응 추가 성공")
        void should_returnCreated_when_validReaction() throws Exception {
            // given
            Long messageId = 500L;
            ChatReactionController.AddReactionRequest request = new ChatReactionController.AddReactionRequest(
                    1L, "👍");

            MessageReaction reaction = MessageReaction.builder()
                    .id(1000L)
                    .messageId(messageId)
                    .userId(1L)
                    .emoji("👍")
                    .createdAt(LocalDateTime.now())
                    .build();

            given(addMessageReactionUseCase.addReaction(anyLong(), anyLong(), anyString()))
                    .willReturn(reaction);

            // when & then
            mockMvc.perform(post("/api/v1/chat/messages/{messageId}/reactions", messageId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.reactionId").value(1000L))
                    .andExpect(jsonPath("$.messageId").value(messageId))
                    .andExpect(jsonPath("$.userId").value(1L))
                    .andExpect(jsonPath("$.emoji").value("👍"));
        }
    }

    @Nested
    @DisplayName("메시지 반응 제거 API")
    class RemoveReactionApi {

        @Test
        @DisplayName("유효한 요청으로 반응 제거 성공")
        void should_returnOk_when_validRemoval() throws Exception {
            // given
            Long messageId = 500L;
            ChatReactionController.RemoveReactionRequest request = new ChatReactionController.RemoveReactionRequest(
                    1L, "👍");

            willDoNothing().given(removeMessageReactionUseCase)
                    .removeReaction(anyLong(), anyLong(), anyString());

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
        void should_returnReactions_when_validMessageId() throws Exception {
            // given
            Long messageId = 500L;
            List<MessageReaction> reactions = List.of(
                    MessageReaction.builder()
                            .id(1000L)
                            .messageId(messageId)
                            .userId(1L)
                            .emoji("👍")
                            .createdAt(LocalDateTime.now())
                            .build(),
                    MessageReaction.builder()
                            .id(1001L)
                            .messageId(messageId)
                            .userId(2L)
                            .emoji("❤️")
                            .createdAt(LocalDateTime.now())
                            .build()
            );

            given(getMessageReactionsUseCase.getReactions(messageId)).willReturn(reactions);

            // when & then
            mockMvc.perform(get("/api/v1/chat/messages/{messageId}/reactions", messageId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reactions").isArray())
                    .andExpect(jsonPath("$.reactions.length()").value(2))
                    .andExpect(jsonPath("$.reactions[0].reactionId").value(1000L))
                    .andExpect(jsonPath("$.reactions[0].emoji").value("👍"))
                    .andExpect(jsonPath("$.reactions[1].reactionId").value(1001L))
                    .andExpect(jsonPath("$.reactions[1].emoji").value("❤️"));
        }

        @Test
        @DisplayName("반응이 없을 때 빈 배열 반환")
        void should_returnEmptyArray_when_noReactions() throws Exception {
            // given
            Long messageId = 500L;
            given(getMessageReactionsUseCase.getReactions(messageId)).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/chat/messages/{messageId}/reactions", messageId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reactions").isArray())
                    .andExpect(jsonPath("$.reactions.length()").value(0));
        }
    }
}
