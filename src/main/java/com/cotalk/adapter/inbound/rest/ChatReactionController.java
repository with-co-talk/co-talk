package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.common.MessageResponse;
import com.cotalk.adapter.inbound.rest.dto.message.AddReactionRequest;
import com.cotalk.adapter.inbound.rest.dto.message.GroupedReactionResponse;
import com.cotalk.adapter.inbound.rest.dto.message.MessageReactionResponse;
import com.cotalk.adapter.inbound.rest.dto.message.RemoveReactionRequest;
import com.cotalk.application.service.message.GetMessageReactionsService;
import com.cotalk.domain.entity.MessageReaction;
import com.cotalk.domain.port.inbound.message.AddMessageReactionUseCase;
import com.cotalk.domain.port.inbound.message.RemoveMessageReactionUseCase;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 메시지 반응(이모지) 관리를 위한 REST 컨트롤러.
 * <p>
 * 메시지에 이모지 반응 추가, 제거, 조회 기능을 제공합니다.
 *
 * @author seunggu.lee
 */
@RestController
@RequestMapping("/api/v1/chat/messages/{messageId}/reactions")
@RequiredArgsConstructor
@Tag(name = "메시지 반응", description = "메시지 반응(이모지) API")
public class ChatReactionController {

    private final AddMessageReactionUseCase addMessageReactionUseCase;
    private final RemoveMessageReactionUseCase removeMessageReactionUseCase;
    private final GetMessageReactionsService getMessageReactionsService;

    /**
     * 메시지에 이모지 반응을 추가합니다.
     *
     * @param messageId 메시지 ID
     * @param principal 인증된 사용자 정보
     * @param request   반응 추가 요청 (이모지)
     * @return 추가된 반응 정보
     */
    @Operation(summary = "메시지 반응 추가", description = "메시지에 이모지 반응을 추가합니다.")
    @PostMapping
    public ResponseEntity<MessageReactionResponse> addReaction(
            @PathVariable Long messageId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody AddReactionRequest request) {
        MessageReaction reaction = addMessageReactionUseCase.addReaction(
                messageId, principal.getUserId(), request.emoji());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MessageReactionResponse.from(reaction));
    }

    /**
     * 메시지에서 이모지 반응을 제거합니다.
     *
     * @param messageId 메시지 ID
     * @param principal 인증된 사용자 정보
     * @param request   반응 제거 요청 (이모지)
     * @return 처리 결과 메시지
     */
    @Operation(summary = "메시지 반응 제거", description = "메시지에서 이모지 반응을 제거합니다.")
    @DeleteMapping
    public ResponseEntity<MessageResponse> removeReaction(
            @PathVariable Long messageId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody RemoveReactionRequest request) {
        removeMessageReactionUseCase.removeReaction(messageId, principal.getUserId(), request.emoji());
        return ResponseEntity.ok(MessageResponse.of("반응이 제거되었습니다."));
    }

    /**
     * 메시지의 모든 반응을 이모지별로 그룹핑하여 조회합니다.
     *
     * @param messageId 메시지 ID
     * @param principal 인증된 사용자 정보
     * @return 그룹핑된 메시지 반응 목록
     */
    @Operation(summary = "메시지 반응 조회", description = "메시지의 모든 반응을 이모지별로 그룹핑하여 조회합니다.")
    @GetMapping
    public ResponseEntity<List<GroupedReactionResponse>> getReactions(
            @PathVariable Long messageId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        List<GroupedReactionResponse> groupedReactions =
                getMessageReactionsService.getGroupedReactions(messageId, principal.getUserId());
        return ResponseEntity.ok(groupedReactions);
    }
}
