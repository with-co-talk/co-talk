package com.cotalk.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 메시지 전송 요청 DTO.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    
    @NotNull(message = "채팅방 ID는 필수입니다.")
    private UUID chatRoomId;

    @NotBlank(message = "메시지 내용은 필수입니다.")
    @Size(max = 1000, message = "메시지는 최대 1000자까지 입력 가능합니다.")
    private String content;
}
