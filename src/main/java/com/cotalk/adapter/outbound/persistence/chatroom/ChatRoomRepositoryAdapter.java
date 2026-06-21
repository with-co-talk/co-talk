package com.cotalk.adapter.outbound.persistence.chatroom;

import com.cotalk.adapter.outbound.persistence.entity.ChatRoomJpaEntity;
import com.cotalk.adapter.outbound.persistence.mapper.ChatRoomMapper;
import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.model.PageQuery;
import com.cotalk.domain.model.PageResult;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import com.cotalk.infrastructure.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 채팅방 영속성 어댑터.
 * JPA 엔티티와 도메인 간 매핑을 수행하며, 도메인 포트를 구현한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class ChatRoomRepositoryAdapter implements ChatRoomRepository {

    private final ChatRoomJpaRepository chatRoomJpaRepository;
    private final ChatRoomMapper mapper;

    /**
     * 채팅방을 저장한다.
     * 저장 후 해당 채팅방 캐시를 무효화한다.
     *
     * @param chatRoom 저장할 채팅방 엔티티
     * @return 저장된 채팅방 엔티티
     */
    @CacheEvict(value = CacheConfig.CHAT_ROOM_CACHE, key = "#chatRoom.id", condition = "#chatRoom.id != null")
    @Override
    public ChatRoom save(ChatRoom chatRoom) {
        ChatRoomJpaEntity saved = chatRoomJpaRepository.save(mapper.toJpa(chatRoom));
        return mapper.toDomain(saved);
    }

    /**
     * ID로 채팅방을 조회한다.
     * 결과는 캐시에 저장되어 반복 조회 시 DB 접근을 줄인다.
     *
     * @param id 채팅방 ID
     * @return 채팅방 (Optional)
     */
    @Cacheable(value = CacheConfig.CHAT_ROOM_CACHE, key = "#id")
    @Override
    public Optional<ChatRoom> findById(Long id) {
        return chatRoomJpaRepository.findById(id).map(mapper::toDomain);
    }

    /**
     * 사용자 ID로 참여 중인 채팅방 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 채팅방 목록
     */
    @Override
    public List<ChatRoom> findByUserId(Long userId) {
        return chatRoomJpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * 사용자 ID로 참여 중인 채팅방 목록을 페이지네이션하여 조회한다.
     *
     * @param userId 사용자 ID
     * @param query  페이지네이션 정보
     * @return 페이지네이션된 채팅방 목록
     */
    @Override
    public PageResult<ChatRoom> findByUserId(Long userId, PageQuery query) {
        Pageable pageable = PageRequest.of(query.page(), query.size());
        Page<ChatRoom> page = chatRoomJpaRepository.findByUserId(userId, pageable).map(mapper::toDomain);
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    /**
     * 두 사용자 간의 1:1 채팅방을 조회한다.
     *
     * @param userId1 첫 번째 사용자 ID
     * @param userId2 두 번째 사용자 ID
     * @return 1:1 채팅방 (Optional)
     */
    @Override
    public Optional<ChatRoom> findDirectChatRoomByUserIds(Long userId1, Long userId2) {
        return chatRoomJpaRepository.findDirectChatRoomByUserIds(userId1, userId2).map(mapper::toDomain);
    }

    /**
     * 사용자의 나와의 채팅방(SELF)을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 나와의 채팅방 (Optional)
     */
    @Override
    public Optional<ChatRoom> findSelfChatRoomByUserId(Long userId) {
        return chatRoomJpaRepository.findSelfChatRoomByUserId(userId).map(mapper::toDomain);
    }

    /**
     * 채팅방을 삭제한다.
     * 삭제 시 해당 채팅방 캐시를 무효화한다.
     *
     * @param chatRoom 삭제할 채팅방 엔티티
     */
    @CacheEvict(value = CacheConfig.CHAT_ROOM_CACHE, key = "#chatRoom.id")
    @Override
    public void delete(ChatRoom chatRoom) {
        chatRoomJpaRepository.delete(mapper.toJpa(chatRoom));
    }

    /**
     * 전체 채팅방 수를 조회한다.
     *
     * @return 채팅방 수
     */
    @Override
    public long count() {
        return chatRoomJpaRepository.count();
    }
}
