package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.AcceptFriendRequestUseCase;
import com.cotalk.domain.port.inbound.GetFriendListUseCase;
import com.cotalk.domain.port.inbound.RejectFriendRequestUseCase;
import com.cotalk.domain.port.inbound.RemoveFriendUseCase;
import com.cotalk.domain.port.inbound.SendFriendRequestUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
@Tag(name = "친구", description = "친구 요청 및 목록 관리 API")
public class FriendController {

    private final SendFriendRequestUseCase sendFriendRequestUseCase;
    private final AcceptFriendRequestUseCase acceptFriendRequestUseCase;
    private final RejectFriendRequestUseCase rejectFriendRequestUseCase;
    private final RemoveFriendUseCase removeFriendUseCase;
    private final GetFriendListUseCase getFriendListUseCase;

    @Operation(summary = "친구 요청 전송", description = "다른 사용자에게 친구 요청을 보냅니다.")
    @PostMapping("/requests")
    public ResponseEntity<SendFriendRequestResponse> sendFriendRequest(
            @Valid @RequestBody SendFriendRequestRequest request) {
        Long requestId = sendFriendRequestUseCase.sendFriendRequest(request.requesterId(), request.receiverId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SendFriendRequestResponse(requestId, "친구 요청이 전송되었습니다."));
    }

    @Operation(summary = "친구 요청 수락", description = "받은 친구 요청을 수락합니다.")
    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<AcceptFriendRequestResponse> acceptFriendRequest(
            @PathVariable Long requestId,
            @RequestParam Long userId) {
        acceptFriendRequestUseCase.acceptFriendRequest(userId, requestId);
        return ResponseEntity.ok(new AcceptFriendRequestResponse("친구 요청을 수락했습니다."));
    }

    @Operation(summary = "친구 요청 거절", description = "받은 친구 요청을 거절합니다.")
    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<RejectFriendRequestResponse> rejectFriendRequest(
            @PathVariable Long requestId,
            @RequestParam Long userId) {
        rejectFriendRequestUseCase.rejectFriendRequest(userId, requestId);
        return ResponseEntity.ok(new RejectFriendRequestResponse("친구 요청을 거절했습니다."));
    }

    @Operation(summary = "친구 목록 조회", description = "사용자의 친구 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<FriendListResponse> getFriendList(@RequestParam Long userId) {
        List<User> friends = getFriendListUseCase.getFriendList(userId);
        List<FriendDto> friendDtos = friends.stream()
                .map(u -> new FriendDto(u.getId(), u.getNickname(), u.getEmail()))
                .toList();
        return ResponseEntity.ok(new FriendListResponse(friendDtos));
    }

    @Operation(summary = "친구 삭제", description = "친구 관계를 삭제합니다.")
    @DeleteMapping("/{friendId}")
    public ResponseEntity<RemoveFriendResponse> removeFriend(
            @PathVariable Long friendId,
            @RequestParam Long userId) {
        removeFriendUseCase.removeFriend(userId, friendId);
        return ResponseEntity.ok(new RemoveFriendResponse("친구가 삭제되었습니다."));
    }

    // Request DTOs
    public record SendFriendRequestRequest(
            @NotNull(message = "요청자 ID는 필수입니다.")
            Long requesterId,

            @NotNull(message = "수신자 ID는 필수입니다.")
            Long receiverId
    ) {}

    // Response DTOs
    public record SendFriendRequestResponse(Long requestId, String message) {}
    public record AcceptFriendRequestResponse(String message) {}
    public record RejectFriendRequestResponse(String message) {}
    public record RemoveFriendResponse(String message) {}
    public record FriendListResponse(List<FriendDto> friends) {}
    public record FriendDto(Long id, String nickname, String email) {}
}
