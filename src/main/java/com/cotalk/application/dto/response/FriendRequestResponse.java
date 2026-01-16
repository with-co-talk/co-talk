package com.cotalk.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 친구 요청 응답 DTO.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public class FriendRequestResponse {
    
    private UUID id;
    private UUID requesterId;
    private UserResponse requester;
    private String status;
    private LocalDateTime createdAt;
}
