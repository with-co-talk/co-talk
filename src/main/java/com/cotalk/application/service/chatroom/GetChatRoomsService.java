package com.cotalk.application.service.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.ChatRoomSummary;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.model.PageQuery;
import com.cotalk.domain.model.PageResult;
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
    private final ChatRoomSummaryAssembler assembler;

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

        // 2-7. 배치 데이터 조회 (6 queries)
        ChatRoomSummaryAssembler.BatchData batchData = assembler.loadBatchData(userId, chatRooms);

        // 8-9. ChatRoomSummary 조립 및 정렬
        return assembler.assembleSummaries(chatRooms, batchData);
    }

    /**
     * 사용자가 참여한 채팅방 목록을 DB 레벨 페이지네이션으로 조회한다.
     * 각 채팅방의 마지막 메시지, 안 읽은 메시지 수, 상대방 정보 등을 포함한다.
     *
     * <p>배치 쿼리를 사용하여 N+1 쿼리 문제를 해결한다.
     *
     * @param userId 사용자 ID
     * @param query  페이지네이션 정보
     * @return 페이지네이션된 채팅방 요약 정보
     */
    @Override
    public PageResult<ChatRoomSummary> getChatRooms(Long userId, PageQuery query) {
        // 1. 채팅방 목록을 페이지네이션하여 조회 (1 query + count query)
        PageResult<ChatRoom> chatRoomPage = chatRoomRepository.findByUserId(userId, query);

        if (chatRoomPage.content().isEmpty()) {
            return new PageResult<>(List.of(), chatRoomPage.page(), chatRoomPage.size(), chatRoomPage.totalElements());
        }

        List<ChatRoom> chatRooms = chatRoomPage.content();

        // 2-7. 배치 데이터 조회 (6 queries)
        ChatRoomSummaryAssembler.BatchData batchData = assembler.loadBatchData(userId, chatRooms);

        // 8. ChatRoomSummary 조립 및 정렬
        List<ChatRoomSummary> summaries = assembler.assembleSummaries(chatRooms, batchData);

        return new PageResult<>(summaries, chatRoomPage.page(), chatRoomPage.size(), chatRoomPage.totalElements());
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

        return assembler.assembleSummary(chatRoom, myMember, lastMessage, unreadCount, otherMember, leftUserId, otherUserMap);
    }
}
