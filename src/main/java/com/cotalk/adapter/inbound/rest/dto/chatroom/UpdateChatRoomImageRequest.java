package com.cotalk.adapter.inbound.rest.dto.chatroom;

import jakarta.validation.constraints.NotBlank;

/**
 * 채팅방 이미지 변경 요청 DTO.
 *
 * @param imageUrl 새 이미지 URL
 * @author seunggu.lee
 */
public record UpdateChatRoomImageRequest(
        @NotBlank(message = "이미지 URL은 필수입니다.")
        String imageUrl
) {}
