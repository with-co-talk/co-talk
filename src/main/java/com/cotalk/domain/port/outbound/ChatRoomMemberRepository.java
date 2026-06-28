package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.ChatRoomMember;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
     * 여러 채팅방 멤버를 일괄 저장한다.
     *
     * @param members 저장할 채팅방 멤버 목록
     * @return 저장된 채팅방 멤버 목록
     */
    List<ChatRoomMember> saveAll(List<ChatRoomMember> members);

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

    /**
     * 마지막 읽은 시간을 원자적으로 업데이트한다.
     * 기존 값보다 큰 경우에만 업데이트하여 Lost Update를 방지한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId     사용자 ID
     * @param lastReadAt 새로운 읽은 시간
     * @return 업데이트된 행 수
     */
    int updateLastReadAtIfNewer(Long chatRoomId, Long userId, LocalDateTime lastReadAt);

    /**
     * 마지막 읽은 메시지 ID를 원자적으로 업데이트한다.
     * 기존 값보다 큰 경우에만 업데이트하여 Lost Update를 방지한다.
     *
     * <p>lastReadAt은 보조 정보로 함께 갱신된다.</p>
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     * @param lastReadAt 마지막 읽은 시간(보조)
     * @param lastReadMessageId 마지막 읽은 메시지 ID (null이면 업데이트하지 않음)
     * @return 업데이트된 행 수
     */
    int updateLastReadMessageIdIfNewer(Long chatRoomId, Long userId, LocalDateTime lastReadAt, Long lastReadMessageId);

    /**
     * 특정 메시지를 읽지 않은 멤버 수를 조회한다. (messageId 기준)
     * 발신자는 제외한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param messageId 메시지 ID
     * @param senderId 발신자 ID
     * @return 읽지 않은 멤버 수
     */
    int countUnreadMembersByMessageId(Long chatRoomId, Long messageId, Long senderId);

    /**
     * 마지막 읽은 시간만 업데이트한다 (메시지가 없는 채팅방용).
     * 메시지가 없는 채팅방에서도 lastReadAt을 설정하여,
     * 나중에 메시지가 추가될 때 정확한 unreadCount를 계산할 수 있도록 한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId     사용자 ID
     * @param lastReadAt 새로운 읽은 시간
     * @return 업데이트된 행 수
     */
    int updateLastReadAt(Long chatRoomId, Long userId, LocalDateTime lastReadAt);

    /**
     * 여러 메시지의 읽지 않은 멤버 수를 한 번에 조회한다. (N+1 쿼리 방지용 배치 조회)
     *
     * @param chatRoomId 채팅방 ID
     * @param messageIds 메시지 ID 목록
     * @return 메시지 ID를 키로, 읽지 않은 멤버 수를 값으로 하는 Map
     */
    Map<Long, Integer> batchCountUnreadMembersByMessageIds(Long chatRoomId, List<Long> messageIds);

    /**
     * 특정 사용자의 여러 채팅방 멤버 정보를 한 번에 조회한다. (N+1 쿼리 방지용 배치 조회)
     *
     * @param userId 사용자 ID
     * @param chatRoomIds 채팅방 ID 목록
     * @return 채팅방 멤버 목록
     */
    List<ChatRoomMember> findByUserIdAndChatRoomIds(Long userId, List<Long> chatRoomIds);

    /**
     * 여러 채팅방의 상대방(본인 제외) 멤버 정보를 한 번에 조회한다. (N+1 쿼리 방지용 배치 조회)
     *
     * @param userId 본인 사용자 ID (제외할 사용자)
     * @param chatRoomIds 채팅방 ID 목록
     * @return 상대방 멤버 목록
     */
    List<ChatRoomMember> findOtherMembersByChatRoomIds(Long userId, List<Long> chatRoomIds);
}
