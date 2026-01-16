package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.port.inbound.AddMessageReactionUseCase;
import com.cotalk.domain.port.inbound.GetMessageReactionsUseCase;
import com.cotalk.domain.port.inbound.RemoveMessageReactionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat/messages/{messageId}/reactions")
@RequiredArgsConstructor
@Tag(name = "메시지 반응", description = "메시지 반응(이모지) API")
public class ChatReactionController {

    private final AddMessageReactionUseCase addMessageReactionUseCase;
    private final RemoveMessageReactionUseCase removeMessageReactionUseCase;
    private final GetMessageReactionsUseCase getMessageReactionsUseCase;

    @Operation(summary = "메시지 반응 추가", description = "메시지에 이모지 반응을 추가합니다.")
    @PostMapping
    public ResponseEntity<MessageReactionResponse> addReaction(
            @PathVariable Long messageId,
            @Valid @RequestBody AddReactionRequest request) {
        MessageReaction reaction = addMessageReactionUseCase.addReaction(messageId, request.userId(), request.emoji());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MessageReactionResponse.from(reaction));
    }

    @Operation(summary = "메시지 반응 제거", description = "메시지에서 이모지 반응을 제거합니다.")
    @DeleteMapping
    public ResponseEntity<RemoveReactionResponse> removeReaction(
            @PathVariable Long messageId,
            @Valid @RequestBody RemoveReactionRequest request) {
        removeMessageReactionUseCase.removeReaction(messageId, request.userId(), request.emoji());
        return ResponseEntity.ok(new RemoveReactionResponse("반응이 제거되었습니다."));
    }

    @Operation(summary = "메시지 반응 조회", description = "메시지의 모든 반응을 조회합니다.")
    @GetMapping
    public ResponseEntity<MessageReactionsResponse> getReactions(@PathVariable Long messageId) {
        List<MessageReaction> reactions = getMessageReactionsUseCase.getReactions(messageId);
        List<MessageReactionResponse> reactionDtos = reactions.stream()
                .map(MessageReactionResponse::from)
                .toList();
        return ResponseEntity.ok(new MessageReactionsResponse(reactionDtos));
    }

    // Request DTOs
    public record AddReactionRequest(
            @NotNull(message = "사용자 ID는 필수입니다.")
            Long userId,

            @NotBlank(message = "이모지는 필수입니다.")
            String emoji
    ) {}

    public record RemoveReactionRequest(
            @NotNull(message = "사용자 ID는 필수입니다.")
            Long userId,

            @NotBlank(message = "이모지는 필수입니다.")
            String emoji
    ) {}

    // Response DTOs
    public record MessageReactionResponse(
            Long reactionId,
            Long messageId,
            Long userId,
            String emoji,
            LocalDateTime createdAt
    ) {
        public static MessageReactionResponse from(MessageReaction reaction) {
            return new MessageReactionResponse(
                    reaction.getId(),
                    reaction.getMessageId(),
                    reaction.getUserId(),
                    reaction.getEmoji(),
                    reaction.getCreatedAt()
            );
        }
    }

    public record MessageReactionsResponse(List<MessageReactionResponse> reactions) {}
    public record RemoveReactionResponse(String message) {}
}
