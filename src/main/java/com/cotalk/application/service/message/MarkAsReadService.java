package com.cotalk.application.service.message;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.message.MarkAsReadUseCase;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserEventBroker;
import com.cotalk.domain.port.outbound.UserEventBroker.ChatListUpdateEvent;
import com.cotalk.domain.port.outbound.UserEventBroker.ReadReceiptEvent;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 메시지 읽음 처리 유스케이스 구현체.
 * 채팅방의 메시지를 읽음 처리한다.
 *
 * <p>동시성 제어:
 * <ul>
 *   <li>원자적 UPDATE 쿼리로 Lost Update 방지</li>
 *   <li>기존 시간보다 새로운 시간인 경우에만 업데이트</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MarkAsReadService implements MarkAsReadUseCase {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomMemberValidator chatRoomMemberValidator;
    private final UserEventBroker userEventBroker;
    private final ChatMessageBroker chatMessageBroker;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    /**
     * 채팅방의 메시지를 읽음 처리한다.
     * 현재 시간을 마지막 읽은 시간으로 업데이트한다.
     *
     * <p>동시성 처리:
     * <ul>
     *   <li>원자적 UPDATE 쿼리 사용 (기존 시간보다 큰 경우에만 업데이트)</li>
     *   <li>여러 스레드가 동시에 호출해도 항상 최신 시간이 유지됨</li>
     * </ul>
     *
     * @param userId     사용자 ID
     * @param chatRoomId 채팅방 ID
     * @throws com.cotalk.domain.exception.ChatRoomAccessDeniedException 채팅방 멤버가 아닌 경우
     */
    @Override
    public void markAsRead(Long userId, Long chatRoomId) {
        // 멤버 검증
        chatRoomMemberValidator.getMemberOrThrow(chatRoomId, userId);

        // 마지막 메시지 ID(= 읽음 기준점). 메시지가 없으면 lastReadMessageId는 null이다.
        Long lastReadMessageId = messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId)
                .map(Message::getId)
                .orElse(null);

        // 원자적 업데이트 (기존 메시지 ID보다 큰 경우에만)
        LocalDateTime now = LocalDateTime.now();
        int updated;
        if (lastReadMessageId == null) {
            // 메시지가 없으면 시간만 갱신(기존 로직 유지)
            updated = chatRoomMemberRepository.updateLastReadAtIfNewer(chatRoomId, userId, now);
        } else {
            updated = chatRoomMemberRepository.updateLastReadMessageIdIfNewer(chatRoomId, userId, now, lastReadMessageId);
        }

        if (updated > 0) {
            log.info("Marked as read: userId={}, chatRoomId={}, lastReadAt={}", userId, chatRoomId, now);
        } else {
            log.debug("No DB update occurred: userId={}, chatRoomId={} (lastReadAt already newer or same)", userId, chatRoomId);
        }

        // 클라이언트 동기화를 위해 항상 READ 이벤트 발행
        // (updated == 0이어도 클라이언트가 정확한 unreadCount를 받아야 함)
        // 다른 채팅방 멤버들에게 읽음 이벤트 브로드캐스트
        publishReadReceiptToMembers(chatRoomId, userId, now, lastReadMessageId);

        // 채팅방 토픽용 READ 이벤트 발행 (카톡/라인 스타일: 방 화면은 방 구독만으로 읽음 반영 가능)
        publishRoomReadEvent(chatRoomId, userId, now, lastReadMessageId);

        // 채팅 목록 업데이트 이벤트 브로드캐스트 (unreadCount 재계산)
        // 읽은 사용자의 현재 lastReadMessageId를 기준으로 정확한 unreadCount 계산
        // updated == 0이어도 현재 DB의 lastReadMessageId를 기준으로 정확한 unreadCount 계산하여 전송
        // (updated == 0이면 DB에서 현재 값을 조회하여 사용)
        Long currentLastReadMessageId = lastReadMessageId;
        if (updated == 0) {
            // DB 업데이트가 없었으면 현재 DB의 lastReadMessageId를 조회
            currentLastReadMessageId = chatRoomMemberRepository
                    .findByChatRoomIdAndUserId(chatRoomId, userId)
                    .map(ChatRoomMember::getLastReadMessageId)
                    .orElse(lastReadMessageId); // 조회 실패 시 계산한 값 사용
        }
        log.info("Publishing chat list update after markAsRead: chatRoomId={}, userId={}, updated={}, lastReadMessageId={}", 
                chatRoomId, userId, updated, currentLastReadMessageId);
        publishChatListUpdate(chatRoomId, userId, now, currentLastReadMessageId);
    }

    /**
     * 채팅방 토픽에 READ 이벤트를 발행한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param readerId   읽은 사용자 ID
     * @param lastReadAt 읽은 시간
     */
    private void publishRoomReadEvent(Long chatRoomId, Long readerId, LocalDateTime lastReadAt, Long lastReadMessageId) {
        chatMessageBroker.publishRoomEvent(
                chatRoomId,
                new RoomReadEvent(
                        1,
                        "room-event:READ:" + chatRoomId + ":" + readerId + ":" + lastReadMessageId,
                        "READ",
                        chatRoomId,
                        readerId,
                        lastReadMessageId,
                        lastReadAt
                )
        );
    }

    /**
     * 채팅방 READ 이벤트 DTO.
     * Redis Pub/Sub -> WebSocket 방 토픽(/topic/chat/room/{roomId})으로 전달되는 이벤트다.
     */
    private record RoomReadEvent(
            Integer schemaVersion,
            String eventId,
            String eventType,
            Long chatRoomId,
            Long userId,
            Long lastReadMessageId,
            LocalDateTime lastReadAt
    ) {}

    /**
     * 읽음 이벤트를 채팅방의 모든 멤버들에게 브로드캐스트한다.
     * 읽은 사용자 본인에게도 이벤트를 전송하여 클라이언트가 읽음 완료를 확인할 수 있도록 한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param readerId   읽은 사용자 ID
     * @param lastReadAt 읽은 시간
     */
    private void publishReadReceiptToMembers(Long chatRoomId, Long readerId, LocalDateTime lastReadAt, Long lastReadMessageId) {
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(chatRoomId);

        ReadReceiptEvent event = new ReadReceiptEvent(
                1,
                "read-receipt:" + chatRoomId + ":" + readerId + ":" + lastReadMessageId,
                chatRoomId,
                readerId,
                lastReadMessageId,
                lastReadAt
        );

        // 모든 멤버에게 이벤트 전송 (읽은 사용자 본인 포함)
        for (ChatRoomMember member : members) {
            userEventBroker.publishReadReceipt(member.getUserId(), event);
            log.debug("Published read receipt to user {}: {}", member.getUserId(), event);
        }
    }

    /**
     * 채팅 목록 업데이트 이벤트를 채팅방 참여자들에게 브로드캐스트한다.
     * 읽음 처리 후 각 멤버의 unreadCount를 재계산하여 전송한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param readerId   읽은 사용자 ID (업데이트된 lastReadAt 사용)
     * @param lastReadAt 읽은 사용자의 업데이트된 마지막 읽은 시간
     */
    private void publishChatListUpdate(Long chatRoomId, Long readerId, LocalDateTime lastReadAt, Long readerLastReadMessageId) {
        // 마지막 메시지 조회
        Message lastMessage = messageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId)
                .orElse(null);

        if (lastMessage == null) {
            // 메시지가 없는 경우 업데이트하지 않음
            log.info("No message found in chat room {}, skipping chat list update", chatRoomId);
            return;
        }

        log.info("Found last message in chat room {}: messageId={}, content={}", 
                chatRoomId, lastMessage.getId(), lastMessage.getContent());

        // 발신자 닉네임 조회
        String senderNickname = userRepository.findById(lastMessage.getSenderId())
                .map(User::getNickname)
                .orElse("알 수 없음");

        // 채팅방 참여자 목록 조회
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(chatRoomId);
        log.info("Found {} members in chat room {} for chat list update", members.size(), chatRoomId);

        if (members.isEmpty()) {
            log.warn("No members found in chat room {}, skipping chat list update", chatRoomId);
            return;
        }

        for (ChatRoomMember member : members) {
            // 해당 멤버의 읽지 않은 메시지 수 계산
            // readerId의 경우 방금 업데이트한(또는 조회한) readerLastReadMessageId를 사용
            // 다른 멤버의 경우 DB에 저장된 member.getLastReadMessageId()를 사용
            Long memberLastReadMessageId = member.getUserId().equals(readerId)
                    ? readerLastReadMessageId
                    : member.getLastReadMessageId();
            
            log.debug("Calculating unreadCount for member {}: lastReadMessageId={}, readerId={}, readerLastReadMessageId={}", 
                    member.getUserId(), memberLastReadMessageId, readerId, readerLastReadMessageId);
            
            int memberUnreadCount = (int) messageRepository.countUnreadMessagesByLastReadMessageId(
                    chatRoomId,
                    member.getUserId(),
                    memberLastReadMessageId
            );
            
            log.debug("Calculated unreadCount for member {}: unreadCount={}", member.getUserId(), memberUnreadCount);

            ChatListUpdateEvent event = new ChatListUpdateEvent(
                    1,
                    "chat-list:READ:" + chatRoomId + ":" + member.getUserId() + ":" + lastMessage.getId(),
                    "READ",
                    chatRoomId,
                    lastMessage.getContent(),
                    lastMessage.getType().name(),
                    lastMessage.getCreatedAt(),
                    lastMessage.getSenderId(),
                    senderNickname,
                    memberUnreadCount
            );

            userEventBroker.publishChatListUpdate(member.getUserId(), event);
            log.info("Published chat list update to user {}: eventType={}, roomId={}, unreadCount={}", 
                    member.getUserId(), event.eventType(), event.roomId(), event.unreadCount());
        }
    }
}
