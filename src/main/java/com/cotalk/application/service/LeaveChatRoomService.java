package com.cotalk.application.service;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.port.inbound.LeaveChatRoomUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LeaveChatRoomService implements LeaveChatRoomUseCase {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;

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
