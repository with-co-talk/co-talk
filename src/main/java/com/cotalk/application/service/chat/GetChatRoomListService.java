package com.cotalk.application.service.chat;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.port.inbound.chat.GetChatRoomListUseCase;
import com.cotalk.domain.port.outbound.persistence.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 채팅방 목록 조회 Use Case 구현체.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetChatRoomListService implements GetChatRoomListUseCase {

    private final ChatRoomRepository chatRoomRepository;

    @Override
    public List<ChatRoom> getChatRoomList(UUID userId) {
        return chatRoomRepository.findByUserId(userId);
    }
}
