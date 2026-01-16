package com.cotalk.application.service;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.ChatRoomSummary;
import com.cotalk.domain.port.inbound.GetChatRoomsUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetChatRoomsService implements GetChatRoomsUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Override
    public List<ChatRoomSummary> getChatRooms(Long userId) {
        List<ChatRoom> chatRooms = chatRoomRepository.findByUserId(userId);

        return chatRooms.stream()
                .map(chatRoom -> buildChatRoomSummary(chatRoom, userId))
                .toList();
    }

    private ChatRoomSummary buildChatRoomSummary(ChatRoom chatRoom, Long userId) {
        Long chatRoomId = chatRoom.getId();

        // 내 멤버 정보 조회
        ChatRoomMember myMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElse(null);

        // 마지막 메시지 정보
        LastMessageInfo lastMessageInfo = getLastMessageInfo(chatRoomId);

        // 안 읽은 메시지 개수
        long unreadCount = calculateUnreadCount(chatRoomId, userId, myMember);

        // 상대방 정보 (1:1 채팅인 경우)
        OtherUserInfo otherUserInfo = getOtherUserInfo(chatRoom, userId);

        return new ChatRoomSummary(
                chatRoom.getId(),
                chatRoom.getName(),
                chatRoom.getType(),
                chatRoom.getCreatedAt(),
                lastMessageInfo.content(),
                lastMessageInfo.createdAt(),
                unreadCount,
                otherUserInfo.userId(),
                otherUserInfo.nickname(),
                otherUserInfo.avatarUrl()
        );
    }

    /**
     * 마지막 메시지 정보를 조회합니다.
     */
    private LastMessageInfo getLastMessageInfo(Long chatRoomId) {
        return messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId)
                .map(msg -> new LastMessageInfo(msg.getContent(), msg.getCreatedAt()))
                .orElse(new LastMessageInfo(null, null));
    }

    /**
     * 안 읽은 메시지 개수를 계산합니다.
     */
    private long calculateUnreadCount(Long chatRoomId, Long userId, ChatRoomMember myMember) {
        if (myMember == null || myMember.getLastReadAt() == null) {
            return 0;
        }
        return messageRepository.countUnreadMessages(chatRoomId, userId, myMember.getLastReadAt());
    }

    /**
     * 1:1 채팅방의 상대방 정보를 조회합니다.
     */
    private OtherUserInfo getOtherUserInfo(ChatRoom chatRoom, Long userId) {
        if (chatRoom.getType() != ChatRoom.ChatRoomType.DIRECT) {
            return new OtherUserInfo(null, null, null);
        }

        return chatRoomMemberRepository.findByChatRoomId(chatRoom.getId()).stream()
                .filter(member -> !member.getUserId().equals(userId))
                .findFirst()
                .flatMap(member -> userRepository.findById(member.getUserId()))
                .map(user -> new OtherUserInfo(user.getId(), user.getNickname(), user.getAvatarUrl()))
                .orElse(new OtherUserInfo(null, null, null));
    }

    /**
     * 마지막 메시지 정보를 담는 레코드
     */
    private record LastMessageInfo(String content, java.time.LocalDateTime createdAt) {}

    /**
     * 상대방 사용자 정보를 담는 레코드
     */
    private record OtherUserInfo(Long userId, String nickname, String avatarUrl) {}
}
