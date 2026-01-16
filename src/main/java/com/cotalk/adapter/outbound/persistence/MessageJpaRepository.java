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
    @Query("SELECT m FROM Message m WHERE m.chatRoomId = :chatRoomId AND m.deleted = false ORDER BY m.createdAt DESC")
    List<Message> findByChatRoomIdOrderByCreatedAtDesc(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoomId = :chatRoomId AND m.createdAt > :lastReadAt AND m.deleted = false")
    long countUnreadMessages(@Param("chatRoomId") Long chatRoomId, @Param("lastReadAt") LocalDateTime lastReadAt);

    @Query("SELECT m FROM Message m WHERE m.chatRoomId = :chatRoomId AND m.deleted = false ORDER BY m.createdAt DESC")
    Optional<Message> findTopByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);

    @Query("SELECT m FROM Message m WHERE m.chatRoomId = :chatRoomId AND m.id < :beforeMessageId AND m.deleted = false ORDER BY m.id DESC")
    List<Message> findByChatRoomIdAndIdLessThan(
            @Param("chatRoomId") Long chatRoomId,
            @Param("beforeMessageId") Long beforeMessageId,
            Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.chatRoomId = :chatRoomId AND m.deleted = false ORDER BY m.id DESC")
    List<Message> findByChatRoomIdOrderByIdDesc(
            @Param("chatRoomId") Long chatRoomId,
            Pageable pageable);
}
