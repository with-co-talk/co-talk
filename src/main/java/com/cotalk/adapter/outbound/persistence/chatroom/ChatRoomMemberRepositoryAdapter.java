package com.cotalk.adapter.outbound.persistence.chatroom;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 채팅방 멤버 영속성 어댑터.
 * JPA를 통해 채팅방 멤버 데이터를 저장하고 조회한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class ChatRoomMemberRepositoryAdapter implements ChatRoomMemberRepository {

    private final ChatRoomMemberJpaRepository chatRoomMemberJpaRepository;

    /**
     * 채팅방 멤버를 저장한다.
     *
     * @param member 저장할 채팅방 멤버 엔티티
     * @return 저장된 채팅방 멤버 엔티티
     */
    @Override
    public ChatRoomMember save(ChatRoomMember member) {
        return chatRoomMemberJpaRepository.save(member);
    }

    /**
     * 여러 채팅방 멤버를 일괄 저장한다.
     *
     * @param members 저장할 채팅방 멤버 목록
     * @return 저장된 채팅방 멤버 목록
     */
    @Override
    public List<ChatRoomMember> saveAll(List<ChatRoomMember> members) {
        return chatRoomMemberJpaRepository.saveAll(members);
    }

    /**
     * 채팅방 ID와 사용자 ID로 채팅방 멤버를 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 채팅방 멤버 (Optional)
     */
    @Override
    public Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long chatRoomId, Long userId) {
        return chatRoomMemberJpaRepository.findByChatRoomIdAndUserId(chatRoomId, userId);
    }

    /**
     * 채팅방 ID로 모든 멤버 목록을 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @return 채팅방 멤버 목록
     */
    @Override
    public List<ChatRoomMember> findByChatRoomId(Long chatRoomId) {
        return chatRoomMemberJpaRepository.findByChatRoomId(chatRoomId);
    }

    /**
     * 사용자 ID로 참여 중인 채팅방 멤버 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 채팅방 멤버 목록
     */
    @Override
    public List<ChatRoomMember> findByUserId(Long userId) {
        return chatRoomMemberJpaRepository.findByUserId(userId);
    }

    /**
     * 채팅방에 해당 사용자가 멤버로 존재하는지 확인한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 멤버 존재 여부
     */
    @Override
    public boolean existsByChatRoomIdAndUserId(Long chatRoomId, Long userId) {
        return chatRoomMemberJpaRepository.existsByChatRoomIdAndUserId(chatRoomId, userId);
    }

    /**
     * 채팅방 멤버를 삭제한다.
     *
     * @param member 삭제할 채팅방 멤버 엔티티
     */
    @Override
    public void delete(ChatRoomMember member) {
        chatRoomMemberJpaRepository.delete(member);
    }

    /**
     * 사용자 ID로 모든 채팅방 멤버 정보를 삭제한다.
     *
     * @param userId 사용자 ID
     */
    @Override
    public void deleteByUserId(Long userId) {
        chatRoomMemberJpaRepository.deleteByUserId(userId);
    }

    /**
     * 마지막 읽은 시간을 원자적으로 업데이트한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId     사용자 ID
     * @param lastReadAt 새로운 읽은 시간
     * @return 업데이트된 행 수
     */
    @Override
    public int updateLastReadAtIfNewer(Long chatRoomId, Long userId, LocalDateTime lastReadAt) {
        return chatRoomMemberJpaRepository.updateLastReadAtIfNewer(chatRoomId, userId, lastReadAt);
    }

    /**
     * 특정 메시지를 읽지 않은 멤버 수를 조회한다.
     *
     * @param chatRoomId       채팅방 ID
     * @param messageCreatedAt 메시지 생성 시간
     * @param senderId         발신자 ID (제외할 사용자)
     * @return 읽지 않은 멤버 수
     */
    @Override
    public int countUnreadMembers(Long chatRoomId, LocalDateTime messageCreatedAt, Long senderId) {
        return chatRoomMemberJpaRepository.countUnreadMembers(chatRoomId, messageCreatedAt, senderId);
    }
}
