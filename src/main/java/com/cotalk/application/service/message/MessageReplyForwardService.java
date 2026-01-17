package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.exception.ChatRoomAccessDeniedException;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.port.inbound.message.MessageReplyForwardUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.port.outbound.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메시지 답장/전달 유스케이스 구현체.
 * 메시지에 답장하거나 다른 채팅방으로 전달한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MessageReplyForwardService implements MessageReplyForwardUseCase {

    private final MessageRepository messageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final IdGenerator idGenerator;

    /**
     * 메시지에 답장한다.
     * 원본 메시지와 동일한 채팅방에 답장 메시지를 생성한다.
     *
     * @param senderId 발신자 ID
     * @param originalMessageId 원본 메시지 ID
     * @param content 답장 내용
     * @return 생성된 답장 메시지
     * @throws MessageNotFoundException 원본 메시지가 존재하지 않는 경우
     * @throws ChatRoomAccessDeniedException 채팅방 멤버가 아닌 경우
     */
    @Override
    public Message replyToMessage(Long senderId, Long originalMessageId, String content) {
        Message originalMessage = getMessageOrThrow(originalMessageId);
        Long chatRoomId = originalMessage.getChatRoomId();

        validateChatRoomAccess(chatRoomId, senderId);

        Message replyMessage = Message.builder()
                .id(idGenerator.nextId())
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content(content)
                .type(Message.MessageType.TEXT)
                .replyToMessageId(originalMessageId)
                .build();

        return messageRepository.save(replyMessage);
    }

    /**
     * 메시지를 다른 채팅방으로 전달한다.
     * 원본 메시지의 내용과 파일 정보를 복사하여 대상 채팅방에 새 메시지를 생성한다.
     *
     * @param senderId 발신자 ID
     * @param originalMessageId 원본 메시지 ID
     * @param targetChatRoomId 대상 채팅방 ID
     * @return 생성된 전달 메시지
     * @throws MessageNotFoundException 원본 메시지가 존재하지 않는 경우
     * @throws ChatRoomAccessDeniedException 원본 또는 대상 채팅방 멤버가 아닌 경우
     */
    @Override
    public Message forwardMessage(Long senderId, Long originalMessageId, Long targetChatRoomId) {
        Message originalMessage = getMessageOrThrow(originalMessageId);
        Long originalChatRoomId = originalMessage.getChatRoomId();

        // 원본 채팅방 접근 권한 확인
        validateChatRoomAccess(originalChatRoomId, senderId);
        // 대상 채팅방 접근 권한 확인
        validateChatRoomAccess(targetChatRoomId, senderId);

        Message forwardedMessage = Message.builder()
                .id(idGenerator.nextId())
                .chatRoomId(targetChatRoomId)
                .senderId(senderId)
                .content(originalMessage.getContent())
                .type(originalMessage.getType())
                .fileUrl(originalMessage.getFileUrl())
                .fileName(originalMessage.getFileName())
                .fileSize(originalMessage.getFileSize())
                .fileContentType(originalMessage.getFileContentType())
                .thumbnailUrl(originalMessage.getThumbnailUrl())
                .forwardedFromMessageId(originalMessageId)
                .build();

        return messageRepository.save(forwardedMessage);
    }

    private Message getMessageOrThrow(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));
    }

    private void validateChatRoomAccess(Long chatRoomId, Long userId) {
        if (!chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)) {
            throw new ChatRoomAccessDeniedException(userId, chatRoomId);
        }
    }
}
