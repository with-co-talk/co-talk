package com.cotalk.domain.port.inbound.message;

import com.cotalk.domain.entity.Message;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 메시지 히스토리 조회 유스케이스.
 * 채팅방의 메시지 히스토리를 커서 기반으로 조회한다.
 *
 * @author seunggu.lee
 */
public interface GetMessageHistoryUseCase {

    /**
     * 커서 기반으로 메시지 히스토리를 조회한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID (권한 확인용)
     * @param beforeMessageId 이 메시지 ID 이전의 메시지를 조회 (null이면 최신 메시지부터)
     * @param size 조회할 메시지 개수
     * @return 메시지 목록 (최신순 정렬)
     */
    List<Message> getMessageHistory(Long chatRoomId, Long userId, Long beforeMessageId, int size);

    /**
     * 커서 기반으로 메시지 히스토리를 조회하고, 읽지 않은 멤버 수와 발신자 정보를 포함한 결과를 반환한다.
     * 배치 쿼리를 사용하여 N+1 쿼리를 방지한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID (권한 확인용)
     * @param beforeMessageId 이 메시지 ID 이전의 메시지를 조회 (null이면 최신 메시지부터)
     * @param size 조회할 메시지 개수
     * @return 메시지 히스토리 조회 결과 (메시지, 읽지 않은 수, 발신자 정보 포함)
     */
    EnrichedMessageHistoryResult getEnrichedMessageHistory(Long chatRoomId, Long userId, Long beforeMessageId, int size);

    /**
     * 메시지 히스토리 조회 결과에 포함되는 개별 메시지 정보.
     *
     * @param message 메시지 엔티티
     * @param unreadCount 읽지 않은 멤버 수
     * @param senderNickname 발신자 닉네임
     * @param senderAvatarUrl 발신자 프로필 이미지 URL
     */
    record EnrichedMessage(
            Message message,
            int unreadCount,
            String senderNickname,
            String senderAvatarUrl
    ) {}

    /**
     * 메시지 히스토리 조회 결과.
     *
     * @param messages 메시지 목록 (읽지 않은 수, 발신자 정보 포함)
     * @param nextCursor 다음 페이지 커서 (마지막 메시지 ID)
     * @param hasMore 다음 페이지 존재 여부
     */
    record EnrichedMessageHistoryResult(
            List<EnrichedMessage> messages,
            Long nextCursor,
            boolean hasMore
    ) {}
}
