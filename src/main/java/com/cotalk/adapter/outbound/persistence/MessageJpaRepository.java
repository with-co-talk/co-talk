package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageJpaRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoomId = :chatRoomId AND m.createdAt > :lastReadAt")
    long countUnreadMessages(@Param("chatRoomId") Long chatRoomId, @Param("lastReadAt") LocalDateTime lastReadAt);

    Optional<Message> findTopByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);

    @Query("SELECT m FROM Message m WHERE m.chatRoomId = :chatRoomId AND m.id < :beforeMessageId ORDER BY m.id DESC")
    List<Message> findByChatRoomIdAndIdLessThan(
            @Param("chatRoomId") Long chatRoomId,
            @Param("beforeMessageId") Long beforeMessageId,
            Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.chatRoomId = :chatRoomId ORDER BY m.id DESC")
    List<Message> findByChatRoomIdOrderByIdDesc(
            @Param("chatRoomId") Long chatRoomId,
            Pageable pageable);
}
