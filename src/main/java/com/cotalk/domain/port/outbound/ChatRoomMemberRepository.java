package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.ChatRoomMember;

import java.util.List;
import java.util.Optional;

/**
 * 채팅방 멤버 레포지토리 포트.
 * 채팅방 참여자 데이터 저장 및 조회를 위한 인터페이스를 정의한다.
 *
 * @author seunggu.lee
 */
public interface ChatRoomMemberRepository {

    /**
     * 채팅방 멤버를 저장한다.
     *
     * @param member 저장할 채팅방 멤버
     * @return 저장된 채팅방 멤버
     */
    ChatRoomMember save(ChatRoomMember member);

    /**
     * 채팅방 ID와 사용자 ID로 채팅방 멤버를 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId     사용자 ID
     * @return 조회된 채팅방 멤버 (Optional)
     */
    Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    /**
     * 특정 채팅방의 모든 멤버를 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @return 채팅방 멤버 목록
     */
    List<ChatRoomMember> findByChatRoomId(Long chatRoomId);

    /**
     * 특정 사용자가 참여한 채팅방 멤버 정보를 조회한다.
     *
     * @param userId 사용자 ID
     * @return 채팅방 멤버 목록
     */
    List<ChatRoomMember> findByUserId(Long userId);

    /**
     * 특정 채팅방에 사용자가 참여 중인지 확인한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId     사용자 ID
     * @return 참여 여부
     */
    boolean existsByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    /**
     * 채팅방 멤버를 삭제한다.
     *
     * @param member 삭제할 채팅방 멤버
     */
    void delete(ChatRoomMember member);

    /**
     * 특정 사용자의 모든 채팅방 멤버 정보를 삭제한다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);
}
