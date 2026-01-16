package com.cotalk.domain.validator;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 채팅방 멤버십 검증을 담당하는 Validator
 */
@Component
@RequiredArgsConstructor
public class ChatRoomMemberValidator {

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    /**
     * 사용자가 채팅방 멤버인지 검증합니다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     * @throws ChatRoomAccessDeniedException 사용자가 채팅방 멤버가 아닌 경우
     */
    public void validateMembership(Long chatRoomId, Long userId) {
        chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new ChatRoomAccessDeniedException(chatRoomId, userId));
    }

    /**
     * 사용자가 채팅방 멤버인지 검증하고 멤버 객체를 반환합니다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 채팅방 멤버 객체
     * @throws ChatRoomAccessDeniedException 사용자가 채팅방 멤버가 아닌 경우
     */
    public ChatRoomMember getMemberOrThrow(Long chatRoomId, Long userId) {
        return chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new ChatRoomAccessDeniedException(chatRoomId, userId));
    }
}
