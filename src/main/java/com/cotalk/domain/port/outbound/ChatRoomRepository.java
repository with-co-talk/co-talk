package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.ChatRoom;

import java.util.List;
import java.util.Optional;


public interface ChatRoomRepository {
    ChatRoom save(ChatRoom chatRoom);
    Optional<ChatRoom> findById(Long id);
    List<ChatRoom> findByUserId(Long userId);
    Optional<ChatRoom> findDirectChatRoomByUserIds(Long userId1, Long userId2);
}
