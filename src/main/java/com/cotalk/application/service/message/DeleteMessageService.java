package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.exception.MessageAccessDeniedException;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.port.inbound.message.DeleteMessageUseCase;
import com.cotalk.domain.port.outbound.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메시지 삭제 유스케이스 구현체.
 * 메시지를 소프트 삭제한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteMessageService implements DeleteMessageUseCase {

    private final MessageRepository messageRepository;

    /**
     * 메시지를 삭제한다.
     * 본인이 보낸 메시지만 삭제할 수 있으며, 소프트 삭제 방식으로 처리된다.
     *
     * @param messageId 삭제할 메시지 ID
     * @param userId 요청 사용자 ID
     * @throws MessageNotFoundException 메시지가 존재하지 않는 경우
     * @throws MessageAccessDeniedException 본인이 보낸 메시지가 아니거나 이미 삭제된 경우
     */
    @Override
    public void deleteMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        // 본인이 보낸 메시지인지 확인
        if (!message.isSentBy(userId)) {
            throw MessageAccessDeniedException.notSender();
        }

        // 이미 삭제된 메시지인지 확인
        if (message.isDeleted()) {
            throw MessageAccessDeniedException.alreadyDeleted();
        }

        // 메시지 삭제 (소프트 삭제)
        message.delete();
        messageRepository.save(message);

        log.info("Message deleted: messageId={}, userId={}", messageId, userId);
    }
}
