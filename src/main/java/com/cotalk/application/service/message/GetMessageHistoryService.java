package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.inbound.message.GetMessageHistoryUseCase;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 메시지 히스토리 조회 유스케이스 구현체.
 * 채팅방의 메시지 히스토리를 조회한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMessageHistoryService implements GetMessageHistoryUseCase {

    private final MessageRepository messageRepository;
    private final ChatRoomMemberValidator chatRoomMemberValidator;

    /**
     * 채팅방의 메시지 히스토리를 조회한다.
     * 특정 메시지 이전의 메시지들을 조회하며, 채팅방 멤버만 조회할 수 있다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 요청 사용자 ID
     * @param beforeMessageId 기준 메시지 ID (이 메시지 이전의 메시지를 조회)
     * @param size 조회할 메시지 개수
     * @return 메시지 목록
     * @throws ChatRoomAccessDeniedException 채팅방 멤버가 아닌 경우
     */
    @Override
    public List<Message> getMessageHistory(Long chatRoomId, Long userId, Long beforeMessageId, int size) {
        chatRoomMemberValidator.validateMembership(chatRoomId, userId);

        return messageRepository.findByChatRoomIdBeforeMessageId(chatRoomId, beforeMessageId, size);
    }
}
