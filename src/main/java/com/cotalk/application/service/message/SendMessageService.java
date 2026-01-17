package com.cotalk.application.service.message;

import com.cotalk.domain.entity.ChatRoomMember;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.Message.MessageType;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase;
import com.cotalk.domain.port.inbound.notification.SendPushNotificationUseCase;
import com.cotalk.domain.port.outbound.ChatRoomMemberRepository;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.port.outbound.IdGenerator;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 메시지 전송 유스케이스 구현체.
 * 텍스트 메시지와 파일 메시지를 전송한다.
 *
 * @author seunggu.lee
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SendMessageService implements SendMessageUseCase {

    private final MessageRepository messageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final IdGenerator idGenerator;
    private final SendPushNotificationUseCase sendPushNotificationUseCase;
    private final ChatRoomMemberValidator chatRoomMemberValidator;

    /**
     * 텍스트 메시지를 전송한다.
     * 채팅방의 다른 멤버들에게 푸시 알림을 전송한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param senderId 발신자 ID
     * @param content 메시지 내용
     * @return 전송된 메시지
     * @throws ChatRoomAccessDeniedException 채팅방 멤버가 아닌 경우
     */
    @Override
    public Message sendMessage(Long chatRoomId, Long senderId, String content) {
        // 채팅방 멤버인지 확인
        chatRoomMemberValidator.validateMembership(chatRoomId, senderId);

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

    /**
     * 파일 메시지를 전송한다.
     * 이미지 또는 파일을 첨부한 메시지를 전송하고, 채팅방의 다른 멤버들에게 푸시 알림을 전송한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param senderId 발신자 ID
     * @param command 파일 메시지 정보
     * @return 전송된 파일 메시지
     * @throws ChatRoomAccessDeniedException 채팅방 멤버가 아닌 경우
     */
    @Override
    public Message sendFileMessage(Long chatRoomId, Long senderId, FileMessageCommand command) {
        // 채팅방 멤버인지 확인
        chatRoomMemberValidator.validateMembership(chatRoomId, senderId);

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

    private void sendPushNotificationsToOtherMembers(Long chatRoomId, Long senderId, String content) {
        // 발신자 정보 조회
        String senderNickname = userRepository.findById(senderId)
                .map(User::getNickname)
                .orElse("알 수 없음");

        // 채팅방의 다른 멤버들 ID 목록 추출
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(chatRoomId);
        List<Long> receiverUserIds = members.stream()
                .map(ChatRoomMember::getUserId)
                .filter(userId -> !userId.equals(senderId))
                .toList();

        // 벌크 푸시 알림 전송 (한 번의 호출로 처리)
        if (!receiverUserIds.isEmpty()) {
            sendPushNotificationUseCase.sendNewMessageNotificationBulk(
                    receiverUserIds,
                    senderNickname,
                    content,
                    chatRoomId
            );
        }
    }
}
