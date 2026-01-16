package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.Message;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface MessageRepository {
    Message save(Message message);
    Optional<Message> findById(Long id);
    List<Message> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, int page, int size);
    long countUnreadMessages(Long chatRoomId, Long userId, LocalDateTime lastReadAt);
    Optional<Message> findTopByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);

    /**
     * 커서 기반 메시지 조회
     * @param chatRoomId 채팅방 ID
     * @param beforeMessageId 이 ID 이전의 메시지 조회 (null이면 최신부터)
     * @param size 조회할 개수
     * @return 메시지 목록 (최신순)
     */
    List<Message> findByChatRoomIdBeforeMessageId(Long chatRoomId, Long beforeMessageId, int size);
}
