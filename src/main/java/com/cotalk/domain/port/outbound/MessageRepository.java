package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.Message;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 메시지 레포지토리 포트.
 * 채팅 메시지 데이터 저장 및 조회를 위한 인터페이스를 정의한다.
 *
 * @author seunggu.lee
 */
public interface MessageRepository {

    /**
     * 메시지를 저장한다.
     *
     * @param message 저장할 메시지
     * @return 저장된 메시지
     */
    Message save(Message message);

    /**
     * ID로 메시지를 조회한다.
     *
     * @param id 메시지 ID
     * @return 조회된 메시지 (Optional)
     */
    Optional<Message> findById(Long id);

    /**
     * 채팅방의 메시지를 생성일시 역순으로 페이징 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param page       페이지 번호 (0부터 시작)
     * @param size       페이지 크기
     * @return 메시지 목록 (최신순)
     */
    List<Message> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, int page, int size);

    /**
     * 채팅방에서 특정 시점 이후의 읽지 않은 메시지 수를 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId     사용자 ID
     * @param lastReadAt 마지막 읽은 시간
     * @return 읽지 않은 메시지 수
     */
    long countUnreadMessages(Long chatRoomId, Long userId, LocalDateTime lastReadAt);

    /**
     * 채팅방에서 마지막 읽은 메시지 ID 이후의 읽지 않은 메시지 수를 조회한다.
     *
     * <p>카톡/라인 스타일: unreadCount(채팅 목록)는 "내가 아직 읽지 않은 메시지 수"이며
     * messageId 기준으로 결정적으로 계산한다.</p>
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID (본인 메시지 제외)
     * @param lastReadMessageId 마지막 읽은 메시지 ID (null이면 모두 안 읽음)
     * @return 읽지 않은 메시지 수
     */
    long countUnreadMessagesByLastReadMessageId(Long chatRoomId, Long userId, Long lastReadMessageId);

    /**
     * 채팅방의 가장 최근 메시지를 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @return 가장 최근 메시지 (Optional)
     */
    Optional<Message> findTopByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);

    /**
     * 커서 기반 메시지 조회.
     * 특정 메시지 ID 이전의 메시지를 조회한다.
     *
     * @param chatRoomId      채팅방 ID
     * @param beforeMessageId 이 ID 이전의 메시지 조회 (null이면 최신부터)
     * @param size            조회할 개수
     * @return 메시지 목록 (최신순)
     */
    List<Message> findByChatRoomIdBeforeMessageId(Long chatRoomId, Long beforeMessageId, int size);

    /**
     * 특정 채팅방 내에서 키워드로 메시지를 검색한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param keyword    검색 키워드
     * @param page       페이지 번호 (0부터 시작)
     * @param size       페이지 크기
     * @return 검색된 메시지 목록 (최신순)
     */
    List<Message> searchByKeywordInChatRoom(Long chatRoomId, String keyword, int page, int size);

    /**
     * 사용자가 속한 모든 채팅방에서 키워드로 메시지를 검색한다.
     *
     * @param userId  사용자 ID
     * @param keyword 검색 키워드
     * @param page    페이지 번호 (0부터 시작)
     * @param size    페이지 크기
     * @return 검색된 메시지 목록 (최신순)
     */
    List<Message> searchByKeywordInUserChatRooms(Long userId, String keyword, int page, int size);

    /**
     * 전체 메시지 수를 조회한다.
     *
     * @return 메시지 수
     */
    long count();

    /**
     * 여러 채팅방의 마지막 메시지를 한 번에 조회한다. (N+1 쿼리 방지용 배치 조회)
     *
     * @param chatRoomIds 채팅방 ID 목록
     * @return 마지막 메시지 목록
     */
    List<Message> findLastMessagesByRoomIds(List<Long> chatRoomIds);

    /**
     * 여러 채팅방의 읽지 않은 메시지 수를 한 번에 조회한다. (N+1 쿼리 방지용 배치 조회)
     *
     * @param userId 사용자 ID
     * @param chatRoomIds 채팅방 ID 목록
     * @return 채팅방 ID를 키로, 읽지 않은 메시지 수를 값으로 하는 Map
     */
    Map<Long, Long> batchCountUnreadMessages(Long userId, List<Long> chatRoomIds);

    /**
     * 채팅방에서 특정 사용자를 제외한 다른 발신자 ID 목록을 조회한다.
     * 1:1 채팅방에서 상대방이 나갔을 때 상대방 ID를 찾는 데 사용한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param excludeUserId 제외할 사용자 ID
     * @return 다른 발신자 ID 목록
     */
    List<Long> findDistinctSenderIdsByChatRoomIdExcludingUser(Long chatRoomId, Long excludeUserId);

    /**
     * 여러 채팅방에서 특정 사용자를 제외한 다른 발신자 ID를 한 번에 조회한다.
     * 1:1 채팅방에서 상대방이 나갔을 때 상대방 ID를 배치로 찾는 데 사용한다. (N+1 쿼리 방지)
     *
     * <p>각 채팅방별로 첫 번째 발신자 ID를 맵으로 반환한다.
     * 1:1 채팅방에서는 본인 외의 발신자가 1명이므로 첫 번째 값만 사용한다.
     *
     * @param chatRoomIds 채팅방 ID 목록
     * @param excludeUserId 제외할 사용자 ID
     * @return 채팅방 ID를 키로, 첫 번째 다른 발신자 ID를 값으로 하는 Map
     */
    Map<Long, Long> findDistinctSenderIdsByChatRoomIdsExcludingUser(List<Long> chatRoomIds, Long excludeUserId);

    /**
     * 채팅방의 모든 멤버에 대해 읽지 않은 메시지 수를 한 번에 조회한다.
     * (N+1 쿼리 방지용 배치 조회)
     *
     * <p>각 멤버의 lastReadMessageId를 기준으로 해당 멤버가 읽지 않은 메시지 수를 계산한다.
     * 본인이 보낸 메시지는 제외한다.</p>
     *
     * @param chatRoomId 채팅방 ID
     * @return 사용자 ID를 키로, 읽지 않은 메시지 수를 값으로 하는 Map
     */
    Map<Long, Long> batchCountUnreadMessagesForAllMembers(Long chatRoomId);

    /**
     * 여러 사용자에 대해 모든 채팅방을 합산한 총 읽지 않은 메시지 수를 한 번에 조회한다.
     * (N+1 쿼리 방지용 배치 조회)
     *
     * <p>각 사용자의 lastReadMessageId를 기준으로 해당 사용자가 참여한 모든 채팅방에서
     * 읽지 않은 메시지 수를 합산한다. 본인이 보낸 메시지는 제외한다.
     * iOS 앱 아이콘 배지 표시 등에 사용한다.</p>
     *
     * @param userIds 사용자 ID 목록
     * @return 사용자 ID를 키로, 총 읽지 않은 메시지 수를 값으로 하는 Map (읽지 않은 메시지가 없는 사용자는 0)
     */
    Map<Long, Long> batchCountTotalUnread(List<Long> userIds);

    /**
     * 채팅방에서 특정 타입의 메시지를 조회한다. (미디어 갤러리용)
     *
     * @param chatRoomId 채팅방 ID
     * @param types 메시지 타입 목록 (IMAGE, FILE 등)
     * @param pageable 페이징 정보
     * @return 해당 타입의 메시지 목록 (최신순)
     */
    List<Message> findByTypeInChatRoom(Long chatRoomId, List<Message.MessageType> types, org.springframework.data.domain.Pageable pageable);

    /**
     * 채팅방에서 링크 미리보기가 있는 메시지를 조회한다. (미디어 갤러리용)
     *
     * @param chatRoomId 채팅방 ID
     * @param pageable 페이징 정보
     * @return 링크 미리보기가 있는 메시지 목록 (최신순)
     */
    List<Message> findMessagesWithLinkPreview(Long chatRoomId, org.springframework.data.domain.Pageable pageable);

    /**
     * 특정 발신자가 보낸 모든 메시지를 삭제한다.
     * 회원 탈퇴 시 사용자가 보낸 메시지를 정리하는 데 사용한다.
     *
     * @param senderId 발신자(사용자) ID
     */
    void deleteBySenderId(Long senderId);
}
