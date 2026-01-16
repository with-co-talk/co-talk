package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.inbound.DeleteMessageUseCase;
import com.cotalk.domain.port.inbound.GetMessageHistoryUseCase;
import com.cotalk.domain.port.inbound.SendMessageUseCase;
import com.cotalk.domain.port.inbound.UpdateMessageUseCase;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatMessageController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RateLimitTestConfiguration.class)
class ChatMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SendMessageUseCase sendMessageUseCase;

    @MockBean
    private GetMessageHistoryUseCase getMessageHistoryUseCase;

    @MockBean
    private UpdateMessageUseCase updateMessageUseCase;

    @MockBean
    private DeleteMessageUseCase deleteMessageUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("메시지 전송 API")
    class SendMessageApi {

        @Test
        @DisplayName("유효한 요청으로 메시지 전송 성공")
        void should_returnCreated_when_validMessage() throws Exception {
            // given
            ChatMessageController.SendMessageRequest request = new ChatMessageController.SendMessageRequest(
                    1L, 100L, "안녕하세요!");

            Message message = Message.builder()
                    .id(500L)
                    .senderId(1L)
                    .chatRoomId(100L)
                    .content("안녕하세요!")
                    .createdAt(LocalDateTime.now())
                    .build();

            given(sendMessageUseCase.sendMessage(anyLong(), anyLong(), anyString())).willReturn(message);

            // when & then
            mockMvc.perform(post("/api/v1/chat/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.messageId").value(500L))
                    .andExpect(jsonPath("$.content").value("안녕하세요!"));
        }
    }

    @Nested
    @DisplayName("메시지 히스토리 조회 API")
    class GetMessageHistoryApi {

        @Test
        @DisplayName("커서 기반 메시지 조회 - 최신 메시지부터 (beforeMessageId 없음)")
        void should_returnLatestMessages_when_noBeforeMessageId() throws Exception {
            // given
            Long roomId = 100L;
            Long userId = 1L;
            List<Message> messages = List.of(
                    Message.builder()
                            .id(1000L)
                            .senderId(1L)
                            .chatRoomId(roomId)
                            .content("최신 메시지")
                            .createdAt(LocalDateTime.now())
                            .build(),
                    Message.builder()
                            .id(999L)
                            .senderId(2L)
                            .chatRoomId(roomId)
                            .content("이전 메시지")
                            .createdAt(LocalDateTime.now().minusMinutes(1))
                            .build()
            );

            given(getMessageHistoryUseCase.getMessageHistory(eq(roomId), eq(userId), isNull(), eq(20)))
                    .willReturn(messages);

            // when & then
            mockMvc.perform(get("/api/v1/chat/messages/rooms/{roomId}", roomId)
                            .param("userId", String.valueOf(userId))
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages").isArray())
                    .andExpect(jsonPath("$.messages.length()").value(2))
                    .andExpect(jsonPath("$.messages[0].id").value(1000))
                    .andExpect(jsonPath("$.messages[0].content").value("최신 메시지"))
                    .andExpect(jsonPath("$.nextCursor").value(999))
                    .andExpect(jsonPath("$.hasMore").value(false));
        }

        @Test
        @DisplayName("커서 기반 메시지 조회 - 특정 메시지 이전부터 (위로 스크롤)")
        void should_returnOlderMessages_when_beforeMessageIdProvided() throws Exception {
            // given
            Long roomId = 100L;
            Long userId = 1L;
            Long beforeMessageId = 1000L;
            int size = 20;

            List<Message> messages = List.of(
                    Message.builder()
                            .id(999L)
                            .senderId(2L)
                            .chatRoomId(roomId)
                            .content("이전 메시지 1")
                            .createdAt(LocalDateTime.now().minusMinutes(1))
                            .build(),
                    Message.builder()
                            .id(998L)
                            .senderId(1L)
                            .chatRoomId(roomId)
                            .content("이전 메시지 2")
                            .createdAt(LocalDateTime.now().minusMinutes(2))
                            .build()
            );

            given(getMessageHistoryUseCase.getMessageHistory(roomId, userId, beforeMessageId, size))
                    .willReturn(messages);

            // when & then
            mockMvc.perform(get("/api/v1/chat/messages/rooms/{roomId}", roomId)
                            .param("userId", String.valueOf(userId))
                            .param("beforeMessageId", String.valueOf(beforeMessageId))
                            .param("size", String.valueOf(size)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages").isArray())
                    .andExpect(jsonPath("$.messages.length()").value(2))
                    .andExpect(jsonPath("$.messages[0].id").value(999))
                    .andExpect(jsonPath("$.messages[1].id").value(998))
                    .andExpect(jsonPath("$.nextCursor").value(998))
                    .andExpect(jsonPath("$.hasMore").value(false));
        }

        @Test
        @DisplayName("커서 기반 메시지 조회 - 더 많은 메시지가 있을 때 hasMore가 true")
        void should_returnHasMoreTrue_when_morePagesExist() throws Exception {
            // given
            Long roomId = 100L;
            Long userId = 1L;
            int size = 2;

            // size와 동일한 개수의 메시지가 반환되면 hasMore = true
            List<Message> messages = List.of(
                    Message.builder()
                            .id(1000L)
                            .senderId(1L)
                            .chatRoomId(roomId)
                            .content("메시지 1")
                            .createdAt(LocalDateTime.now())
                            .build(),
                    Message.builder()
                            .id(999L)
                            .senderId(2L)
                            .chatRoomId(roomId)
                            .content("메시지 2")
                            .createdAt(LocalDateTime.now().minusMinutes(1))
                            .build()
            );

            given(getMessageHistoryUseCase.getMessageHistory(eq(roomId), eq(userId), isNull(), eq(size)))
                    .willReturn(messages);

            // when & then
            mockMvc.perform(get("/api/v1/chat/messages/rooms/{roomId}", roomId)
                            .param("userId", String.valueOf(userId))
                            .param("size", String.valueOf(size)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages.length()").value(2))
                    .andExpect(jsonPath("$.nextCursor").value(999))
                    .andExpect(jsonPath("$.hasMore").value(true));
        }
    }

    @Nested
    @DisplayName("메시지 수정 API")
    class UpdateMessageApi {

        @Test
        @DisplayName("유효한 요청으로 메시지 수정 성공")
        void should_returnOk_when_validUpdate() throws Exception {
            // given
            Long messageId = 500L;
            ChatMessageController.UpdateMessageRequest request = new ChatMessageController.UpdateMessageRequest(
                    1L, "수정된 메시지");

            Message updatedMessage = Message.builder()
                    .id(messageId)
                    .senderId(1L)
                    .chatRoomId(100L)
                    .content("수정된 메시지")
                    .createdAt(LocalDateTime.now().minusMinutes(5))
                    .updatedAt(LocalDateTime.now())
                    .build();

            given(updateMessageUseCase.updateMessage(eq(messageId), eq(1L), eq("수정된 메시지")))
                    .willReturn(updatedMessage);

            // when & then
            mockMvc.perform(put("/api/v1/chat/messages/{messageId}", messageId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messageId").value(messageId))
                    .andExpect(jsonPath("$.content").value("수정된 메시지"));
        }
    }

    @Nested
    @DisplayName("메시지 삭제 API")
    class DeleteMessageApi {

        @Test
        @DisplayName("유효한 요청으로 메시지 삭제 성공")
        void should_returnOk_when_validDelete() throws Exception {
            // given
            Long messageId = 500L;
            Long userId = 1L;

            willDoNothing().given(deleteMessageUseCase).deleteMessage(messageId, userId);

            // when & then
            mockMvc.perform(delete("/api/v1/chat/messages/{messageId}", messageId)
                            .param("userId", String.valueOf(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("메시지가 삭제되었습니다."));
        }
    }
}
