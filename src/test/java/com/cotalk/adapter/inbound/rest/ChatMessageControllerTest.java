package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.message.ForwardMessageRequest;
import com.cotalk.adapter.inbound.rest.dto.message.ReplyMessageRequest;
import com.cotalk.adapter.inbound.rest.dto.message.SendFileMessageRequest;
import com.cotalk.adapter.inbound.rest.dto.message.SendMessageRequest;
import com.cotalk.adapter.inbound.rest.dto.message.UpdateMessageRequest;
import com.cotalk.domain.entity.Message;
import com.cotalk.infrastructure.security.WithMockCustomUser;
import com.cotalk.domain.port.inbound.message.DeleteMessageUseCase;
import com.cotalk.domain.port.inbound.message.GetMediaGalleryUseCase;
import com.cotalk.domain.port.inbound.message.GetMessageHistoryUseCase;
import com.cotalk.domain.port.inbound.message.MessageReplyForwardUseCase;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase;
import com.cotalk.domain.port.inbound.message.UpdateMessageUseCase;
import com.cotalk.domain.port.outbound.FileStorage;
import com.cotalk.infrastructure.ratelimit.RateLimitTestConfiguration;
import com.cotalk.infrastructure.security.JwtAuthenticationFilter;
import com.cotalk.infrastructure.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @MockitoBean
    private SendMessageUseCase sendMessageUseCase;

    @MockitoBean
    private GetMessageHistoryUseCase getMessageHistoryUseCase;

    @MockitoBean
    private UpdateMessageUseCase updateMessageUseCase;

    @MockitoBean
    private DeleteMessageUseCase deleteMessageUseCase;

    @MockitoBean
    private MessageReplyForwardUseCase messageReplyForwardUseCase;

    @MockitoBean
    private GetMediaGalleryUseCase getMediaGalleryUseCase;

    @MockitoBean
    private FileStorage fileStorage;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUpPresignStub() {
        // 기본적으로 첨부파일 URL을 그대로 통과시켜 기존 검증을 유지한다(실제로는 단기 Pre-signed URL로 재발급).
        lenient().when(fileStorage.presignAttachmentUrl(anyString(), anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(fileStorage.presignAttachmentUrl(isNull(), anyInt())).thenReturn(null);
    }

    @Nested
    @DisplayName("메시지 전송 API")
    class SendMessageApi {

        @Test
        @DisplayName("유효한 요청으로 메시지 전송 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnCreated_when_validMessage() throws Exception {
            // given
            SendMessageRequest request = new SendMessageRequest(100L, "안녕하세요!");

            Message message = Message.builder()
                    .id(500L)
                    .senderId(1L)
                    .chatRoomId(100L)
                    .content("안녕하세요!")
                    .build();

            given(sendMessageUseCase.sendTextMessageAndBroadcast(anyLong(), anyLong(), anyString())).willReturn(message);

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
    @DisplayName("파일 메시지 전송 API")
    class SendFileMessageApi {

        @Test
        @DisplayName("유효한 요청으로 이미지 메시지 전송 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnCreated_when_validImageMessage() throws Exception {
            // given
            SendFileMessageRequest request = SendFileMessageRequest.of(
                    100L,
                    "https://example.com/image.png",
                    "image.png",
                    1024L,
                    "image/png",
                    "https://example.com/thumbnail.png"
            );

            Message message = Message.builder()
                    .id(500L)
                    .senderId(1L)
                    .chatRoomId(100L)
                    .type(Message.MessageType.IMAGE)
                    .fileUrl("https://example.com/image.png")
                    .fileName("image.png")
                    .fileSize(1024L)
                    .fileContentType("image/png")
                    .thumbnailUrl("https://example.com/thumbnail.png")
                    .build();

            // BaseEntity의 createdAt 필드 설정 (JPA Auditing이 동작하지 않는 단위 테스트용)
            ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.now());

            given(sendMessageUseCase.sendFileMessageAndBroadcast(anyLong(), anyLong(), any()))
                    .willReturn(message);

            // when & then
            mockMvc.perform(post("/api/v1/chat/messages/file")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.messageId").value(500L))
                    .andExpect(jsonPath("$.type").value("IMAGE"))
                    .andExpect(jsonPath("$.fileUrl").value("https://example.com/image.png"));
        }

        @Test
        @DisplayName("유효한 요청으로 파일 메시지 전송 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnCreated_when_validFileMessage() throws Exception {
            // given
            SendFileMessageRequest request = SendFileMessageRequest.of(
                    100L,
                    "https://example.com/file.pdf",
                    "document.pdf",
                    2048L,
                    "application/pdf",
                    null
            );

            Message message = Message.builder()
                    .id(500L)
                    .senderId(1L)
                    .chatRoomId(100L)
                    .type(Message.MessageType.FILE)
                    .fileUrl("https://example.com/file.pdf")
                    .fileName("document.pdf")
                    .fileSize(2048L)
                    .fileContentType("application/pdf")
                    .build();

            // BaseEntity의 createdAt 필드 설정 (JPA Auditing이 동작하지 않는 단위 테스트용)
            ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.now());

            given(sendMessageUseCase.sendFileMessageAndBroadcast(anyLong(), anyLong(), any()))
                    .willReturn(message);

            // when & then
            mockMvc.perform(post("/api/v1/chat/messages/file")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.messageId").value(500L))
                    .andExpect(jsonPath("$.type").value("FILE"))
                    .andExpect(jsonPath("$.fileUrl").value("https://example.com/file.pdf"));
        }

        @Test
        @DisplayName("응답의 첨부파일 URL은 저장 원본이 아닌 단기 Pre-signed URL이다 (H-1)")
        @WithMockCustomUser(userId = 1L)
        void should_returnPresignedAttachmentUrl_when_sendFileMessage() throws Exception {
            // given
            String storedFileUrl = "https://minio.example.com/cotalk/uploads/1/image.png";
            String storedThumbnailUrl = "https://minio.example.com/cotalk/uploads/1/thumb.png";
            String presignedFileUrl = "https://minio.example.com/cotalk/uploads/1/image.png?X-Amz-Signature=file";
            String presignedThumbnailUrl = "https://minio.example.com/cotalk/uploads/1/thumb.png?X-Amz-Signature=thumb";

            SendFileMessageRequest request = SendFileMessageRequest.of(
                    100L, storedFileUrl, "image.png", 1024L, "image/png", storedThumbnailUrl);

            Message message = Message.builder()
                    .id(500L)
                    .senderId(1L)
                    .chatRoomId(100L)
                    .type(Message.MessageType.IMAGE)
                    .fileUrl(storedFileUrl)
                    .fileName("image.png")
                    .fileSize(1024L)
                    .fileContentType("image/png")
                    .thumbnailUrl(storedThumbnailUrl)
                    .build();
            ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.now());

            given(sendMessageUseCase.sendFileMessageAndBroadcast(anyLong(), anyLong(), any()))
                    .willReturn(message);
            given(fileStorage.presignAttachmentUrl(eq(storedFileUrl), anyInt())).willReturn(presignedFileUrl);
            given(fileStorage.presignAttachmentUrl(eq(storedThumbnailUrl), anyInt())).willReturn(presignedThumbnailUrl);

            // when & then: 응답에는 저장 원본이 아니라 단기 Pre-signed URL이 노출되어야 한다
            mockMvc.perform(post("/api/v1/chat/messages/file")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.fileUrl").value(presignedFileUrl))
                    .andExpect(jsonPath("$.thumbnailUrl").value(presignedThumbnailUrl));
        }
    }

    @Nested
    @DisplayName("메시지 히스토리 조회 API")
    class GetMessageHistoryApi {

        @Test
        @DisplayName("커서 기반 메시지 조회 - 최신 메시지부터 (beforeMessageId 없음)")
        @WithMockCustomUser(userId = 1L)
        void should_returnLatestMessages_when_noBeforeMessageId() throws Exception {
            // given
            Long roomId = 100L;
            Long userId = 1L;

            Message msg1 = Message.builder()
                    .id(1000L).senderId(1L).chatRoomId(roomId).content("최신 메시지").build();
            Message msg2 = Message.builder()
                    .id(999L).senderId(2L).chatRoomId(roomId).content("이전 메시지").build();

            List<GetMessageHistoryUseCase.EnrichedMessage> enriched = List.of(
                    new GetMessageHistoryUseCase.EnrichedMessage(msg1, 0, "사용자1", null, null, null),
                    new GetMessageHistoryUseCase.EnrichedMessage(msg2, 1, "사용자2", null, null, null)
            );

            GetMessageHistoryUseCase.EnrichedMessageHistoryResult result =
                    new GetMessageHistoryUseCase.EnrichedMessageHistoryResult(enriched, 999L, false);

            given(getMessageHistoryUseCase.getEnrichedMessageHistory(eq(roomId), eq(userId), isNull(), eq(20)))
                    .willReturn(result);

            // when & then
            mockMvc.perform(get("/api/v1/chat/messages/rooms/{roomId}", roomId)
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
        @WithMockCustomUser(userId = 1L)
        void should_returnOlderMessages_when_beforeMessageIdProvided() throws Exception {
            // given
            Long roomId = 100L;
            Long userId = 1L;
            Long beforeMessageId = 1000L;
            int size = 20;

            Message msg1 = Message.builder()
                    .id(999L).senderId(2L).chatRoomId(roomId).content("이전 메시지 1").build();
            Message msg2 = Message.builder()
                    .id(998L).senderId(1L).chatRoomId(roomId).content("이전 메시지 2").build();

            List<GetMessageHistoryUseCase.EnrichedMessage> enriched = List.of(
                    new GetMessageHistoryUseCase.EnrichedMessage(msg1, 0, "사용자2", null, null, null),
                    new GetMessageHistoryUseCase.EnrichedMessage(msg2, 0, "사용자1", null, null, null)
            );

            GetMessageHistoryUseCase.EnrichedMessageHistoryResult result =
                    new GetMessageHistoryUseCase.EnrichedMessageHistoryResult(enriched, 998L, false);

            given(getMessageHistoryUseCase.getEnrichedMessageHistory(roomId, userId, beforeMessageId, size))
                    .willReturn(result);

            // when & then
            mockMvc.perform(get("/api/v1/chat/messages/rooms/{roomId}", roomId)
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
        @WithMockCustomUser(userId = 1L)
        void should_returnHasMoreTrue_when_morePagesExist() throws Exception {
            // given
            Long roomId = 100L;
            Long userId = 1L;
            int size = 2;

            Message msg1 = Message.builder()
                    .id(1000L).senderId(1L).chatRoomId(roomId).content("메시지 1").build();
            Message msg2 = Message.builder()
                    .id(999L).senderId(2L).chatRoomId(roomId).content("메시지 2").build();

            List<GetMessageHistoryUseCase.EnrichedMessage> enriched = List.of(
                    new GetMessageHistoryUseCase.EnrichedMessage(msg1, 0, "사용자1", null, null, null),
                    new GetMessageHistoryUseCase.EnrichedMessage(msg2, 0, "사용자2", null, null, null)
            );

            GetMessageHistoryUseCase.EnrichedMessageHistoryResult result =
                    new GetMessageHistoryUseCase.EnrichedMessageHistoryResult(enriched, 999L, true);

            given(getMessageHistoryUseCase.getEnrichedMessageHistory(eq(roomId), eq(userId), isNull(), eq(size)))
                    .willReturn(result);

            // when & then
            mockMvc.perform(get("/api/v1/chat/messages/rooms/{roomId}", roomId)
                            .param("size", String.valueOf(size)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages.length()").value(2))
                    .andExpect(jsonPath("$.nextCursor").value(999))
                    .andExpect(jsonPath("$.hasMore").value(true));
        }

        @Test
        @DisplayName("메시지가 없을 때 빈 배열과 null nextCursor 반환")
        @WithMockCustomUser(userId = 1L)
        void should_returnEmptyArray_when_noMessages() throws Exception {
            // given
            Long roomId = 100L;
            Long userId = 1L;

            GetMessageHistoryUseCase.EnrichedMessageHistoryResult result =
                    new GetMessageHistoryUseCase.EnrichedMessageHistoryResult(List.of(), null, false);

            given(getMessageHistoryUseCase.getEnrichedMessageHistory(eq(roomId), eq(userId), isNull(), eq(20)))
                    .willReturn(result);

            // when & then
            mockMvc.perform(get("/api/v1/chat/messages/rooms/{roomId}", roomId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages").isArray())
                    .andExpect(jsonPath("$.messages.length()").value(0))
                    .andExpect(jsonPath("$.nextCursor").isEmpty())
                    .andExpect(jsonPath("$.hasMore").value(false));
        }
    }

    @Nested
    @DisplayName("메시지 수정 API")
    class UpdateMessageApi {

        @Test
        @DisplayName("유효한 요청으로 메시지 수정 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_validUpdate() throws Exception {
            // given
            Long messageId = 500L;
            UpdateMessageRequest request = new UpdateMessageRequest("수정된 메시지");

            Message updatedMessage = Message.builder()
                    .id(messageId)
                    .senderId(1L)
                    .chatRoomId(100L)
                    .content("수정된 메시지")
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
        @WithMockCustomUser(userId = 1L)
        void should_returnOk_when_validDelete() throws Exception {
            // given
            Long messageId = 500L;
            Long userId = 1L;

            willDoNothing().given(deleteMessageUseCase).deleteMessage(messageId, userId);

            // when & then
            mockMvc.perform(delete("/api/v1/chat/messages/{messageId}", messageId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("메시지가 삭제되었습니다."));
        }
    }

    @Nested
    @DisplayName("메시지 답장 API")
    class ReplyMessageApi {

        @Test
        @DisplayName("유효한 요청으로 메시지 답장 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnCreated_when_validReply() throws Exception {
            // given
            Long originalMessageId = 500L;
            Long senderId = 1L;
            String content = "답장 메시지입니다.";

            ReplyMessageRequest request = new ReplyMessageRequest(content);

            Message replyMessage = Message.builder()
                    .id(501L)
                    .senderId(senderId)
                    .chatRoomId(100L)
                    .content(content)
                    .replyToMessageId(originalMessageId)
                    .build();

            given(messageReplyForwardUseCase.replyToMessage(senderId, originalMessageId, content))
                    .willReturn(replyMessage);

            // when & then
            mockMvc.perform(post("/api/v1/chat/messages/{messageId}/reply", originalMessageId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.messageId").value(501L))
                    .andExpect(jsonPath("$.content").value(content))
                    .andExpect(jsonPath("$.replyToMessageId").value(originalMessageId));
        }
    }

    @Nested
    @DisplayName("메시지 전달 API")
    class ForwardMessageApi {

        @Test
        @DisplayName("유효한 요청으로 메시지 전달 성공")
        @WithMockCustomUser(userId = 1L)
        void should_returnCreated_when_validForward() throws Exception {
            // given
            Long originalMessageId = 500L;
            Long senderId = 1L;
            Long targetChatRoomId = 200L;

            ForwardMessageRequest request = new ForwardMessageRequest(targetChatRoomId);

            Message forwardedMessage = Message.builder()
                    .id(502L)
                    .senderId(senderId)
                    .chatRoomId(targetChatRoomId)
                    .content("전달된 메시지 내용")
                    .forwardedFromMessageId(originalMessageId)
                    .build();

            given(messageReplyForwardUseCase.forwardMessage(senderId, originalMessageId, targetChatRoomId))
                    .willReturn(forwardedMessage);

            // when & then
            mockMvc.perform(post("/api/v1/chat/messages/{messageId}/forward", originalMessageId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.messageId").value(502L))
                    .andExpect(jsonPath("$.forwardedFromMessageId").value(originalMessageId));
        }
    }
}
