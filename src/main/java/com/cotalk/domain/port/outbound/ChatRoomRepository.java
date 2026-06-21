package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.ChatRoom;
import com.cotalk.domain.model.PageQuery;
import com.cotalk.domain.model.PageResult;

import java.util.List;
import java.util.Optional;

/**
 * 채팅방 레포지토리 포트.
 * 채팅방 데이터 저장 및 조회를 위한 인터페이스를 정의한다.
 *
 * @author seunggu.lee
 */
public interface ChatRoomRepository {

    /**
     * 채팅방을 저장한다.
     *
     * @param chatRoom 저장할 채팅방
     * @return 저장된 채팅방
     */
    ChatRoom save(ChatRoom chatRoom);

    /**
     * ID로 채팅방을 조회한다.
     *
     * @param id 채팅방 ID
     * @return 조회된 채팅방 (Optional)
     */
    Optional<ChatRoom> findById(Long id);

    /**
     * 사용자가 참여한 채팅방 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 사용자가 참여한 채팅방 목록
     */
    List<ChatRoom> findByUserId(Long userId);

    /**
     * 사용자가 참여한 채팅방 목록을 DB 레벨 페이지네이션으로 조회한다.
     *
     * @param userId 사용자 ID
     * @param query  페이지네이션 정보
     * @return 페이지네이션된 채팅방 목록
     */
    PageResult<ChatRoom> findByUserId(Long userId, PageQuery query);

    /**
     * 두 사용자 간의 1:1 채팅방을 조회한다.
     *
     * @param userId1 첫 번째 사용자 ID
     * @param userId2 두 번째 사용자 ID
     * @return 1:1 채팅방 (Optional)
     */
    Optional<ChatRoom> findDirectChatRoomByUserIds(Long userId1, Long userId2);

    /**
     * 사용자의 나와의 채팅방(SELF)을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 나와의 채팅방 (Optional)
     */
    Optional<ChatRoom> findSelfChatRoomByUserId(Long userId);

    /**
     * 채팅방을 삭제한다.
     *
     * @param chatRoom 삭제할 채팅방
     */
    void delete(ChatRoom chatRoom);

    /**
     * 전체 채팅방 수를 조회한다.
     *
     * @return 채팅방 수
     */
    long count();
}
