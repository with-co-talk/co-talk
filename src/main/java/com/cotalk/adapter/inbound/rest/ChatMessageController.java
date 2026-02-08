package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.common.MessageResponse;
import com.cotalk.adapter.inbound.rest.dto.message.ForwardMessageRequest;
import com.cotalk.adapter.inbound.rest.dto.message.MediaGalleryResponse;
import com.cotalk.adapter.inbound.rest.dto.message.MessageDto;
import com.cotalk.adapter.inbound.rest.dto.message.MessageHistoryResponse;
import com.cotalk.adapter.inbound.rest.dto.message.ReplyMessageRequest;
import com.cotalk.adapter.inbound.rest.dto.message.SendFileMessageRequest;
import com.cotalk.adapter.inbound.rest.dto.message.SendMessageRequest;
import com.cotalk.adapter.inbound.rest.dto.message.SendMessageResponse;
import com.cotalk.adapter.inbound.rest.dto.message.UpdateMessageRequest;
import com.cotalk.adapter.inbound.rest.dto.message.UpdateMessageResponse;
import com.cotalk.domain.entity.Message;
import com.cotalk.domain.port.inbound.message.DeleteMessageUseCase;
import com.cotalk.domain.port.inbound.message.GetMediaGalleryUseCase;
import com.cotalk.domain.port.inbound.message.GetMessageHistoryUseCase;
import com.cotalk.domain.port.inbound.message.MessageReplyForwardUseCase;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase;
import com.cotalk.domain.port.inbound.message.UpdateMessageUseCase;
import com.cotalk.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 채팅 메시지 관리를 위한 REST 컨트롤러.
 * <p>
 * 메시지 전송, 조회, 수정, 삭제, 답장, 전달 등의 기능을 제공합니다.
 * 인바운드 UseCase 포트만 의존하며, 아웃바운드 포트에 직접 접근하지 않습니다.
 *
 * @author seunggu.lee
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat/messages")
@RequiredArgsConstructor
@Tag(name = "채팅 메시지", description = "채팅 메시지 API")
public class ChatMessageController {

    private final SendMessageUseCase sendMessageUseCase;
    private final GetMessageHistoryUseCase getMessageHistoryUseCase;
    private final UpdateMessageUseCase updateMessageUseCase;
    private final DeleteMessageUseCase deleteMessageUseCase;
    private final MessageReplyForwardUseCase messageReplyForwardUseCase;
    private final GetMediaGalleryUseCase getMediaGalleryUseCase;

