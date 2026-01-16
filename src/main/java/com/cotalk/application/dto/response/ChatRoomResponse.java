package com.cotalk.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 채팅방 응답 DTO.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public class ChatRoomResponse {
    
    private UUID id;
    private String type;
    private List<UserResponse> members;
    private MessageResponse lastMessage;
    private long unreadCount;
    private LocalDateTime updatedAt;
}
