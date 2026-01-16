package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.outbound.persistence.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 메시지 Repository 어댑터.
 * 
 * <p>도메인 Repository 인터페이스를 JPA Repository로 구현합니다.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
public class MessageRepositoryAdapter implements MessageRepository {

    private final MessageJpaRepository messageJpaRepository;

    @Override
    public Message save(Message message) {
        return messageJpaRepository.save(message);
    }

    @Override
    public Optional<Message> findById(UUID id) {
        return messageJpaRepository.findById(id);
    }

    @Override
    public List<Message> findByChatRoomIdOrderByCreatedAtDesc(UUID chatRoomId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageJpaRepository.findByChatRoomIdOrderByCreatedAtDesc(chatRoomId, pageable);
    }

    @Override
    public List<Message> findByChatRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            UUID chatRoomId, LocalDateTime before, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageJpaRepository.findByChatRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                chatRoomId, before, pageable);
    }

    @Override
    public long countUnreadMessages(UUID chatRoomId, UUID userId, LocalDateTime lastReadAt) {
        return messageJpaRepository.countUnreadMessages(chatRoomId, userId, lastReadAt);
    }
}
