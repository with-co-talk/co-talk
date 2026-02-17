package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.ChatRoomSummary;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 채팅방 요약 정보 조립 서비스.
 * 배치 쿼리를 통해 채팅방 목록에 필요한 모든 데이터를 한 번에 조회하고,
 * ChatRoomSummary 객체를 조립한다.
 *
 * <p>성능 최적화를 위해 N+1 쿼리 문제를 해결하는 배치 조회 로직을 제공한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomSummaryAssembler {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    /**
     * 배치 조회된 데이터를 담는 레코드.
     * 채팅방 목록 조립에 필요한 모든 데이터를 포함한다.
     *
     * @param myMemberMap 내 멤버 정보 맵 (key: 채팅방 ID)
     * @param lastMessageMap 마지막 메시지 맵 (key: 채팅방 ID)
     * @param unreadCountMap 읽지 않은 메시지 수 맵 (key: 채팅방 ID)
     * @param otherMemberMap 상대방 멤버 정보 맵 (key: 채팅방 ID, DIRECT 채팅방만)
     * @param leftUserIdMap 나간 사용자 ID 맵 (key: 채팅방 ID, 상대방이 나간 경우)
     * @param otherUserMap 사용자 정보 맵 (key: 사용자 ID)
     */
    public record BatchData(
            Map<Long, ChatRoomMember> myMemberMap,
            Map<Long, Message> lastMessageMap,
            Map<Long, Long> unreadCountMap,
            Map<Long, ChatRoomMember> otherMemberMap,
            Map<Long, Long> leftUserIdMap,
            Map<Long, User> otherUserMap
    ) {}

    /**
     * 채팅방 목록에 필요한 모든 데이터를 배치로 조회한다.
     * 총 6개의 쿼리로 모든 데이터를 조회하여 N+1 쿼리 문제를 해결한다.
     *
     * @param userId 사용자 ID
     * @param chatRooms 조회할 채팅방 목록
     * @return 배치 조회된 데이터
     */
    public BatchData loadBatchData(Long userId, List<ChatRoom> chatRooms) {
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

        // 6. 상대방이 나간 채팅방의 경우 메시지 기록에서 상대방 ID 복구 (1 query)
        // otherMemberMap에 없는 DIRECT 채팅방은 상대방이 나간 것
        List<Long> leftRoomIds = directChatRoomIds.stream()
                .filter(roomId -> !otherMemberMap.containsKey(roomId))
                .toList();

        Map<Long, Long> leftUserIdMap;
        if (leftRoomIds.isEmpty()) {
            leftUserIdMap = new HashMap<>();
        } else {
            leftUserIdMap = messageRepository.findDistinctSenderIdsByChatRoomIdsExcludingUser(leftRoomIds, userId);
        }

        // 7. 상대방 사용자 정보 배치 조회 (1 query)
        Set<Long> otherUserIds = new HashSet<>();
        otherUserIds.addAll(otherMemberMap.values().stream()
                .map(ChatRoomMember::getUserId)
                .collect(Collectors.toSet()));
        otherUserIds.addAll(leftUserIdMap.values());

        Map<Long, User> otherUserMap = userRepository.findAllById(otherUserIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return new BatchData(myMemberMap, lastMessageMap, unreadCountMap, otherMemberMap, leftUserIdMap, otherUserMap);
    }

    /**
     * 배치 조회된 데이터를 사용하여 ChatRoomSummary 목록을 조립한다.
     * 마지막 메시지 시간 기준 내림차순으로 정렬한다.
     *
     * @param chatRooms 조립할 채팅방 목록
     * @param data 배치 조회된 데이터
     * @return 정렬된 채팅방 요약 정보 목록
     */
    public List<ChatRoomSummary> assembleSummaries(List<ChatRoom> chatRooms, BatchData data) {
        return chatRooms.stream()
                .map(chatRoom -> assembleSummary(
                        chatRoom,
                        data.myMemberMap.get(chatRoom.getId()),
                        data.lastMessageMap.get(chatRoom.getId()),
                        data.unreadCountMap.getOrDefault(chatRoom.getId(), 0L),
                        data.otherMemberMap.get(chatRoom.getId()),
                        data.leftUserIdMap.get(chatRoom.getId()),
                        data.otherUserMap
                ))
                .sorted((a, b) -> {
                    LocalDateTime aTime = a.lastMessageAt() != null ? a.lastMessageAt() : a.createdAt();
                    LocalDateTime bTime = b.lastMessageAt() != null ? b.lastMessageAt() : b.createdAt();
                    if (aTime == null && bTime == null) return 0;
                    if (aTime == null) return 1;
                    if (bTime == null) return -1;
                    return bTime.compareTo(aTime); // 내림차순 (최신이 위)
                })
                .toList();
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
    public ChatRoomSummary assembleSummary(
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

        log.debug("assembleSummary: chatRoomId={}, unreadCount={}, isOtherUserLeft={}, otherUserId={}, isOtherUserOnline={}",
                chatRoom.getId(), unreadCount, isOtherUserLeft, otherUserId, isOtherUserOnline);

        return new ChatRoomSummary(
                chatRoom.getId(),
                chatRoom.getName(),
                chatRoom.getImageUrl(),
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
