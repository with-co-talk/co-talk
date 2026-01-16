package com.cotalk.domain.port.inbound;

import com.cotalk.domain.entity.ChatRoomSummary;

import java.util.List;

public interface GetChatRoomsUseCase {
    List<ChatRoomSummary> getChatRooms(Long userId);
}
