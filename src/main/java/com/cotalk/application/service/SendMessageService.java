package com.cotalk.application.service;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.Message.MessageType;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.ChatRoomNotFoundException;
import com.cotalk.domain.port.inbound.SendMessageUseCase;
import com.cotalk.domain.port.inbound.SendPushNotificationUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.id.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SendMessageService implements SendMessageUseCase {

    private final MessageRepository messageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final SendPushNotificationUseCase sendPushNotificationUseCase;

    @Override
    public Message sendMessage(Long chatRoomId, Long senderId, String content) {
        // 채팅방 멤버인지 확인
        validateChatRoomMember(chatRoomId, senderId);

        // 메시지 생성
        Message message = Message.builder()
                .id(idGenerator.nextId())
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content(content)
                .type(MessageType.TEXT)
                .build();

        // 내용 검증
        message.validateContent();

        Message savedMessage = messageRepository.save(message);

        // 푸시 알림 전송 (비동기)
        sendPushNotificationsToOtherMembers(chatRoomId, senderId, content);

        return savedMessage;
    }

    @Override
    public Message sendFileMessage(Long chatRoomId, Long senderId, FileMessageCommand command) {
        // 채팅방 멤버인지 확인
        validateChatRoomMember(chatRoomId, senderId);

        // 파일 메시지 생성
        Message message = Message.builder()
                .id(idGenerator.nextId())
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content(command.fileName()) // 파일명을 content로 저장
                .type(command.getMessageType())
                .fileUrl(command.fileUrl())
                .fileName(command.fileName())
                .fileSize(command.fileSize())
                .fileContentType(command.contentType())
                .thumbnailUrl(command.thumbnailUrl())
                .build();

        // 내용 검증
        message.validateContent();

        Message savedMessage = messageRepository.save(message);

        // 푸시 알림 전송 (비동기)
        String notificationContent = command.getMessageType() == MessageType.IMAGE 
                ? "📷 사진을 보냈습니다." 
                : "📎 파일을 보냈습니다: " + command.fileName();
        sendPushNotificationsToOtherMembers(chatRoomId, senderId, notificationContent);

        return savedMessage;
    }

    private void validateChatRoomMember(Long chatRoomId, Long userId) {
        chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new ChatRoomNotFoundException(chatRoomId));
    }

    private void sendPushNotificationsToOtherMembers(Long chatRoomId, Long senderId, String content) {
        // 발신자 정보 조회
        String senderNickname = userRepository.findById(senderId)
                .map(User::getNickname)
                .orElse("알 수 없음");

        // 채팅방의 다른 멤버들에게 푸시 알림 전송
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(chatRoomId);
        members.stream()
                .filter(member -> !member.getUserId().equals(senderId))
                .forEach(member -> 
                        sendPushNotificationUseCase.sendNewMessageNotification(
                                member.getUserId(), 
                                senderNickname, 
                                content, 
                                chatRoomId
                        )
                );
    }
}
