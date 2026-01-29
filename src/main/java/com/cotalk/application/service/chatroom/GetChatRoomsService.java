package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.ChatRoomSummary;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.exception.ChatRoomNotFoundException;
import com.cotalk.domain.port.inbound.chatroom.GetChatRoomUseCase;
import com.cotalk.domain.port.inbound.chatroom.GetChatRoomsUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 채팅방 목록 조회 유스케이스 구현체.
 * 사용자가 참여한 채팅방 목록을 조회한다.
 *
 * <p>성능 최적화:
 * - 배치 쿼리를 사용하여 N+1 쿼리 문제를 해결
 * - 채팅방 N개 조회 시 기존: 4N+1 쿼리 → 최적화 후: 6개 쿼리
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GetChatRoomsService implements GetChatRoomsUseCase, GetChatRoomUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    /**
     * 사용자가 참여한 채팅방 목록을 조회한다.
     * 각 채팅방의 마지막 메시지, 안 읽은 메시지 수, 상대방 정보 등을 포함한다.
     *
     * <p>배치 쿼리를 사용하여 N+1 쿼리 문제를 해결한다.
     *
     * @param userId 사용자 ID
     * @return 채팅방 요약 정보 목록
     */
    @Override
    public List<ChatRoomSummary> getChatRooms(Long userId) {
        // 1. 채팅방 목록 조회 (1 query)
        List<ChatRoom> chatRooms = chatRoomRepository.findByUserId(userId);

        if (chatRooms.isEmpty()) {
            return List.of();
        }

        List<Long> chatRoomIds = chatRooms.stream()
                .map(ChatRoom::getId)
                .toList();

        // 2. 내 멤버 정보 배치 조회 (1 query)
        Map<Long, ChatRoomMember> myMemberMap = chatRoomMemberRepository
                .findByUserIdAndChatRoomIds(userId, chatRoomIds)
                .stream()
                .collect(Collectors.toMap(ChatRoomMember::getChatRoomId, Function.identity()));

        // 3. 마지막 메시지 배치 조회 (1 query)
        Map<Long, Message> lastMessageMap = messageRepository
                .findLastMessagesByRoomIds(chatRoomIds)
                .stream()
                .collect(Collectors.toMap(Message::getChatRoomId, Function.identity()));

        // 4. 읽지 않은 메시지 수 배치 조회 (1 query)
        Map<Long, Long> unreadCountMap = messageRepository.batchCountUnreadMessages(userId, chatRoomIds);

        // 5. DIRECT 채팅방의 상대방 멤버 정보 배치 조회 (1 query)
        List<Long> directChatRoomIds = chatRooms.stream()
                .filter(room -> room.getType() == ChatRoom.ChatRoomType.DIRECT)
                .map(ChatRoom::getId)
                .toList();

        Map<Long, ChatRoomMember> otherMemberMap = chatRoomMemberRepository
                .findOtherMembersByChatRoomIds(userId, directChatRoomIds)
                .stream()
                .collect(Collectors.toMap(ChatRoomMember::getChatRoomId, Function.identity(), (a, b) -> a));

        // 6. 상대방이 나간 채팅방의 경우 메시지 기록에서 상대방 ID 복구
        // otherMemberMap에 없는 DIRECT 채팅방은 상대방이 나간 것
        Map<Long, Long> leftUserIdMap = new java.util.HashMap<>();
        for (Long directRoomId : directChatRoomIds) {
            if (!otherMemberMap.containsKey(directRoomId)) {
                // 상대방이 나간 채팅방 - 메시지 기록에서 상대방 ID 찾기
                List<Long> senderIds = messageRepository.findDistinctSenderIdsByChatRoomIdExcludingUser(directRoomId, userId);
                if (!senderIds.isEmpty()) {
                    leftUserIdMap.put(directRoomId, senderIds.get(0));
                }
            }
        }

        // 7. 상대방 사용자 정보 배치 조회 (1 query)
        Set<Long> otherUserIds = new java.util.HashSet<>();
        otherUserIds.addAll(otherMemberMap.values().stream()
                .map(ChatRoomMember::getUserId)
                .collect(Collectors.toSet()));
        otherUserIds.addAll(leftUserIdMap.values());

        Map<Long, User> otherUserMap = userRepository.findAllById(otherUserIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 8. ChatRoomSummary 조립 (no query)
        return chatRooms.stream()
                .map(chatRoom -> buildChatRoomSummary(
                        chatRoom,
                        myMemberMap.get(chatRoom.getId()),
                        lastMessageMap.get(chatRoom.getId()),
                        unreadCountMap.getOrDefault(chatRoom.getId(), 0L),
                        otherMemberMap.get(chatRoom.getId()),
                        leftUserIdMap.get(chatRoom.getId()),
                        otherUserMap
                ))
                .toList();
    }

    /**
     * 특정 채팅방의 상세 정보를 조회한다.
     *
     * @param roomId 채팅방 ID
     * @param userId 조회하는 사용자 ID
     * @return 채팅방 요약 정보
     * @throws ChatRoomNotFoundException 채팅방이 존재하지 않는 경우
     * @throws ChatRoomAccessDeniedException 해당 채팅방의 멤버가 아닌 경우
     */
    @Override
    public ChatRoomSummary getChatRoom(Long roomId, Long userId) {
        // 1. 채팅방 존재 확인
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(roomId));

        // 2. 사용자가 채팅방 멤버인지 확인
        ChatRoomMember myMember = chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ChatRoomAccessDeniedException(roomId, userId));

        // 3. 마지막 메시지 조회
        Message lastMessage = messageRepository.findLastMessagesByRoomIds(List.of(roomId))
                .stream()
                .findFirst()
                .orElse(null);

        // 4. 읽지 않은 메시지 수 조회
        Map<Long, Long> unreadCountMap = messageRepository.batchCountUnreadMessages(userId, List.of(roomId));
        long unreadCount = unreadCountMap.getOrDefault(roomId, 0L);

        // 5. 상대방 정보 조회 (1:1 채팅인 경우)
        ChatRoomMember otherMember = null;
        Long leftUserId = null;
        Map<Long, User> otherUserMap = Map.of();

        if (chatRoom.getType() == ChatRoom.ChatRoomType.DIRECT) {
            List<ChatRoomMember> otherMembers = chatRoomMemberRepository
                    .findOtherMembersByChatRoomIds(userId, List.of(roomId));
            otherMember = otherMembers.isEmpty() ? null : otherMembers.get(0);

            if (otherMember != null) {
                otherUserMap = userRepository.findAllById(Set.of(otherMember.getUserId()))
                        .stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));
            } else {
                // 상대방이 나간 경우 메시지 기록에서 상대방 ID 복구
                List<Long> senderIds = messageRepository.findDistinctSenderIdsByChatRoomIdExcludingUser(roomId, userId);
                if (!senderIds.isEmpty()) {
                    leftUserId = senderIds.get(0);
                    otherUserMap = userRepository.findAllById(Set.of(leftUserId))
                            .stream()
                            .collect(Collectors.toMap(User::getId, Function.identity()));
                }
            }
        }

        return buildChatRoomSummary(chatRoom, myMember, lastMessage, unreadCount, otherMember, leftUserId, otherUserMap);
    }

    /**
     * 배치 조회된 데이터를 사용하여 ChatRoomSummary를 조립한다.
     *
     * @param chatRoom 채팅방
     * @param myMember 내 멤버 정보
     * @param lastMessage 마지막 메시지
     * @param unreadCount 읽지 않은 메시지 수
     * @param otherMember 상대방 멤버 정보 (나간 경우 null)
     * @param leftUserId 나간 사용자 ID (메시지 기록에서 복구, 없으면 null)
     * @param otherUserMap 사용자 정보 맵
     * @return 채팅방 요약 정보
     */
    private ChatRoomSummary buildChatRoomSummary(
            ChatRoom chatRoom,
            ChatRoomMember myMember,
            Message lastMessage,
            long unreadCount,
            ChatRoomMember otherMember,
            Long leftUserId,
            Map<Long, User> otherUserMap
    ) {
        // 마지막 메시지 정보
        String lastMessageContent = lastMessage != null ? lastMessage.getContent() : "";
        String lastMessageType = lastMessage != null ? lastMessage.getType().name() : null;
        LocalDateTime lastMessageCreatedAt = lastMessage != null ? lastMessage.getCreatedAt() : null;

        // 상대방 정보 (1:1 채팅인 경우)
        Long otherUserId = null;
        String otherNickname = null;
        String otherAvatarUrl = null;
        boolean isOtherUserLeft = false;
        boolean isOtherUserOnline = false;
        LocalDateTime otherUserLastActiveAt = null;

        if (chatRoom.getType() == ChatRoom.ChatRoomType.DIRECT) {
            // 1:1 채팅방에서 상대방이 나갔는지 확인
            // otherMember가 null이면 상대방이 채팅방을 나간 것
            isOtherUserLeft = (otherMember == null);

            if (otherMember != null) {
                User otherUser = otherUserMap.get(otherMember.getUserId());
                if (otherUser != null) {
                    otherUserId = otherUser.getId();
                    otherNickname = otherUser.getNickname();
                    otherAvatarUrl = otherUser.getAvatarUrl();
                    isOtherUserOnline = otherUser.isOnline();
                    otherUserLastActiveAt = otherUser.getLastActiveAt();
                }
            } else if (leftUserId != null) {
                // 상대방이 나갔지만 메시지 기록에서 ID를 복구한 경우
                User leftUser = otherUserMap.get(leftUserId);
                if (leftUser != null) {
                    otherUserId = leftUser.getId();
                    otherNickname = leftUser.getNickname();
                    otherAvatarUrl = leftUser.getAvatarUrl();
                    // 나간 사용자는 오프라인 처리
                    isOtherUserOnline = false;
                    otherUserLastActiveAt = leftUser.getLastActiveAt();
                }
            }
        }

        log.debug("buildChatRoomSummary: chatRoomId={}, unreadCount={}, isOtherUserLeft={}, otherUserId={}, isOtherUserOnline={}",
                chatRoom.getId(), unreadCount, isOtherUserLeft, otherUserId, isOtherUserOnline);

        return new ChatRoomSummary(
                chatRoom.getId(),
                chatRoom.getName(),
                chatRoom.getType(),
                chatRoom.getCreatedAt(),
                lastMessageContent,
                lastMessageType,
                lastMessageCreatedAt,
                unreadCount,
                otherUserId,
                otherNickname,
                otherAvatarUrl,
                isOtherUserLeft,
                isOtherUserOnline,
                otherUserLastActiveAt
        );
    }
}
