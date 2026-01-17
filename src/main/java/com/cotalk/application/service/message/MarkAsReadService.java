package com.cotalk.application.service.message;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.port.inbound.message.MarkAsReadUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 메시지 읽음 처리 유스케이스 구현체.
 * 채팅방의 메시지를 읽음 처리한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MarkAsReadService implements MarkAsReadUseCase {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomMemberValidator chatRoomMemberValidator;

    /**
     * 채팅방의 메시지를 읽음 처리한다.
     * 현재 시간을 마지막 읽은 시간으로 업데이트한다.
     *
     * @param userId 사용자 ID
     * @param chatRoomId 채팅방 ID
     * @throws ChatRoomAccessDeniedException 채팅방 멤버가 아닌 경우
     */
    @Override
    public void markAsRead(Long userId, Long chatRoomId) {
        ChatRoomMember member = chatRoomMemberValidator.getMemberOrThrow(chatRoomId, userId);

        member.updateLastReadAt(LocalDateTime.now());
        chatRoomMemberRepository.save(member);
    }
}
