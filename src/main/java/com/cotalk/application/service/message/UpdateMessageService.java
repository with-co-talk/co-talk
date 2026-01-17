package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.exception.MessageAccessDeniedException;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.port.inbound.message.UpdateMessageUseCase;
import com.cotalk.domain.port.outbound.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메시지 수정 유스케이스 구현체.
 * 메시지 내용을 수정한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateMessageService implements UpdateMessageUseCase {

    private final MessageRepository messageRepository;

    /**
     * 메시지 내용을 수정한다.
     * 본인이 보낸 메시지만 수정할 수 있으며, 삭제된 메시지는 수정할 수 없다.
     *
     * @param messageId 수정할 메시지 ID
     * @param userId 요청 사용자 ID
     * @param newContent 새로운 메시지 내용
     * @return 수정된 메시지
     * @throws MessageNotFoundException 메시지가 존재하지 않는 경우
     * @throws MessageAccessDeniedException 본인이 보낸 메시지가 아니거나 이미 삭제된 경우
     */
    @Override
    public Message updateMessage(Long messageId, Long userId, String newContent) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        // 본인이 보낸 메시지인지 확인
        if (!message.isSentBy(userId)) {
            throw MessageAccessDeniedException.notSender();
        }

        // 삭제된 메시지인지 확인
        if (message.isDeleted()) {
            throw MessageAccessDeniedException.alreadyDeleted();
        }

        // 메시지 수정
        message.updateContent(newContent);
        Message updated = messageRepository.save(message);

        log.info("Message updated: messageId={}, userId={}", messageId, userId);
        return updated;
    }
}
