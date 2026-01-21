package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.common.MessageResponse;
import com.cotalk.adapter.inbound.rest.dto.friend.FriendDto;
import com.cotalk.adapter.inbound.rest.dto.friend.FriendListResponse;
import com.cotalk.adapter.inbound.rest.dto.friend.FriendRequestDto;
import com.cotalk.adapter.inbound.rest.dto.friend.FriendRequestListResponse;
import com.cotalk.adapter.inbound.rest.dto.friend.SendFriendRequestRequest;
import com.cotalk.adapter.inbound.rest.dto.friend.SendFriendRequestResponse;
import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.UnauthorizedException;
import com.cotalk.domain.port.inbound.friend.AcceptFriendRequestUseCase;
import com.cotalk.domain.port.inbound.friend.GetFriendListUseCase;
import com.cotalk.domain.port.inbound.friend.GetReceivedFriendRequestsUseCase;
import com.cotalk.domain.port.inbound.friend.GetSentFriendRequestsUseCase;
import com.cotalk.domain.port.inbound.friend.RejectFriendRequestUseCase;
import com.cotalk.domain.port.inbound.friend.RemoveFriendUseCase;
import com.cotalk.domain.port.inbound.friend.SendFriendRequestUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.security.SecurityContextHelper;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 친구 관리를 위한 REST 컨트롤러.
 * <p>
 * 친구 요청 전송, 수락, 거절, 친구 목록 조회, 친구 삭제 등의 기능을 제공합니다.
 *
 * @author seunggu.lee
 */
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
    private final GetReceivedFriendRequestsUseCase getReceivedFriendRequestsUseCase;
    private final GetSentFriendRequestsUseCase getSentFriendRequestsUseCase;
    private final UserRepository userRepository;
    private final SecurityContextHelper securityContextHelper;

    /**
     * 다른 사용자에게 친구 요청을 보냅니다.
     *
     * @param request 친구 요청 전송 요청 (요청자 ID, 수신자 ID)
     * @return 생성된 친구 요청 정보
     */
    @Operation(summary = "친구 요청 전송", description = "다른 사용자에게 친구 요청을 보냅니다.")
    @PostMapping("/requests")
    public ResponseEntity<SendFriendRequestResponse> sendFriendRequest(
            @Valid @RequestBody SendFriendRequestRequest request) {
        Long requestId = sendFriendRequestUseCase.sendFriendRequest(request.requesterId(), request.receiverId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SendFriendRequestResponse.of(requestId, "친구 요청이 전송되었습니다."));
    }

    /**
     * 받은 친구 요청을 수락합니다.
     *
     * @param requestId 친구 요청 ID
     * @param userId    요청 수락자 ID
     * @return 처리 결과 메시지
     */
    @Operation(summary = "친구 요청 수락", description = "받은 친구 요청을 수락합니다.")
    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<MessageResponse> acceptFriendRequest(
            @PathVariable Long requestId,
            @RequestParam Long userId) {
        validateUserAccess(userId);
        acceptFriendRequestUseCase.acceptFriendRequest(userId, requestId);
        return ResponseEntity.ok(MessageResponse.of("친구 요청을 수락했습니다."));
    }

    /**
     * 받은 친구 요청을 거절합니다.
     *
     * @param requestId 친구 요청 ID
     * @param userId    요청 거절자 ID
     * @return 처리 결과 메시지
     */
    @Operation(summary = "친구 요청 거절", description = "받은 친구 요청을 거절합니다.")
    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<MessageResponse> rejectFriendRequest(
            @PathVariable Long requestId,
            @RequestParam Long userId) {
        validateUserAccess(userId);
        rejectFriendRequestUseCase.rejectFriendRequest(userId, requestId);
        return ResponseEntity.ok(MessageResponse.of("친구 요청을 거절했습니다."));
    }

    /**
     * 사용자의 친구 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 친구 목록
     */
    @Operation(summary = "친구 목록 조회", description = "사용자의 친구 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<FriendListResponse> getFriendList(@RequestParam Long userId) {
        validateUserAccess(userId);
        List<User> friends = getFriendListUseCase.getFriendList(userId);
        List<FriendDto> friendDtos = friends.stream()
                .map(FriendDto::from)
                .toList();
        return ResponseEntity.ok(FriendListResponse.of(friendDtos));
    }

    /**
     * 친구 관계를 삭제합니다.
     *
     * @param friendId 삭제할 친구의 사용자 ID
     * @param userId   요청자 ID
     * @return 처리 결과 메시지
     */
    @Operation(summary = "친구 삭제", description = "친구 관계를 삭제합니다.")
    @DeleteMapping("/{friendId}")
    public ResponseEntity<MessageResponse> removeFriend(
            @PathVariable Long friendId,
            @RequestParam Long userId) {
        validateUserAccess(userId);
        removeFriendUseCase.removeFriend(userId, friendId);
        return ResponseEntity.ok(MessageResponse.of("친구가 삭제되었습니다."));
    }

    /**
     * 받은 친구 요청 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 받은 친구 요청 목록
     */
    @Operation(summary = "받은 친구 요청 목록 조회", description = "사용자가 받은 대기 중인 친구 요청 목록을 조회합니다.")
    @GetMapping("/requests/received")
    public ResponseEntity<FriendRequestListResponse> getReceivedFriendRequests(@RequestParam Long userId) {
        validateUserAccess(userId);
        List<FriendRequest> requests = getReceivedFriendRequestsUseCase.getReceivedFriendRequests(userId);
        
        // 요청자 ID 목록 추출
        List<Long> requesterIds = requests.stream()
                .map(FriendRequest::getRequesterId)
                .distinct()
                .toList();
        
        // 요청자 정보 일괄 조회
        Map<Long, User> requesterMap = userRepository.findAllById(requesterIds)
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        
        // DTO 변환
        List<FriendRequestDto> requestDtos = requests.stream()
                .map(request -> {
                    User requester = requesterMap.get(request.getRequesterId());
                    User receiver = userRepository.findById(userId)
                            .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다."));
                    
                    return FriendRequestDto.of(
                            request.getId(),
                            FriendDto.from(requester),
                            FriendDto.from(receiver),
                            request.getStatus().name(),
                            request.getCreatedAt()
                    );
                })
                .toList();
        
        return ResponseEntity.ok(FriendRequestListResponse.of(requestDtos));
    }

    /**
     * 보낸 친구 요청 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 보낸 친구 요청 목록
     */
    @Operation(summary = "보낸 친구 요청 목록 조회", description = "사용자가 보낸 대기 중인 친구 요청 목록을 조회합니다.")
    @GetMapping("/requests/sent")
    public ResponseEntity<FriendRequestListResponse> getSentFriendRequests(@RequestParam Long userId) {
        validateUserAccess(userId);
        List<FriendRequest> requests = getSentFriendRequestsUseCase.getSentFriendRequests(userId);
        
        // 수신자 ID 목록 추출
        List<Long> receiverIds = requests.stream()
                .map(FriendRequest::getReceiverId)
                .distinct()
                .toList();
        
        // 수신자 정보 일괄 조회
        Map<Long, User> receiverMap = userRepository.findAllById(receiverIds)
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        
        // 현재 사용자 정보 조회
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다."));
        
        // DTO 변환
        List<FriendRequestDto> requestDtos = requests.stream()
                .map(request -> {
                    User receiver = receiverMap.get(request.getReceiverId());
                    
                    return FriendRequestDto.of(
                            request.getId(),
                            FriendDto.from(requester),
                            FriendDto.from(receiver),
                            request.getStatus().name(),
                            request.getCreatedAt()
                    );
                })
                .toList();
        
        return ResponseEntity.ok(FriendRequestListResponse.of(requestDtos));
    }

    /**
     * 요청된 userId가 현재 인증된 사용자의 ID와 일치하는지 검증합니다.
     *
     * @param userId 검증할 사용자 ID
     * @throws UnauthorizedException 사용자 ID가 일치하지 않는 경우
     */
    private void validateUserAccess(Long userId) {
        Long currentUserId = securityContextHelper.getCurrentUserId();
        if (!currentUserId.equals(userId)) {
            throw new UnauthorizedException("자신의 리소스만 접근할 수 있습니다.");
        }
    }
}
