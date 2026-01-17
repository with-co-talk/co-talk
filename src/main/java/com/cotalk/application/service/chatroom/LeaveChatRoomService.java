package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.port.inbound.chatroom.LeaveChatRoomUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 채팅방 나가기 유스케이스 구현체.
 * 사용자가 채팅방에서 나간다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LeaveChatRoomService implements LeaveChatRoomUseCase {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;

    /**
     * 채팅방에서 나간다.
     * 마지막 멤버가 나가면 채팅방도 함께 삭제된다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 나가는 사용자 ID
     * @throws ChatRoomAccessDeniedException 해당 채팅방의 멤버가 아닌 경우
     */
    @Override
    public void leaveChatRoom(Long chatRoomId, Long userId) {
        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new ChatRoomAccessDeniedException(chatRoomId, userId));

        // 멤버 삭제
        chatRoomMemberRepository.delete(member);

        // 남은 멤버 확인
        List<ChatRoomMember> remainingMembers = chatRoomMemberRepository.findByChatRoomId(chatRoomId);

        // 마지막 멤버가 나가면 채팅방도 삭제
        if (remainingMembers.isEmpty()) {
            chatRoomRepository.findById(chatRoomId).ifPresent(chatRoom -> {
                chatRoomRepository.delete(chatRoom);
                log.info("Chat room deleted as last member left: chatRoomId={}", chatRoomId);
            });
        }

        log.info("User left chat room: userId={}, chatRoomId={}", userId, chatRoomId);
    }
}
