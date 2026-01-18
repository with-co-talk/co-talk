package com.cotalk.application.service.message;

import com.cotalk.domain.port.inbound.message.MarkAsReadUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 메시지 읽음 처리 유스케이스 구현체.
 * 채팅방의 메시지를 읽음 처리한다.
 *
 * <p>동시성 제어:
 * <ul>
 *   <li>원자적 UPDATE 쿼리로 Lost Update 방지</li>
 *   <li>기존 시간보다 새로운 시간인 경우에만 업데이트</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MarkAsReadService implements MarkAsReadUseCase {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomMemberValidator chatRoomMemberValidator;

    /**
     * 채팅방의 메시지를 읽음 처리한다.
     * 현재 시간을 마지막 읽은 시간으로 업데이트한다.
     *
     * <p>동시성 처리:
     * <ul>
     *   <li>원자적 UPDATE 쿼리 사용 (기존 시간보다 큰 경우에만 업데이트)</li>
     *   <li>여러 스레드가 동시에 호출해도 항상 최신 시간이 유지됨</li>
     * </ul>
     *
     * @param userId     사용자 ID
     * @param chatRoomId 채팅방 ID
     * @throws com.cotalk.domain.exception.ChatRoomAccessDeniedException 채팅방 멤버가 아닌 경우
     */
    @Override
    public void markAsRead(Long userId, Long chatRoomId) {
        // 멤버 검증
        chatRoomMemberValidator.getMemberOrThrow(chatRoomId, userId);

        // 원자적 업데이트 (기존 시간보다 큰 경우에만)
        LocalDateTime now = LocalDateTime.now();
        int updated = chatRoomMemberRepository.updateLastReadAtIfNewer(chatRoomId, userId, now);

        if (updated > 0) {
            log.debug("Marked as read: userId={}, chatRoomId={}, lastReadAt={}", userId, chatRoomId, now);
        }
    }
}
