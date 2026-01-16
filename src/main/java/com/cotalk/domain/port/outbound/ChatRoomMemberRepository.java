package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.ChatRoomMember;

import java.util.List;
import java.util.Optional;


public interface ChatRoomMemberRepository {
    ChatRoomMember save(ChatRoomMember member);
    Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);
    List<ChatRoomMember> findByChatRoomId(Long chatRoomId);
    List<ChatRoomMember> findByUserId(Long userId);
    void delete(ChatRoomMember member);
    void deleteByUserId(Long userId);
}
