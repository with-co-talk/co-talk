package com.cotalk.application.service;

import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.port.inbound.GetMessageReactionsUseCase;
import com.cotalk.domain.port.outbound.MessageReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMessageReactionsService implements GetMessageReactionsUseCase {

    private final MessageReactionRepository reactionRepository;

    @Override
    public List<MessageReaction> getReactions(Long messageId) {
        return reactionRepository.findByMessageId(messageId);
    }
}