    /**
     * 채팅방에 텍스트 메시지를 전송합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param request 메시지 전송 요청 (채팅방 ID, 내용)
     * @return 전송된 메시지 정보
     */
    @Operation(summary = "메시지 전송", description = "채팅방에 메시지를 전송합니다.")
    @PostMapping
    public ResponseEntity<SendMessageResponse> sendMessage(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody SendMessageRequest request) {
        Message message = sendMessageUseCase.sendTextMessageAndBroadcast(request.chatRoomId(), principal.getUserId(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SendMessageResponse.from(message));
    }

    /**
     * 채팅방에 파일 또는 이미지를 전송합니다.
     * 발신자 ID는 JWT 토큰에서 자동으로 추출됩니다.
     * 저장 후 WebSocket으로 브로드캐스트하여 실시간 전달합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param request 파일 메시지 전송 요청
     * @return 전송된 메시지 정보
     */
    @Operation(summary = "파일/이미지 메시지 전송", description = "채팅방에 파일 또는 이미지를 전송합니다.")
    @PostMapping("/file")
    public ResponseEntity<SendMessageResponse> sendFileMessage(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody SendFileMessageRequest request) {
        SendMessageUseCase.FileMessageCommand command = new SendMessageUseCase.FileMessageCommand(
                request.fileUrl(),
                request.fileName(),
                request.fileSize(),
                request.contentType(),
                request.thumbnailUrl()
        );

        // senderId는 JWT 토큰에서 추출 (요청의 senderId는 무시)
        // 메시지 저장 + WebSocket 브로드캐스트를 서비스 내부에서 처리
        Message message = sendMessageUseCase.sendFileMessageAndBroadcast(
                request.chatRoomId(), principal.getUserId(), command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SendMessageResponse.from(message));
    }

    /**
     * 채팅방의 메시지 히스토리를 커서 기반으로 조회합니다.
     *
     * @param principal       인증된 사용자 정보
     * @param roomId          채팅방 ID
     * @param beforeMessageId 이 메시지 ID 이전의 메시지 조회 (커서)
     * @param size            조회할 메시지 수 (기본값: 20)
     * @return 메시지 히스토리
     */
    @Operation(summary = "메시지 히스토리 조회", description = "채팅방의 메시지 히스토리를 커서 기반으로 조회합니다.")
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<MessageHistoryResponse> getMessageHistory(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long beforeMessageId,
            @RequestParam(defaultValue = "20") int size) {
        GetMessageHistoryUseCase.EnrichedMessageHistoryResult result =
                getMessageHistoryUseCase.getEnrichedMessageHistory(roomId, principal.getUserId(), beforeMessageId, size);

        List<MessageDto> messageDtos = result.messages().stream()
                .map(enriched -> MessageDto.from(
                        enriched.message(),
                        enriched.unreadCount(),
                        enriched.senderNickname(),
                        enriched.senderAvatarUrl()))
                .toList();

        return ResponseEntity.ok(MessageHistoryResponse.of(messageDtos, result.nextCursor(), result.hasMore()));
    }

    /**
     * 채팅방의 미디어 갤러리를 조회합니다.
     * 사진, 파일, 링크를 타입별로 페이징하여 반환합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param roomId 채팅방 ID
     * @param type 미디어 유형 (PHOTO, FILE, LINK)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기 (기본값: 30)
     * @return 미디어 갤러리 응답
     */
    @Operation(summary = "미디어 갤러리 조회", description = "채팅방의 사진, 파일, 링크를 조회합니다.")
    @GetMapping("/rooms/{roomId}/media")
    public ResponseEntity<MediaGalleryResponse> getMediaGallery(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long roomId,
            @RequestParam String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {

        GetMediaGalleryUseCase.MediaGalleryResult result =
                getMediaGalleryUseCase.getMediaGallery(roomId, principal.getUserId(), type, page, size);

        List<MediaGalleryResponse.MediaGalleryItem> items = result.items().stream()
                .map(item -> new MediaGalleryResponse.MediaGalleryItem(
                        item.messageId(),
                        item.type(),
                        item.fileUrl(),
                        item.fileName(),
                        item.fileSize(),
                        item.fileContentType(),
                        item.thumbnailUrl(),
                        item.linkPreviewUrl(),
                        item.linkPreviewTitle(),
                        item.linkPreviewDescription(),
                        item.linkPreviewImageUrl(),
                        item.createdAtMillis(),
                        item.senderId(),
                        item.senderNickname()
                ))
                .toList();

        return ResponseEntity.ok(new MediaGalleryResponse(items, result.nextCursor(), result.hasMore()));
    }

    /**
     * 본인이 보낸 메시지를 수정합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param messageId 수정할 메시지 ID
     * @param request   메시지 수정 요청 (새 내용)
     * @return 수정된 메시지 정보
     */
    @Operation(summary = "메시지 수정", description = "본인이 보낸 메시지를 수정합니다.")
    @PutMapping("/{messageId}")
    public ResponseEntity<UpdateMessageResponse> updateMessage(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long messageId,
            @Valid @RequestBody UpdateMessageRequest request) {
        Message message = updateMessageUseCase.updateMessage(messageId, principal.getUserId(), request.content());
        return ResponseEntity.ok(UpdateMessageResponse.from(message));
    }

    /**
     * 본인이 보낸 메시지를 삭제합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param messageId 삭제할 메시지 ID
     * @return 처리 결과 메시지
     */
    @Operation(summary = "메시지 삭제", description = "본인이 보낸 메시지를 삭제합니다.")
    @DeleteMapping("/{messageId}")
    public ResponseEntity<MessageResponse> deleteMessage(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long messageId) {
        deleteMessageUseCase.deleteMessage(messageId, principal.getUserId());
        return ResponseEntity.ok(MessageResponse.of("메시지가 삭제되었습니다."));
    }

    /**
     * 특정 메시지에 답장합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param messageId 답장할 원본 메시지 ID
     * @param request   답장 요청 (답장 내용)
     * @return 전송된 답장 메시지 정보
     */
    @Operation(summary = "메시지 답장", description = "특정 메시지에 답장합니다.")
    @PostMapping("/{messageId}/reply")
    public ResponseEntity<SendMessageResponse> replyToMessage(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long messageId,
            @Valid @RequestBody ReplyMessageRequest request) {
        Message message = messageReplyForwardUseCase.replyToMessage(
                principal.getUserId(), messageId, request.content());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SendMessageResponse.from(message));
    }

    /**
     * 메시지를 다른 채팅방으로 전달합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param messageId 전달할 메시지 ID
     * @param request   전달 요청 (대상 채팅방 ID)
     * @return 전달된 메시지 정보
     */
    @Operation(summary = "메시지 전달", description = "메시지를 다른 채팅방으로 전달합니다.")
    @PostMapping("/{messageId}/forward")
    public ResponseEntity<SendMessageResponse> forwardMessage(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long messageId,
            @Valid @RequestBody ForwardMessageRequest request) {
        Message message = messageReplyForwardUseCase.forwardMessage(
                principal.getUserId(), messageId, request.targetChatRoomId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SendMessageResponse.from(message));
    }
}
