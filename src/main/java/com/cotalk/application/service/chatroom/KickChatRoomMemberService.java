package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.exception.ChatRoomNotFoundException;
import com.cotalk.domain.exception.InvalidChatRoomException;
import com.cotalk.domain.port.inbound.chatroom.KickChatRoomMemberUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 채팅방 멤버 강제 퇴장 유스케이스 구현체.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class KickChatRoomMemberService implements KickChatRoomMemberUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    /**
     * 채팅방에서 멤버를 강제 퇴장시킨다.
     *
     * @param chatRoomId   채팅방 ID
     * @param adminUserId  관리자 사용자 ID
     * @param targetUserId 강제 퇴장시킬 사용자 ID
     * @throws ChatRoomAccessDeniedException 관리자가 아니거나 대상이 채팅방 멤버가 아닌 경우
     * @throws InvalidChatRoomException      1:1 채팅방이거나 자기 자신을 퇴장시키려는 경우
     */
    @Override
    public void kickMember(Long chatRoomId, Long adminUserId, Long targetUserId) {
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(chatRoomId));

        // 1:1 채팅방 검증
        if (chatRoom.getType() == ChatRoom.ChatRoomType.DIRECT || chatRoom.isSelfChat()) {
            throw new InvalidChatRoomException("1:1 채팅방 또는 나와의 채팅방에서는 멤버를 강제 퇴장시킬 수 없습니다.");
        }

        // 자기 자신 강제 퇴장 검증
        if (adminUserId.equals(targetUserId)) {
            throw new InvalidChatRoomException("자기 자신을 강제 퇴장시킬 수 없습니다.");
        }

        // 관리자 권한 검증
        ChatRoomMember adminMember = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, adminUserId)
                .orElseThrow(() -> new ChatRoomAccessDeniedException("채팅방 멤버가 아닙니다."));

        if (!adminMember.isAdmin()) {
            throw new ChatRoomAccessDeniedException("관리자만 멤버를 강제 퇴장시킬 수 있습니다.");
        }

        // 대상 멤버 조회
        ChatRoomMember targetMember = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, targetUserId)
                .orElseThrow(() -> new ChatRoomAccessDeniedException("대상 사용자가 채팅방 멤버가 아닙니다."));

        // 강제 퇴장 처리
        chatRoomMemberRepository.delete(targetMember);

        log.info("Member kicked from chat room: chatRoomId={}, adminUserId={}, targetUserId={}",
                chatRoomId, adminUserId, targetUserId);
    }
}
