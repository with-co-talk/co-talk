package com.cotalk.application.service;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.port.inbound.MarkAsReadUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class MarkAsReadService implements MarkAsReadUseCase {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomMemberValidator chatRoomMemberValidator;

    @Override
    public void markAsRead(Long userId, Long chatRoomId) {
        ChatRoomMember member = chatRoomMemberValidator.getMemberOrThrow(chatRoomId, userId);

        member.updateLastReadAt(LocalDateTime.now());
        chatRoomMemberRepository.save(member);
    }
}
