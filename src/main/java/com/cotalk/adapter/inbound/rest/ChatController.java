package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.ChatRoomSummary;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.inbound.CreateChatRoomUseCase;
import com.cotalk.domain.port.inbound.GetChatRoomsUseCase;
import com.cotalk.domain.port.inbound.GetMessageHistoryUseCase;
import com.cotalk.domain.port.inbound.DeleteMessageUseCase;
import com.cotalk.domain.port.inbound.LeaveChatRoomUseCase;
import com.cotalk.domain.port.inbound.MarkAsReadUseCase;
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
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "채팅", description = "채팅방 및 메시지 API")
public class ChatController {

    private final CreateChatRoomUseCase createChatRoomUseCase;
    private final SendMessageUseCase sendMessageUseCase;
    private final GetMessageHistoryUseCase getMessageHistoryUseCase;
    private final LeaveChatRoomUseCase leaveChatRoomUseCase;
    private final MarkAsReadUseCase markAsReadUseCase;
    private final GetChatRoomsUseCase getChatRoomsUseCase;
    private final UpdateMessageUseCase updateMessageUseCase;
    private final DeleteMessageUseCase deleteMessageUseCase;

    @Operation(summary = "채팅방 생성", description = "1:1 채팅방을 생성합니다.")
    @PostMapping("/rooms")
    public ResponseEntity<CreateChatRoomResponse> createChatRoom(@Valid @RequestBody CreateChatRoomRequest request) {
        Long roomId = createChatRoomUseCase.createChatRoom(request.userId1(), request.userId2());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateChatRoomResponse(roomId, "채팅방이 생성되었습니다."));
    }

    @Operation(summary = "메시지 전송", description = "채팅방에 메시지를 전송합니다.")
    @PostMapping("/messages")
    public ResponseEntity<SendMessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        Message message = sendMessageUseCase.sendMessage(request.senderId(), request.chatRoomId(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SendMessageResponse.from(message));
    }

    @Operation(summary = "파일/이미지 메시지 전송", description = "채팅방에 파일 또는 이미지를 전송합니다.")
    @PostMapping("/messages/file")
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
    @GetMapping("/rooms/{roomId}/messages")
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

    @Operation(summary = "채팅방 나가기", description = "채팅방에서 나갑니다.")
    @PostMapping("/rooms/{roomId}/leave")
    public ResponseEntity<LeaveChatRoomResponse> leaveChatRoom(
            @PathVariable Long roomId,
            @RequestParam Long userId) {
        leaveChatRoomUseCase.leaveChatRoom(roomId, userId);
        return ResponseEntity.ok(new LeaveChatRoomResponse("채팅방을 나갔습니다."));
    }

    @Operation(summary = "메시지 수정", description = "본인이 보낸 메시지를 수정합니다.")
    @PutMapping("/messages/{messageId}")
    public ResponseEntity<UpdateMessageResponse> updateMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody UpdateMessageRequest request) {
        Message message = updateMessageUseCase.updateMessage(messageId, request.userId(), request.content());
        return ResponseEntity.ok(UpdateMessageResponse.from(message));
    }

    @Operation(summary = "메시지 삭제", description = "본인이 보낸 메시지를 삭제합니다.")
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<DeleteMessageResponse> deleteMessage(
            @PathVariable Long messageId,
            @RequestParam Long userId) {
        deleteMessageUseCase.deleteMessage(messageId, userId);
        return ResponseEntity.ok(new DeleteMessageResponse("메시지가 삭제되었습니다."));
    }

    @Operation(summary = "읽음 표시", description = "채팅방 메시지를 읽음 처리합니다.")
    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<MarkAsReadResponse> markAsRead(
            @PathVariable Long roomId,
            @RequestParam Long userId) {
        markAsReadUseCase.markAsRead(userId, roomId);
        return ResponseEntity.ok(new MarkAsReadResponse("읽음 처리되었습니다."));
    }

    @Operation(summary = "채팅방 목록 조회", description = "사용자의 채팅방 목록을 조회합니다.")
    @GetMapping("/rooms")
    public ResponseEntity<ChatRoomsResponse> getChatRooms(@RequestParam Long userId) {
        List<ChatRoomSummary> chatRooms = getChatRoomsUseCase.getChatRooms(userId);
        List<ChatRoomDto> roomDtos = chatRooms.stream()
                .map(r -> new ChatRoomDto(
                        r.id(),
                        r.name(),
                        r.type().name(),
                        r.createdAt(),
                        r.lastMessage(),
                        r.lastMessageAt(),
                        r.unreadCount(),
                        r.otherUserId(),
                        r.otherUserNickname(),
                        r.otherUserAvatarUrl()))
                .toList();
        return ResponseEntity.ok(new ChatRoomsResponse(roomDtos));
    }

    // Request DTOs
    public record CreateChatRoomRequest(
            @NotNull(message = "첫 번째 사용자 ID는 필수입니다.")
            Long userId1,

            @NotNull(message = "두 번째 사용자 ID는 필수입니다.")
            Long userId2
    ) {}

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

    // Response DTOs
    public record CreateChatRoomResponse(Long roomId, String message) {}
    
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

    public record UpdateMessageRequest(
            @NotNull(message = "사용자 ID는 필수입니다.")
            Long userId,

            @NotBlank(message = "메시지 내용은 필수입니다.")
            String content
    ) {}

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

    public record LeaveChatRoomResponse(String message) {}
    public record MarkAsReadResponse(String message) {}
    public record ChatRoomsResponse(List<ChatRoomDto> rooms) {}
    public record ChatRoomDto(
            Long id,
            String name,
            String type,
            LocalDateTime createdAt,
            String lastMessage,
            LocalDateTime lastMessageAt,
            long unreadCount,
            Long otherUserId,
            String otherUserNickname,
            String otherUserAvatarUrl) {}
}
