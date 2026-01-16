package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.outbound.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MessageRepositoryAdapter implements MessageRepository {

    private final MessageJpaRepository messageJpaRepository;

    @Override
    public Message save(Message message) {
        return messageJpaRepository.save(message);
    }

    @Override
    public Optional<Message> findById(Long id) {
        return messageJpaRepository.findById(id);
    }

    @Override
    public List<Message> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, int page, int size) {
        return messageJpaRepository.findByChatRoomIdOrderByCreatedAtDesc(chatRoomId, PageRequest.of(page, size));
    }

    @Override
    public long countUnreadMessages(Long chatRoomId, Long userId, LocalDateTime lastReadAt) {
        if (lastReadAt == null) {
            return 0;
        }
        return messageJpaRepository.countUnreadMessages(chatRoomId, lastReadAt);
    }

    @Override
    public Optional<Message> findTopByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId) {
        return messageJpaRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoomId);
    }

    @Override
    public List<Message> findByChatRoomIdBeforeMessageId(Long chatRoomId, Long beforeMessageId, int size) {
        if (beforeMessageId == null) {
            return messageJpaRepository.findByChatRoomIdOrderByIdDesc(chatRoomId, PageRequest.of(0, size));
        }
        return messageJpaRepository.findByChatRoomIdAndIdLessThan(chatRoomId, beforeMessageId, PageRequest.of(0, size));
    }
}
