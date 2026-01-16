package com.cotalk.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 친구 요청 전송 요청 DTO.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SendFriendRequestRequest {
    
    @NotNull(message = "수신자 ID는 필수입니다.")
    private UUID receiverId;
}
