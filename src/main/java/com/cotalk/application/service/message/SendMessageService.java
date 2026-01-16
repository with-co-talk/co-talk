package com.cotalk.application.service.message;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.exception.ChatRoomNotFoundException;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase;
import com.cotalk.domain.port.outbound.persistence.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.persistence.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 메시지 전송 Use Case 구현체.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SendMessageService implements SendMessageUseCase {

    private final MessageRepository messageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Override
    public Message sendMessage(UUID chatRoomId, UUID senderId, String content) {
        // 발신자가 채팅방 멤버인지 확인
        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, senderId)
                .orElseThrow(() -> new ChatRoomNotFoundException(chatRoomId));

        // 메시지 생성
        Message message = Message.builder()
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content(content)
                .messageType(Message.MessageType.TEXT)
                .build();

        // 메시지 내용 검증
        message.validateContent();

        // 메시지 저장
        return messageRepository.save(message);
    }
}
