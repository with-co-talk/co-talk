package com.cotalk.adapter.inbound.rest.chat;

import com.cotalk.application.dto.response.ApiResponse;
import com.cotalk.application.dto.response.ChatRoomResponse;
import com.cotalk.application.dto.response.MessageResponse;
import com.cotalk.application.dto.response.UserResponse;
import com.cotalk.application.service.chat.CreateChatRoomService;
import com.cotalk.application.service.chat.GetChatRoomListService;
import com.cotalk.application.service.chat.GetChatRoomService;
import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.outbound.persistence.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.persistence.MessageRepository;
import com.cotalk.domain.port.outbound.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 채팅 REST API 컨트롤러.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final CreateChatRoomService createChatRoomService;
    private final GetChatRoomListService getChatRoomListService;
    private final GetChatRoomService getChatRoomService;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    /**
     * 채팅방 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> createChatRoom(
            @RequestParam UUID userId2,
            Authentication authentication) {
        UUID userId1 = (UUID) authentication.getPrincipal();
        UUID chatRoomId = createChatRoomService.createChatRoom(userId1, userId2);
        return ResponseEntity.ok(ApiResponse.success(chatRoomId));
    }

    /**
     * 채팅방 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getChatRoomList(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<ChatRoom> chatRooms = getChatRoomListService.getChatRoomList(userId);
        List<ChatRoomResponse> responses = chatRooms.stream()
                .map(room -> toChatRoomResponse(room, userId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 채팅방 조회
     */
    @GetMapping("/{chatRoomId}")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> getChatRoom(
            @PathVariable UUID chatRoomId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        ChatRoom chatRoom = getChatRoomService.getChatRoom(chatRoomId, userId);
        return ResponseEntity.ok(ApiResponse.success(toChatRoomResponse(chatRoom, userId)));
    }

    private ChatRoomResponse toChatRoomResponse(ChatRoom chatRoom, UUID currentUserId) {
        // 채팅방 멤버 조회
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(chatRoom.getId());
        List<UserResponse> memberResponses = members.stream()
                .map(member -> userRepository.findById(member.getUserId()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(this::toUserResponse)
                .collect(Collectors.toList());

        // 최근 메시지 조회
        List<Message> messages = messageRepository.findByChatRoomIdOrderByCreatedAtDesc(
                chatRoom.getId(), 0, 1);
        MessageResponse lastMessage = messages.isEmpty() ? null : toMessageResponse(messages.get(0));

        // 읽지 않은 메시지 수 계산
        ChatRoomMember currentMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoom.getId(), currentUserId)
                .orElse(null);
        long unreadCount = 0;
        if (currentMember != null && currentMember.getLastReadAt() != null) {
            unreadCount = messageRepository.countUnreadMessages(
                    chatRoom.getId(), currentUserId, currentMember.getLastReadAt());
        }

        return new ChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getType().name(),
                memberResponses,
                lastMessage,
                unreadCount,
                chatRoom.getUpdatedAt()
        );
    }

    private UserResponse toUserResponse(com.cotalk.domain.entity.User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl()
        );
    }

    private MessageResponse toMessageResponse(Message message) {
        com.cotalk.domain.entity.User sender = userRepository.findById(message.getSenderId())
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
}
