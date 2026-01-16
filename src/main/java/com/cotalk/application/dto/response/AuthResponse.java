package com.cotalk.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 인증 응답 DTO.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public class AuthResponse {
    
    private String token;
    private UserResponse user;
}
