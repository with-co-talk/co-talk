package com.cotalk.application.service;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.inbound.GetMessageHistoryUseCase;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMessageHistoryService implements GetMessageHistoryUseCase {

    private final MessageRepository messageRepository;
    private final ChatRoomMemberValidator chatRoomMemberValidator;

    @Override
    public List<Message> getMessageHistory(Long chatRoomId, Long userId, Long beforeMessageId, int size) {
        chatRoomMemberValidator.validateMembership(chatRoomId, userId);

        return messageRepository.findByChatRoomIdBeforeMessageId(chatRoomId, beforeMessageId, size);
    }
}
