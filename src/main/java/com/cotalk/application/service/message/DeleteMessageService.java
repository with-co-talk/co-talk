package com.cotalk.application.service.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.exception.MessageNotFoundException;
import com.cotalk.domain.exception.ResourceAccessDeniedException;
import com.cotalk.domain.port.inbound.message.DeleteMessageUseCase;
import com.cotalk.domain.port.outbound.ChatMessageBroker;
import com.cotalk.domain.port.outbound.FileStorage;
import com.cotalk.domain.port.outbound.MessageRepository;
import com.cotalk.domain.port.outbound.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * 메시지 삭제 유스케이스 구현체.
 * 메시지를 소프트 삭제하고 실시간으로 채팅방 참여자들에게 알린다.
 *
 * <p><b>삭제 정책:</b> 삭제는 "본인이 보낸 메시지"에 한해 허용되는 소프트 삭제이며,
 * 일반 메신저 동작과 동일하게 <b>작성 후 시간이 지나도 본인 메시지는 삭제할 수 있다</b>
 * (수정과 달리 시간 제한을 두지 않는다).
 *
 * <p><b>스토리지 정리:</b> 파일/이미지 메시지를 삭제하면 스토리지에 남는 고아 객체를 막기 위해
 * {@code fileUrl}/{@code thumbnailUrl}이 가리키는 스토리지 객체를 정리한다. 외부 스토리지 호출은
 * DB 트랜잭션 경계 밖(커밋 이후)에서 수행하며, 정리에 실패하더라도 메시지 삭제 자체는 성공한다
 * (실패는 로깅으로 처리하고 후속 정리 잡에 맡긴다).
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteMessageService implements DeleteMessageUseCase {

    /**
     * 업로드 객체 키의 최상위 디렉터리.
     * {@code UploadFileService.generateStoragePath}가 생성하는 {@code uploads/{userId}/...} 규칙과 일치한다.
     */
    private static final String UPLOAD_ROOT = "uploads";

    private final MessageRepository messageRepository;
    private final ChatMessageBroker chatMessageBroker;
    private final TimeProvider timeProvider;
    private final FileStorage fileStorage;

    /**
     * 메시지를 삭제한다.
     * 본인이 보낸 메시지만 삭제할 수 있으며, 소프트 삭제 방식으로 처리된다.
     * 파일/이미지 메시지인 경우 스토리지 원본을 커밋 이후 별도로 정리한다.
     *
     * @param messageId 삭제할 메시지 ID
     * @param userId 요청 사용자 ID
     * @throws MessageNotFoundException 메시지가 존재하지 않는 경우
     * @throws ResourceAccessDeniedException 본인이 보낸 메시지가 아니거나 이미 삭제된 경우
     */
    @Override
    public void deleteMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        // 본인이 보낸 메시지인지 확인
        if (!message.isSentBy(userId)) {
            throw ResourceAccessDeniedException.messageNotSender();
        }

        // 이미 삭제된 메시지인지 확인
        if (message.isDeleted()) {
            throw ResourceAccessDeniedException.messageAlreadyDeleted();
        }

        // 본인 메시지의 (소프트)삭제는 시간 제한 없이 허용한다.
        // 5분 제한은 메시지 "수정"(UpdateMessageService)에만 적용된다.
        var now = timeProvider.now();

        // 메시지 삭제 (소프트 삭제)
        message.delete(now);
        messageRepository.save(message);

        log.info("Message deleted: messageId={}, userId={}", messageId, userId);

        // 채팅방 참여자들에게 메시지 삭제 이벤트 브로드캐스트
        // 삭제 시각(now)을 그대로 넘겨 message.delete(now)와 이벤트 시각을 단일 타임스탬프로 일관화한다.
        publishMessageDeletedEvent(message.getChatRoomId(), messageId, userId, now);

        // 파일/이미지 메시지면 스토리지 원본을 트랜잭션 밖(커밋 이후)에서 정리한다.
        scheduleStorageCleanup(message, messageId);
    }

    /**
     * 파일/이미지 메시지의 스토리지 원본 정리를 예약한다.
     * <p>
     * 외부 스토리지 삭제는 DB 트랜잭션 안에서 수행하면 안 되므로, 트랜잭션 동기화가 활성화된 경우
     * 커밋 이후({@link TransactionSynchronization#afterCommit()})로 미룬다. 트랜잭션이 없는 경우
     * (예: 단위 테스트)에는 즉시 수행한다. 정리 실패는 메시지 삭제에 영향을 주지 않고 로깅만 한다.
     * </p>
     *
     * @param message   삭제된 메시지
     * @param messageId 메시지 ID (로깅용)
     */
    private void scheduleStorageCleanup(Message message, Long messageId) {
        List<String> objectKeys = collectStorageKeys(message);
        if (objectKeys.isEmpty()) {
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cleanupStorage(objectKeys, messageId);
                }
            });
        } else {
            cleanupStorage(objectKeys, messageId);
        }
    }

    /**
     * 메시지의 파일/썸네일 URL에서 스토리지 객체 키를 추출한다.
     * 파일/이미지 메시지가 아니거나 URL이 본 서버 업로드 경로가 아니면 빈 목록을 반환한다.
     *
     * @param message 대상 메시지
     * @return 정리해야 할 스토리지 객체 키 목록
     */
    private List<String> collectStorageKeys(Message message) {
        List<String> keys = new ArrayList<>();
        if (!message.isFile() && !message.isImage()) {
            return keys;
        }
        addObjectKey(keys, message.getFileUrl());
        addObjectKey(keys, message.getThumbnailUrl());
        return keys;
    }

    /**
     * URL에서 업로드 객체 키({@code uploads/{userId}/...})를 추출해 목록에 추가한다.
     * 본 서버 업로드 경로가 아니면 추가하지 않는다.
     *
     * @param keys 객체 키 목록
     * @param url  파일/썸네일 URL
     */
    private void addObjectKey(List<String> keys, String url) {
        String key = extractObjectKey(url);
        if (key != null && !keys.contains(key)) {
            keys.add(key);
        }
    }

    /**
     * 파일 URL에서 스토리지 객체 키를 추출한다.
     * <p>
     * 업로드는 {@code {baseUrl}/.../uploads/{userId}/{file}} 형태의 URL을 저장하므로,
     * path에서 {@code uploads} 세그먼트부터 끝까지를 객체 키로 사용한다
     * ({@code UploadFileService}가 생성하는 키 규칙과 일치).
     * </p>
     *
     * @param url 파일 URL (절대/상대)
     * @return 추출된 객체 키, 추출할 수 없으면 {@code null}
     */
    private String extractObjectKey(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        String path;
        try {
            path = new URI(url).getPath();
        } catch (URISyntaxException e) {
            log.warn("Failed to parse file URL for storage cleanup: {}", url);
            return null;
        }
        if (path == null || path.isBlank()) {
            return null;
        }

        String[] segments = path.split("/");
        for (int i = 0; i < segments.length; i++) {
            if (UPLOAD_ROOT.equals(segments[i]) && i + 2 < segments.length && !segments[i + 2].isEmpty()) {
                StringBuilder key = new StringBuilder(UPLOAD_ROOT);
                for (int j = i + 1; j < segments.length; j++) {
                    if (!segments[j].isEmpty()) {
                        key.append('/').append(segments[j]);
                    }
                }
                return key.toString();
            }
        }

        // URL은 존재하나 업로드 키 규칙(uploads/{userId}/{file})에 맞지 않아 키를 추출하지 못한 경우.
        // 조용히 null을 반환하면 정리 대상에서 누락돼 고아 객체가 추적 없이 누적되므로 로그를 남긴다
        // (경로 규칙 변경/레거시 마이그레이션 URL 등 사후 추적용).
        log.warn("Could not extract upload object key from file URL; storage object may be left orphaned: {}", url);
        return null;
    }

    /**
     * 스토리지 객체들을 삭제한다. 개별 삭제 실패는 로깅만 하고 전체 흐름을 중단하지 않는다.
     *
     * @param objectKeys 삭제할 스토리지 객체 키 목록
     * @param messageId  메시지 ID (로깅용)
     */
    private void cleanupStorage(List<String> objectKeys, Long messageId) {
        for (String key : objectKeys) {
            try {
                fileStorage.delete(key);
                log.info("Storage object cleaned up for deleted message: messageId={}, key={}", messageId, key);
            } catch (Exception e) {
                // 스토리지 정리 실패는 메시지 삭제 성공에 영향을 주지 않는다(고아 객체는 후속 정리 잡에 맡긴다).
                log.warn("Failed to clean up storage object for deleted message: messageId={}, key={}", messageId, key, e);
            }
        }
    }

    /**
     * 메시지 삭제 이벤트를 채팅방에 브로드캐스트한다.
     *
     * @param chatRoomId 채팅방 ID
     * @param messageId  삭제된 메시지 ID
     * @param deletedBy  삭제한 사용자 ID
     * @param deletedAt  삭제 시각 (소프트 삭제에 사용한 시각과 동일한 값을 전달해 일관성 유지)
     */
    private void publishMessageDeletedEvent(Long chatRoomId, Long messageId, Long deletedBy, LocalDateTime deletedAt) {
        String eventId = "message-deleted:" + chatRoomId + ":" + messageId;

        chatMessageBroker.publishRoomEvent(
                chatRoomId,
                new MessageDeletedEvent(
                        1,
                        eventId,
                        "MESSAGE_DELETED",
                        chatRoomId,
                        messageId,
                        deletedBy,
                        deletedAt.toInstant(ZoneOffset.UTC).toEpochMilli()
                )
        );

        log.debug("Message deleted event published: roomId={}, messageId={}", chatRoomId, messageId);
    }

    /**
     * 메시지 삭제 이벤트 DTO.
     * Redis Pub/Sub -> WebSocket 방 토픽(/topic/chat/room/{roomId})으로 전달되는 이벤트다.
     *
     * @param schemaVersion 스키마 버전
     * @param eventId       이벤트 고유 ID (중복 체크용)
     * @param eventType     이벤트 유형 (MESSAGE_DELETED)
     * @param chatRoomId    채팅방 ID
     * @param messageId     삭제된 메시지 ID
     * @param deletedBy     삭제한 사용자 ID
     * @param deletedAtMillis 삭제 시간 (밀리초)
     */
    private record MessageDeletedEvent(
            Integer schemaVersion,
            String eventId,
            String eventType,
            Long chatRoomId,
            Long messageId,
            Long deletedBy,
            Long deletedAtMillis
    ) {}
}
