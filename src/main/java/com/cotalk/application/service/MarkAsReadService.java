package com.cotalk.application.service;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.MarkAsReadUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class MarkAsReadService implements MarkAsReadUseCase {

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Override
    public void markAsRead(Long userId, Long chatRoomId) {
        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new DomainException("채팅방 멤버가 아닙니다."));

        member.updateLastReadAt(LocalDateTime.now());
        chatRoomMemberRepository.save(member);
    }
}
