package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.inbound.DeleteMessageUseCase;
import com.cotalk.domain.port.inbound.GetMessageHistoryUseCase;
import com.cotalk.domain.port.inbound.SendMessageUseCase;
import com.cotalk.domain.port.inbound.UpdateMessageUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat/messages")
@RequiredArgsConstructor
@Tag(name = "채팅 메시지", description = "채팅 메시지 API")
public class ChatMessageController {

    private final SendMessageUseCase sendMessageUseCase;
    private final GetMessageHistoryUseCase getMessageHistoryUseCase;
    private final UpdateMessageUseCase updateMessageUseCase;
    private final DeleteMessageUseCase deleteMessageUseCase;

    @Operation(summary = "메시지 전송", description = "채팅방에 메시지를 전송합니다.")
    @PostMapping
    public ResponseEntity<SendMessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        Message message = sendMessageUseCase.sendMessage(request.senderId(), request.chatRoomId(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SendMessageResponse.from(message));
    }

    @Operation(summary = "파일/이미지 메시지 전송", description = "채팅방에 파일 또는 이미지를 전송합니다.")
    @PostMapping("/file")
    public ResponseEntity<SendMessageResponse> sendFileMessage(@Valid @RequestBody SendFileMessageRequest request) {
        SendMessageUseCase.FileMessageCommand command = new SendMessageUseCase.FileMessageCommand(
                request.fileUrl(),
                request.fileName(),
                request.fileSize(),
                request.contentType(),
                request.thumbnailUrl()
        );

        Message message = sendMessageUseCase.sendFileMessage(request.chatRoomId(), request.senderId(), command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SendMessageResponse.from(message));
    }

    @Operation(summary = "메시지 히스토리 조회", description = "채팅방의 메시지 히스토리를 커서 기반으로 조회합니다.")
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<MessageHistoryResponse> getMessageHistory(
            @PathVariable Long roomId,
            @RequestParam Long userId,
            @RequestParam(required = false) Long beforeMessageId,
            @RequestParam(defaultValue = "20") int size) {
        List<Message> messages = getMessageHistoryUseCase.getMessageHistory(roomId, userId, beforeMessageId, size);
        List<MessageDto> messageDtos = messages.stream()
                .map(MessageDto::from)
                .toList();

        // 다음 페이지 존재 여부를 위한 nextCursor 계산
        Long nextCursor = messages.isEmpty() ? null : messages.get(messages.size() - 1).getId();
        boolean hasMore = messages.size() == size;

        return ResponseEntity.ok(new MessageHistoryResponse(messageDtos, nextCursor, hasMore));
    }

    @Operation(summary = "메시지 수정", description = "본인이 보낸 메시지를 수정합니다.")
    @PutMapping("/{messageId}")
    public ResponseEntity<UpdateMessageResponse> updateMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody UpdateMessageRequest request) {
        Message message = updateMessageUseCase.updateMessage(messageId, request.userId(), request.content());
        return ResponseEntity.ok(UpdateMessageResponse.from(message));
    }

    @Operation(summary = "메시지 삭제", description = "본인이 보낸 메시지를 삭제합니다.")
    @DeleteMapping("/{messageId}")
    public ResponseEntity<DeleteMessageResponse> deleteMessage(
            @PathVariable Long messageId,
            @RequestParam Long userId) {
        deleteMessageUseCase.deleteMessage(messageId, userId);
        return ResponseEntity.ok(new DeleteMessageResponse("메시지가 삭제되었습니다."));
    }

    // Request DTOs
    public record SendMessageRequest(
            @NotNull(message = "발신자 ID는 필수입니다.")
            Long senderId,

            @NotNull(message = "채팅방 ID는 필수입니다.")
            Long chatRoomId,

            @NotBlank(message = "메시지 내용은 필수입니다.")
            String content
    ) {}

    public record SendFileMessageRequest(
            @NotNull(message = "발신자 ID는 필수입니다.")
            Long senderId,

            @NotNull(message = "채팅방 ID는 필수입니다.")
            Long chatRoomId,

            @NotBlank(message = "파일 URL은 필수입니다.")
            String fileUrl,

            @NotBlank(message = "파일명은 필수입니다.")
            String fileName,

            @NotNull(message = "파일 크기는 필수입니다.")
            Long fileSize,

            @NotBlank(message = "파일 형식은 필수입니다.")
            String contentType,

            String thumbnailUrl  // 선택 (이미지인 경우)
    ) {}

    public record UpdateMessageRequest(
            @NotNull(message = "사용자 ID는 필수입니다.")
            Long userId,

            @NotBlank(message = "메시지 내용은 필수입니다.")
            String content
    ) {}

    // Response DTOs
    public record SendMessageResponse(
            Long messageId,
            String content,
            String type,
            LocalDateTime createdAt,
            String fileUrl,
            String fileName,
            Long fileSize,
            String contentType,
            String thumbnailUrl
    ) {
        public static SendMessageResponse from(Message message) {
            return new SendMessageResponse(
                    message.getId(),
                    message.getContent(),
                    message.getType().name(),
                    message.getCreatedAt(),
                    message.getFileUrl(),
                    message.getFileName(),
                    message.getFileSize(),
                    message.getFileContentType(),
                    message.getThumbnailUrl()
            );
        }
    }

    public record MessageHistoryResponse(List<MessageDto> messages, Long nextCursor, boolean hasMore) {}

    public record MessageDto(
            Long id,
            Long senderId,
            String content,
            String type,
            LocalDateTime createdAt,
            String fileUrl,
            String fileName,
            Long fileSize,
            String contentType,
            String thumbnailUrl
    ) {
        public static MessageDto from(Message m) {
            return new MessageDto(
                    m.getId(),
                    m.getSenderId(),
                    m.getContent(),
                    m.getType().name(),
                    m.getCreatedAt(),
                    m.getFileUrl(),
                    m.getFileName(),
                    m.getFileSize(),
                    m.getFileContentType(),
                    m.getThumbnailUrl()
            );
        }
    }

    public record UpdateMessageResponse(
            Long messageId,
            String content,
            LocalDateTime updatedAt
    ) {
        public static UpdateMessageResponse from(Message message) {
            return new UpdateMessageResponse(
                    message.getId(),
                    message.getContent(),
                    message.getUpdatedAt()
            );
        }
    }

    public record DeleteMessageResponse(String message) {}
}
