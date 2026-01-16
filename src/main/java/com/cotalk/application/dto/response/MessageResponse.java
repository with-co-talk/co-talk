package com.cotalk.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 메시지 응답 DTO.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public class MessageResponse {
    
    private UUID id;
    private UUID chatRoomId;
    private UUID senderId;
    private UserResponse sender;
    private String content;
    private String messageType;
    private LocalDateTime createdAt;
}
