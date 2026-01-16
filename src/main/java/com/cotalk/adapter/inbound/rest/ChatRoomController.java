package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.ChatRoomSummary;
import com.cotalk.domain.port.inbound.CreateChatRoomUseCase;
import com.cotalk.domain.port.inbound.GetChatRoomsUseCase;
import com.cotalk.domain.port.inbound.LeaveChatRoomUseCase;
import com.cotalk.domain.port.inbound.MarkAsReadUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
@Tag(name = "채팅방", description = "채팅방 관리 API")
public class ChatRoomController {

    private final CreateChatRoomUseCase createChatRoomUseCase;
    private final GetChatRoomsUseCase getChatRoomsUseCase;
    private final LeaveChatRoomUseCase leaveChatRoomUseCase;
    private final MarkAsReadUseCase markAsReadUseCase;

    @Operation(summary = "채팅방 생성", description = "1:1 채팅방을 생성합니다.")
    @PostMapping
    public ResponseEntity<CreateChatRoomResponse> createChatRoom(@Valid @RequestBody CreateChatRoomRequest request) {
        Long roomId = createChatRoomUseCase.createChatRoom(request.userId1(), request.userId2());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateChatRoomResponse(roomId, "채팅방이 생성되었습니다."));
    }

    @Operation(summary = "채팅방 목록 조회", description = "사용자의 채팅방 목록을 조회합니다.")
    @GetMapping
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

    @Operation(summary = "채팅방 나가기", description = "채팅방에서 나갑니다.")
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<LeaveChatRoomResponse> leaveChatRoom(
            @PathVariable Long roomId,
            @RequestParam Long userId) {
        leaveChatRoomUseCase.leaveChatRoom(roomId, userId);
        return ResponseEntity.ok(new LeaveChatRoomResponse("채팅방을 나갔습니다."));
    }

    @Operation(summary = "읽음 표시", description = "채팅방 메시지를 읽음 처리합니다.")
    @PostMapping("/{roomId}/read")
    public ResponseEntity<MarkAsReadResponse> markAsRead(
            @PathVariable Long roomId,
            @RequestParam Long userId) {
        markAsReadUseCase.markAsRead(userId, roomId);
        return ResponseEntity.ok(new MarkAsReadResponse("읽음 처리되었습니다."));
    }

    // Request DTOs
    public record CreateChatRoomRequest(
            @NotNull(message = "첫 번째 사용자 ID는 필수입니다.")
            Long userId1,

            @NotNull(message = "두 번째 사용자 ID는 필수입니다.")
            Long userId2
    ) {}

    // Response DTOs
    public record CreateChatRoomResponse(Long roomId, String message) {}
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
