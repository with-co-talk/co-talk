package com.cotalk.application.service;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.ChatRoomSummary;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.GetChatRoomsUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

        // 내 멤버 정보 조회 (lastReadAt 확인용)
        Optional<ChatRoomMember> myMemberOpt = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId);
        ChatRoomMember myMember = myMemberOpt.orElse(null);

        // 마지막 메시지 조회
        Optional<Message> lastMessageOpt = messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId);
        String lastMessageContent = lastMessageOpt.map(Message::getContent).orElse(null);
        var lastMessageAt = lastMessageOpt.map(Message::getCreatedAt).orElse(null);

        // 안 읽은 메시지 개수 계산
        long unreadCount = 0;
        if (myMember != null && myMember.getLastReadAt() != null) {
            unreadCount = messageRepository.countUnreadMessages(chatRoomId, userId, myMember.getLastReadAt());
        }

        // 상대방 정보 조회 (1:1 채팅인 경우)
        Long otherUserId = null;
        String otherUserNickname = null;
        String otherUserAvatarUrl = null;

        if (chatRoom.getType() == ChatRoom.ChatRoomType.DIRECT) {
            List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(chatRoomId);
            Optional<ChatRoomMember> otherMemberOpt = members.stream()
                    .filter(m -> !m.getUserId().equals(userId))
                    .findFirst();

            if (otherMemberOpt.isPresent()) {
                otherUserId = otherMemberOpt.get().getUserId();
                Optional<User> otherUserOpt = userRepository.findById(otherUserId);
                if (otherUserOpt.isPresent()) {
                    User otherUser = otherUserOpt.get();
                    otherUserNickname = otherUser.getNickname();
                    otherUserAvatarUrl = otherUser.getAvatarUrl();
                }
            }
        }

        return new ChatRoomSummary(
                chatRoom.getId(),
                chatRoom.getName(),
                chatRoom.getType(),
                chatRoom.getCreatedAt(),
                lastMessageContent,
                lastMessageAt,
                unreadCount,
                otherUserId,
                otherUserNickname,
                otherUserAvatarUrl
        );
    }
}
