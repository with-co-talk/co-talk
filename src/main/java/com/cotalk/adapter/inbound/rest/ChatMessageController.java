package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.common.MessageResponse;
import com.cotalk.adapter.inbound.rest.dto.message.ForwardMessageRequest;
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
import com.cotalk.domain.port.inbound.message.GetMessageHistoryUseCase;
import com.cotalk.domain.port.inbound.message.MessageReplyForwardUseCase;
import com.cotalk.domain.port.inbound.message.SendMessageUseCase;
import com.cotalk.domain.port.inbound.message.UpdateMessageUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 채팅 메시지 관리를 위한 REST 컨트롤러.
 * <p>
 * 메시지 전송, 조회, 수정, 삭제, 답장, 전달 등의 기능을 제공합니다.
 *
 * @author seunggu.lee
 */
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

    /**
     * 채팅방에 텍스트 메시지를 전송합니다.
     *
     * @param request 메시지 전송 요청 (발신자 ID, 채팅방 ID, 내용)
     * @return 전송된 메시지 정보
     */
    @Operation(summary = "메시지 전송", description = "채팅방에 메시지를 전송합니다.")
    @PostMapping
    public ResponseEntity<SendMessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        Message message = sendMessageUseCase.sendMessage(request.chatRoomId(), request.senderId(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SendMessageResponse.from(message));
    }

    /**
     * 채팅방에 파일 또는 이미지를 전송합니다.
     *
     * @param request 파일 메시지 전송 요청
     * @return 전송된 메시지 정보
     */
    @Operation(summary = "파일/이미지 메시지 전송", description = "채팅방에 파일 또는 이미지를 전송합니다.")
    @PostMapping("/file")
    public ResponseEntity<SendMessageResponse> sendFileMessage(@Valid @RequestBody SendFileMessageRequest request) {
        SendMessageUseCase.FileMessageCommand command = new SendMessageUseCase.FileMessageCommand(
                request.fileUrl(),
                request.fileName(),
                request.fileSize(),
                request.contentType(),
                request.thumbnailUrl()
        );

        Message message = sendMessageUseCase.sendFileMessage(request.chatRoomId(), request.senderId(), command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SendMessageResponse.from(message));
    }

    /**
     * 채팅방의 메시지 히스토리를 커서 기반으로 조회합니다.
     *
     * @param roomId          채팅방 ID
     * @param userId          요청 사용자 ID
     * @param beforeMessageId 이 메시지 ID 이전의 메시지 조회 (커서)
     * @param size            조회할 메시지 수 (기본값: 20)
     * @return 메시지 히스토리
     */
    @Operation(summary = "메시지 히스토리 조회", description = "채팅방의 메시지 히스토리를 커서 기반으로 조회합니다.")
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<MessageHistoryResponse> getMessageHistory(
            @PathVariable Long roomId,
            @RequestParam Long userId,
            @RequestParam(required = false) Long beforeMessageId,
            @RequestParam(defaultValue = "20") int size) {
        List<Message> messages = getMessageHistoryUseCase.getMessageHistory(roomId, userId, beforeMessageId, size);
        List<MessageDto> messageDtos = messages.stream()
                .map(MessageDto::from)
                .toList();

        // 다음 페이지 존재 여부를 위한 nextCursor 계산
        Long nextCursor = messages.isEmpty() ? null : messages.get(messages.size() - 1).getId();
        boolean hasMore = messages.size() == size;

        return ResponseEntity.ok(MessageHistoryResponse.of(messageDtos, nextCursor, hasMore));
    }

    /**
     * 본인이 보낸 메시지를 수정합니다.
     *
     * @param messageId 수정할 메시지 ID
     * @param request   메시지 수정 요청 (사용자 ID, 새 내용)
     * @return 수정된 메시지 정보
     */
    @Operation(summary = "메시지 수정", description = "본인이 보낸 메시지를 수정합니다.")
    @PutMapping("/{messageId}")
    public ResponseEntity<UpdateMessageResponse> updateMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody UpdateMessageRequest request) {
        Message message = updateMessageUseCase.updateMessage(messageId, request.userId(), request.content());
        return ResponseEntity.ok(UpdateMessageResponse.from(message));
    }

    /**
     * 본인이 보낸 메시지를 삭제합니다.
     *
     * @param messageId 삭제할 메시지 ID
     * @param userId    요청 사용자 ID
     * @return 처리 결과 메시지
     */
    @Operation(summary = "메시지 삭제", description = "본인이 보낸 메시지를 삭제합니다.")
    @DeleteMapping("/{messageId}")
    public ResponseEntity<MessageResponse> deleteMessage(
            @PathVariable Long messageId,
            @RequestParam Long userId) {
        deleteMessageUseCase.deleteMessage(messageId, userId);
        return ResponseEntity.ok(MessageResponse.of("메시지가 삭제되었습니다."));
    }

    /**
     * 특정 메시지에 답장합니다.
     *
     * @param messageId 답장할 원본 메시지 ID
     * @param request   답장 요청 (발신자 ID, 답장 내용)
     * @return 전송된 답장 메시지 정보
     */
    @Operation(summary = "메시지 답장", description = "특정 메시지에 답장합니다.")
    @PostMapping("/{messageId}/reply")
    public ResponseEntity<SendMessageResponse> replyToMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody ReplyMessageRequest request) {
        Message message = messageReplyForwardUseCase.replyToMessage(
                request.senderId(), messageId, request.content());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SendMessageResponse.from(message));
    }

    /**
     * 메시지를 다른 채팅방으로 전달합니다.
     *
     * @param messageId 전달할 메시지 ID
     * @param request   전달 요청 (발신자 ID, 대상 채팅방 ID)
     * @return 전달된 메시지 정보
     */
    @Operation(summary = "메시지 전달", description = "메시지를 다른 채팅방으로 전달합니다.")
    @PostMapping("/{messageId}/forward")
    public ResponseEntity<SendMessageResponse> forwardMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody ForwardMessageRequest request) {
        Message message = messageReplyForwardUseCase.forwardMessage(
                request.senderId(), messageId, request.targetChatRoomId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SendMessageResponse.from(message));
    }
}
