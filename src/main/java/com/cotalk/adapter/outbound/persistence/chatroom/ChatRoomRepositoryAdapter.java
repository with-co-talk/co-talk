package com.cotalk.adapter.outbound.persistence.chatroom;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.port.outbound.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 채팅방 영속성 어댑터.
 * JPA를 통해 채팅방 데이터를 저장하고 조회한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class ChatRoomRepositoryAdapter implements ChatRoomRepository {

    private final ChatRoomJpaRepository chatRoomJpaRepository;

    /**
     * 채팅방을 저장한다.
     *
     * @param chatRoom 저장할 채팅방 엔티티
     * @return 저장된 채팅방 엔티티
     */
    @Override
    public ChatRoom save(ChatRoom chatRoom) {
        return chatRoomJpaRepository.save(chatRoom);
    }

    /**
     * ID로 채팅방을 조회한다.
     *
     * @param id 채팅방 ID
     * @return 채팅방 (Optional)
     */
    @Override
    public Optional<ChatRoom> findById(Long id) {
        return chatRoomJpaRepository.findById(id);
    }

    /**
     * 사용자 ID로 참여 중인 채팅방 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 채팅방 목록
     */
    @Override
    public List<ChatRoom> findByUserId(Long userId) {
        return chatRoomJpaRepository.findByUserId(userId);
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
        return chatRoomJpaRepository.findDirectChatRoomByUserIds(userId1, userId2);
    }

    /**
     * 채팅방을 삭제한다.
     *
     * @param chatRoom 삭제할 채팅방 엔티티
     */
    @Override
    public void delete(ChatRoom chatRoom) {
        chatRoomJpaRepository.delete(chatRoom);
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
