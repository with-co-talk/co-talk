package com.cotalk.adapter.inbound.websocket.dto;

import com.cotalk.domain.constants.MessageConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 텍스트 채팅 메시지 전송 요청 DTO.
 * WebSocket을 통해 클라이언트로부터 수신되는 텍스트 메시지 요청입니다.
 *
 * @param roomId   채팅방 ID
 * @param content  메시지 내용 (최대 5000자)
 * @author seunggu.lee
 */
public record ChatMessageRequest(
        @NotNull(message = "채팅방 ID는 필수입니다.")
        Long roomId,

        @NotBlank(message = "메시지 내용은 필수입니다.")
        @Size(max = MessageConstants.MAX_MESSAGE_LENGTH, message = "메시지는 최대 " + MessageConstants.MAX_MESSAGE_LENGTH + "자까지 가능합니다.")
        String content
) {}
