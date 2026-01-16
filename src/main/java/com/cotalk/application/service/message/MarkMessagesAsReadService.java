package com.cotalk.application.service.message;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.exception.ChatRoomNotFoundException;
import com.cotalk.domain.port.inbound.message.MarkMessagesAsReadUseCase;
import com.cotalk.domain.port.outbound.persistence.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 메시지 읽음 처리 Use Case 구현체.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MarkMessagesAsReadService implements MarkMessagesAsReadUseCase {

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Override
    public void markMessagesAsRead(UUID chatRoomId, UUID userId) {
        // 사용자가 채팅방 멤버인지 확인
        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new ChatRoomNotFoundException(chatRoomId));

        // 마지막 읽은 시간 업데이트
        member.updateLastReadAt();
        chatRoomMemberRepository.save(member);
    }
}
