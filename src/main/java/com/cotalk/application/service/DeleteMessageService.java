package com.cotalk.application.service;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.exception.MessageAccessDeniedException;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.port.inbound.DeleteMessageUseCase;
import com.cotalk.domain.port.outbound.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteMessageService implements DeleteMessageUseCase {

    private final MessageRepository messageRepository;

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
