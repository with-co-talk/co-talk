package com.cotalk.adapter.inbound.rest.message;

import com.cotalk.application.dto.request.SendMessageRequest;
import com.cotalk.application.dto.response.ApiResponse;
import com.cotalk.application.dto.response.MessageResponse;
import com.cotalk.application.dto.response.UserResponse;
import com.cotalk.application.service.message.GetMessageHistoryService;
import com.cotalk.application.service.message.MarkMessagesAsReadService;
import com.cotalk.application.service.message.SendMessageService;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.outbound.persistence.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 메시지 REST API 컨트롤러.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final SendMessageService sendMessageService;
    private final GetMessageHistoryService getMessageHistoryService;
    private final MarkMessagesAsReadService markMessagesAsReadService;
    private final UserRepository userRepository;

    /**
     * 메시지 전송
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {
        UUID senderId = (UUID) authentication.getPrincipal();
        Message message = sendMessageService.sendMessage(
                request.getChatRoomId(), senderId, request.getContent());
        return ResponseEntity.ok(ApiResponse.success(toMessageResponse(message)));
    }

    /**
     * 메시지 히스토리 조회
     */
    @GetMapping("/chat/{chatRoomId}")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessageHistory(
            @PathVariable UUID chatRoomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<Message> messages = getMessageHistoryService.getMessageHistory(chatRoomId, userId, page, size);
        List<MessageResponse> responses = messages.stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 메시지 읽음 처리
     */
    @PostMapping("/chat/{chatRoomId}/read")
    public ResponseEntity<ApiResponse<Void>> markMessagesAsRead(
            @PathVariable UUID chatRoomId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        markMessagesAsReadService.markMessagesAsRead(chatRoomId, userId);
        return ResponseEntity.ok(ApiResponse.success("메시지가 읽음 처리되었습니다.", null));
    }

    private MessageResponse toMessageResponse(Message message) {
        User sender = userRepository.findById(message.getSenderId())
                .orElse(null);
        UserResponse senderResponse = sender != null ? toUserResponse(sender) : null;

        return new MessageResponse(
                message.getId(),
                message.getChatRoomId(),
                message.getSenderId(),
                senderResponse,
                message.getContent(),
                message.getMessageType().name(),
                message.getCreatedAt()
        );
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl()
        );
    }
}
