package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.message.GetMediaGalleryUseCase;
import com.cotalk.domain.port.outbound.FileStorage;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.domain.validator.ChatRoomMemberValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 미디어 갤러리 조회 유스케이스 구현체.
 * 채팅방의 사진, 파일, 링크를 타입별로 페이징하여 조회한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class GetMediaGalleryService implements GetMediaGalleryUseCase {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatRoomMemberValidator chatRoomMemberValidator;
    private final FileStorage fileStorage;
    private final int presignedUrlExpiryMinutes;

    /**
     * GetMediaGalleryService를 생성한다.
     *
     * @param messageRepository          메시지 저장소 포트
     * @param userRepository             사용자 저장소 포트
     * @param chatRoomMemberValidator    채팅방 멤버십 검증기
     * @param fileStorage                파일 저장소 포트(첨부파일 Pre-signed URL 재발급용)
     * @param presignedUrlExpiryMinutes  첨부파일 Pre-signed URL 만료 시간(분)
     */
    public GetMediaGalleryService(
            MessageRepository messageRepository,
            UserRepository userRepository,
            ChatRoomMemberValidator chatRoomMemberValidator,
            FileStorage fileStorage,
            @Value("${minio.presigned-url-expiry-minutes:10}") int presignedUrlExpiryMinutes) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.chatRoomMemberValidator = chatRoomMemberValidator;
        this.fileStorage = fileStorage;
        this.presignedUrlExpiryMinutes = presignedUrlExpiryMinutes;
    }

    /**
     * 채팅방의 미디어 갤러리를 조회한다.
     * 사진, 파일, 링크를 타입별로 페이징하여 반환한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID (권한 확인용)
     * @param type 미디어 유형 (PHOTO, FILE, LINK)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 미디어 갤러리 조회 결과
     * @throws com.cotalk.domain.exception.ChatRoomAccessDeniedException 채팅방 멤버가 아닌 경우
     * @throws IllegalArgumentException 지원하지 않는 미디어 유형인 경우
     */
    @Override
    public MediaGalleryResult getMediaGallery(Long chatRoomId, Long userId, String type, int page, int size) {
        // 권한 체크: 채팅방 멤버인지 확인
        chatRoomMemberValidator.validateMembership(chatRoomId, userId);

        Pageable pageable = PageRequest.of(page, size);
        List<Message> messages;

        switch (type.toUpperCase()) {
            case "PHOTO" -> messages = messageRepository.findByTypeInChatRoom(
                    chatRoomId, List.of(Message.MessageType.IMAGE), pageable);
            case "FILE" -> messages = messageRepository.findByTypeInChatRoom(
                    chatRoomId, List.of(Message.MessageType.FILE), pageable);
            case "LINK" -> messages = messageRepository.findMessagesWithLinkPreview(chatRoomId, pageable);
            default -> throw new IllegalArgumentException("지원하지 않는 미디어 유형입니다: " + type);
        }

        // 발신자 정보 배치 조회 (N+1 쿼리 방지)
        Set<Long> senderIds = messages.stream().map(Message::getSenderId).collect(Collectors.toSet());
        Map<Long, User> senderMap = userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 멤버십이 검증된 멤버에게만, 저장된 첨부파일 URL을 단기 Pre-signed URL로 재발급한다(H-1).
        // 페이지 전체에 대해 방 단위로 멤버십을 1회만 검증했으므로 첨부파일별 재검증은 하지 않는다.
        List<MediaGalleryItem> items = messages.stream()
                .map(message -> {
                    User sender = senderMap.get(message.getSenderId());
                    return new MediaGalleryItem(
                            message.getId(),
                            message.getType().name(),
                            fileStorage.presignAttachmentUrl(message.getFileUrl(), presignedUrlExpiryMinutes),
                            message.getFileName(),
                            message.getFileSize(),
                            message.getFileContentType(),
                            fileStorage.presignAttachmentUrl(message.getThumbnailUrl(), presignedUrlExpiryMinutes),
                            message.getLinkPreviewUrl(),
                            message.getLinkPreviewTitle(),
                            message.getLinkPreviewDescription(),
                            message.getLinkPreviewImageUrl(),
                            message.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli(),
                            message.getSenderId(),
                            sender != null ? sender.getNickname() : null
                    );
                })
                .toList();

        Long nextCursor = messages.isEmpty() ? null : messages.get(messages.size() - 1).getId();
        boolean hasMore = messages.size() == size;

        return new MediaGalleryResult(items, nextCursor, hasMore);
    }
}
