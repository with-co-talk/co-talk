package com.cotalk.application.service.message;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.exception.ChatRoomNotFoundException;
import com.cotalk.domain.port.inbound.message.GetMessageHistoryUseCase;
import com.cotalk.domain.port.outbound.persistence.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.persistence.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 메시지 히스토리 조회 Use Case 구현체.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMessageHistoryService implements GetMessageHistoryUseCase {

    private final MessageRepository messageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Override
    public List<Message> getMessageHistory(UUID chatRoomId, UUID userId, int page, int size) {
        // 사용자가 채팅방 멤버인지 확인
        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new ChatRoomNotFoundException(chatRoomId));

        // 메시지 히스토리 조회
        return messageRepository.findByChatRoomIdOrderByCreatedAtDesc(chatRoomId, page, size);
    }
}
