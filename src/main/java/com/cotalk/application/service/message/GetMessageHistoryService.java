package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.message.GetMessageHistoryUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 메시지 히스토리 조회 유스케이스 구현체.
 * 채팅방의 메시지 히스토리를 조회한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMessageHistoryService implements GetMessageHistoryUseCase {

    private final MessageRepository messageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final ChatRoomMemberValidator chatRoomMemberValidator;

    /**
     * 채팅방의 메시지 히스토리를 조회한다.
     * 특정 메시지 이전의 메시지들을 조회하며, 채팅방 멤버만 조회할 수 있다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 요청 사용자 ID
     * @param beforeMessageId 기준 메시지 ID (이 메시지 이전의 메시지를 조회)
     * @param size 조회할 메시지 개수
     * @return 메시지 목록
     * @throws com.cotalk.domain.exception.ChatRoomAccessDeniedException 채팅방 멤버가 아닌 경우
     */
    @Override
    public List<Message> getMessageHistory(Long chatRoomId, Long userId, Long beforeMessageId, int size) {
        chatRoomMemberValidator.validateMembership(chatRoomId, userId);

        return messageRepository.findByChatRoomIdBeforeMessageId(chatRoomId, beforeMessageId, size);
    }

    /**
     * 채팅방의 메시지 히스토리를 조회하고, 읽지 않은 멤버 수와 발신자 정보를 포함한 결과를 반환한다.
     * 배치 쿼리를 사용하여 N+1 쿼리를 방지한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 요청 사용자 ID
     * @param beforeMessageId 기준 메시지 ID
     * @param size 조회할 메시지 개수
     * @return 메시지 히스토리 조회 결과 (메시지, 읽지 않은 수, 발신자 정보 포함)
     * @throws com.cotalk.domain.exception.ChatRoomAccessDeniedException 채팅방 멤버가 아닌 경우
     */
    @Override
    public EnrichedMessageHistoryResult getEnrichedMessageHistory(Long chatRoomId, Long userId,
                                                                    Long beforeMessageId, int size) {
        chatRoomMemberValidator.validateMembership(chatRoomId, userId);

        List<Message> messages = messageRepository.findByChatRoomIdBeforeMessageId(chatRoomId, beforeMessageId, size);

        // 배치 쿼리로 모든 메시지의 unreadCount를 한 번에 조회 (N+1 쿼리 방지)
        List<Long> messageIds = messages.stream().map(Message::getId).toList();
        Map<Long, Integer> unreadCountMap = chatRoomMemberRepository.batchCountUnreadMembersByMessageIds(
                chatRoomId, messageIds);

        // 배치 쿼리로 모든 발신자의 정보를 한 번에 조회 (N+1 쿼리 방지)
        Set<Long> senderIds = messages.stream().map(Message::getSenderId).collect(Collectors.toSet());
        Map<Long, User> senderMap = userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<EnrichedMessage> enrichedMessages = messages.stream()
                .map(message -> {
                    int unreadCount = unreadCountMap.getOrDefault(message.getId(), 0);
                    User sender = senderMap.get(message.getSenderId());
                    String senderNickname = sender != null ? sender.getNickname() : null;
                    String senderAvatarUrl = sender != null ? sender.getAvatarUrl() : null;
                    return new EnrichedMessage(message, unreadCount, senderNickname, senderAvatarUrl);
                })
                .toList();

        Long nextCursor = messages.isEmpty() ? null : messages.get(messages.size() - 1).getId();
        boolean hasMore = messages.size() == size;

        return new EnrichedMessageHistoryResult(enrichedMessages, nextCursor, hasMore);
    }
}
