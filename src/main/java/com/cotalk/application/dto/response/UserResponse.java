package com.cotalk.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * 사용자 응답 DTO.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public class UserResponse {
    
    private UUID id;
    private String email;
    private String nickname;
    private String avatarUrl;
}
