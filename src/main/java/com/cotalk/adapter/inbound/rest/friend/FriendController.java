package com.cotalk.adapter.inbound.rest.friend;

import com.cotalk.application.dto.request.SendFriendRequestRequest;
import com.cotalk.application.dto.response.ApiResponse;
import com.cotalk.application.dto.response.FriendRequestResponse;
import com.cotalk.application.dto.response.UserResponse;
import com.cotalk.application.service.friend.*;
import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.outbound.persistence.FriendRequestRepository;
import com.cotalk.domain.port.outbound.persistence.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 친구 REST API 컨트롤러.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final SendFriendRequestService sendFriendRequestService;
    private final AcceptFriendRequestService acceptFriendRequestService;
    private final RejectFriendRequestService rejectFriendRequestService;
    private final GetFriendListService getFriendListService;
    private final GetFriendRequestListService getFriendRequestListService;
    private final DeleteFriendService deleteFriendService;
    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;

    /**
     * 친구 요청 전송
     */
    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<Void>> sendFriendRequest(
            @Valid @RequestBody SendFriendRequestRequest request,
            Authentication authentication) {
        UUID requesterId = (UUID) authentication.getPrincipal();
        sendFriendRequestService.sendFriendRequest(requesterId, request.getReceiverId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("친구 요청이 전송되었습니다.", null));
    }

    /**
     * 친구 요청 수락
     */
    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<UUID>> acceptFriendRequest(
            @PathVariable UUID requestId,
            Authentication authentication) {
        UUID receiverId = (UUID) authentication.getPrincipal();
        UUID chatRoomId = acceptFriendRequestService.acceptFriendRequest(receiverId, requestId);
        return ResponseEntity.ok(ApiResponse.success("친구 요청이 수락되었습니다.", chatRoomId));
    }

    /**
     * 친구 요청 거절
     */
    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectFriendRequest(
            @PathVariable UUID requestId,
            Authentication authentication) {
        UUID receiverId = (UUID) authentication.getPrincipal();
        rejectFriendRequestService.rejectFriendRequest(receiverId, requestId);
        return ResponseEntity.ok(ApiResponse.success("친구 요청이 거절되었습니다.", null));
    }

    /**
     * 친구 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getFriendList(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<User> friends = getFriendListService.getFriendList(userId);
        List<UserResponse> responses = friends.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 친구 요청 목록 조회
     */
    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<List<FriendRequestResponse>>> getFriendRequestList(
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<FriendRequest> requests = getFriendRequestListService.getFriendRequestList(userId);
        List<FriendRequestResponse> responses = requests.stream()
                .map(this::toFriendRequestResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 친구 삭제
     */
    @DeleteMapping("/{friendId}")
    public ResponseEntity<ApiResponse<Void>> deleteFriend(
            @PathVariable UUID friendId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        deleteFriendService.deleteFriend(userId, friendId);
        return ResponseEntity.ok(ApiResponse.success("친구가 삭제되었습니다.", null));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl()
        );
    }

    private FriendRequestResponse toFriendRequestResponse(FriendRequest request) {
        User requester = userRepository.findById(request.getRequesterId())
                .orElse(null);
        UserResponse requesterResponse = requester != null ? toUserResponse(requester) : null;
        
        return new FriendRequestResponse(
                request.getId(),
                request.getRequesterId(),
                requesterResponse,
                request.getStatus().name(),
                request.getCreatedAt()
        );
    }
}
