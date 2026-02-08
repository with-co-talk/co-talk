package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.common.MessageResponse;
import com.cotalk.adapter.inbound.rest.dto.friend.FriendDto;
import com.cotalk.adapter.inbound.rest.dto.friend.FriendListResponse;
import com.cotalk.adapter.inbound.rest.dto.friend.FriendRequestDto;
import com.cotalk.adapter.inbound.rest.dto.friend.FriendRequestListResponse;
import com.cotalk.adapter.inbound.rest.dto.friend.HiddenFriendDto;
import com.cotalk.adapter.inbound.rest.dto.friend.HiddenFriendsResponse;
import com.cotalk.adapter.inbound.rest.dto.friend.SendFriendRequestRequest;
import com.cotalk.adapter.inbound.rest.dto.friend.SendFriendRequestResponse;
import com.cotalk.adapter.inbound.rest.dto.user.UserDto;
import com.cotalk.domain.entity.FriendRequest;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.friend.AcceptFriendRequestUseCase;
import com.cotalk.domain.port.inbound.friend.GetFriendListUseCase;
import com.cotalk.domain.port.inbound.friend.GetHiddenFriendsUseCase;
import com.cotalk.domain.port.inbound.friend.GetReceivedFriendRequestsUseCase;
import com.cotalk.domain.port.inbound.friend.GetSentFriendRequestsUseCase;
import com.cotalk.domain.port.inbound.friend.HideFriendUseCase;
import com.cotalk.domain.port.inbound.friend.RejectFriendRequestUseCase;
import com.cotalk.domain.port.inbound.friend.RemoveFriendUseCase;
import com.cotalk.domain.port.inbound.friend.SendFriendRequestUseCase;
import com.cotalk.domain.port.inbound.friend.UnhideFriendUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
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
    private final HideFriendUseCase hideFriendUseCase;
    private final UnhideFriendUseCase unhideFriendUseCase;
    private final GetHiddenFriendsUseCase getHiddenFriendsUseCase;
    private final UserRepository userRepository;

    /**
     * 다른 사용자에게 친구 요청을 보냅니다.
     *
     * @param principal 인증된 사용자 정보
     * @param request   친구 요청 전송 요청 (수신자 ID)
     * @return 생성된 친구 요청 정보
     */
    @Operation(summary = "친구 요청 전송", description = "다른 사용자에게 친구 요청을 보냅니다.")
    @PostMapping("/requests")
    public ResponseEntity<SendFriendRequestResponse> sendFriendRequest(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody SendFriendRequestRequest request) {
        Long requestId = sendFriendRequestUseCase.sendFriendRequest(principal.getUserId(), request.receiverId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SendFriendRequestResponse.of(requestId, "친구 요청이 전송되었습니다."));
    }

    /**
     * 받은 친구 요청을 수락합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param requestId 친구 요청 ID
     * @return 처리 결과 메시지
     */
    @Operation(summary = "친구 요청 수락", description = "받은 친구 요청을 수락합니다.")
    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<MessageResponse> acceptFriendRequest(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long requestId) {
        acceptFriendRequestUseCase.acceptFriendRequest(principal.getUserId(), requestId);
        return ResponseEntity.ok(MessageResponse.of("친구 요청을 수락했습니다."));
    }

    /**
     * 받은 친구 요청을 거절합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param requestId 친구 요청 ID
     * @return 처리 결과 메시지
     */
    @Operation(summary = "친구 요청 거절", description = "받은 친구 요청을 거절합니다.")
    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<MessageResponse> rejectFriendRequest(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long requestId) {
        rejectFriendRequestUseCase.rejectFriendRequest(principal.getUserId(), requestId);
        return ResponseEntity.ok(MessageResponse.of("친구 요청을 거절했습니다."));
    }

    /**
     * 사용자의 친구 목록을 조회합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20, 최대: 100)
     * @return 친구 목록
     */
    @Operation(summary = "친구 목록 조회", description = "사용자의 친구 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<FriendListResponse> getFriendList(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, 100);
        List<User> friends = getFriendListUseCase.getFriendList(principal.getUserId());
        List<FriendDto> friendDtos = friends.stream()
                .skip((long) page * safeSize)
                .limit(safeSize)
                .map(FriendDto::from)
                .toList();
        return ResponseEntity.ok(FriendListResponse.of(friendDtos));
    }

    /**
     * 친구 관계를 삭제합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param friendId 삭제할 친구의 사용자 ID
     * @return 처리 결과 메시지
     */
    @Operation(summary = "친구 삭제", description = "친구 관계를 삭제합니다.")
    @DeleteMapping("/{friendId}")
    public ResponseEntity<MessageResponse> removeFriend(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long friendId) {
        removeFriendUseCase.removeFriend(principal.getUserId(), friendId);
        return ResponseEntity.ok(MessageResponse.of("친구가 삭제되었습니다."));
    }

    /**
     * 받은 친구 요청 목록을 조회합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20, 최대: 100)
     * @return 받은 친구 요청 목록
     */
    @Operation(summary = "받은 친구 요청 목록 조회", description = "사용자가 받은 대기 중인 친구 요청 목록을 조회합니다.")
    @GetMapping("/requests/received")
    public ResponseEntity<FriendRequestListResponse> getReceivedFriendRequests(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, 100);
        Long userId = principal.getUserId();
        List<FriendRequest> requests = getReceivedFriendRequestsUseCase.getReceivedFriendRequests(userId);

        // 빈 리스트일 때 early return
        if (requests.isEmpty()) {
            return ResponseEntity.ok(FriendRequestListResponse.of(List.of()));
        }

        // 현재 사용자(수신자) 정보 조회 (한 번만)
        User receiver = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

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
                .skip((long) page * safeSize)
                .limit(safeSize)
                .map(request -> {
                    User requester = requesterMap.get(request.getRequesterId());
                    if (requester == null) {
                        throw new UserNotFoundException(request.getRequesterId());
                    }

                    LocalDateTime createdAt = request.getCreatedAt();
                    if (createdAt == null) {
                        throw new IllegalStateException("친구 요청의 생성 시간이 없습니다.");
                    }

                    return FriendRequestDto.of(
                            request.getId(),
                            UserDto.from(requester),
                            UserDto.from(receiver),
                            request.getStatus().name(),
                            createdAt
                    );
                })
                .toList();

        return ResponseEntity.ok(FriendRequestListResponse.of(requestDtos));
    }

    /**
     * 보낸 친구 요청 목록을 조회합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20, 최대: 100)
     * @return 보낸 친구 요청 목록
     */
    @Operation(summary = "보낸 친구 요청 목록 조회", description = "사용자가 보낸 대기 중인 친구 요청 목록을 조회합니다.")
    @GetMapping("/requests/sent")
    public ResponseEntity<FriendRequestListResponse> getSentFriendRequests(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, 100);
        Long userId = principal.getUserId();
        List<FriendRequest> requests = getSentFriendRequestsUseCase.getSentFriendRequests(userId);

        // 빈 리스트일 때 early return
        if (requests.isEmpty()) {
            return ResponseEntity.ok(FriendRequestListResponse.of(List.of()));
        }

        // 현재 사용자(요청자) 정보 조회 (한 번만)
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 수신자 ID 목록 추출
        List<Long> receiverIds = requests.stream()
                .map(FriendRequest::getReceiverId)
                .distinct()
                .toList();

        // 수신자 정보 일괄 조회
        Map<Long, User> receiverMap = userRepository.findAllById(receiverIds)
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // DTO 변환
        List<FriendRequestDto> requestDtos = requests.stream()
                .skip((long) page * safeSize)
                .limit(safeSize)
                .map(request -> {
                    User receiver = receiverMap.get(request.getReceiverId());
                    if (receiver == null) {
                        throw new UserNotFoundException(request.getReceiverId());
                    }

                    LocalDateTime createdAt = request.getCreatedAt();
                    if (createdAt == null) {
                        throw new IllegalStateException("친구 요청의 생성 시간이 없습니다.");
                    }

                    return FriendRequestDto.of(
                            request.getId(),
                            UserDto.from(requester),
                            UserDto.from(receiver),
                            request.getStatus().name(),
                            createdAt
                    );
                })
                .toList();

        return ResponseEntity.ok(FriendRequestListResponse.of(requestDtos));
    }

    /**
     * 친구를 숨김 처리합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param friendId 숨길 친구의 사용자 ID
     * @return 처리 결과
     */
    @Operation(summary = "친구 숨기기", description = "친구를 숨김 처리합니다. 숨긴 친구는 친구 목록에서 보이지 않습니다.")
    @PostMapping("/{friendId}/hide")
    public ResponseEntity<Void> hideFriend(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long friendId) {
        hideFriendUseCase.hideFriend(principal.getUserId(), friendId);
        return ResponseEntity.ok().build();
    }

    /**
     * 숨긴 친구를 다시 표시합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param friendId 숨김 해제할 친구의 사용자 ID
     * @return 처리 결과
     */
    @Operation(summary = "친구 숨김 해제", description = "숨긴 친구를 다시 표시합니다.")
    @DeleteMapping("/{friendId}/hide")
    public ResponseEntity<Void> unhideFriend(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long friendId) {
        unhideFriendUseCase.unhideFriend(principal.getUserId(), friendId);
        return ResponseEntity.ok().build();
    }

    /**
     * 숨긴 친구 목록을 조회합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20, 최대: 100)
     * @return 숨긴 친구 목록
     */
    @Operation(summary = "숨긴 친구 목록 조회", description = "사용자가 숨긴 친구 목록을 조회합니다.")
    @GetMapping("/hidden")
    public ResponseEntity<HiddenFriendsResponse> getHiddenFriends(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, 100);
        List<HiddenFriendDto> hiddenFriends = getHiddenFriendsUseCase.getHiddenFriends(principal.getUserId());
        List<HiddenFriendDto> paginatedFriends = hiddenFriends.stream()
                .skip((long) page * safeSize)
                .limit(safeSize)
                .toList();
        return ResponseEntity.ok(HiddenFriendsResponse.of(paginatedFriends));
    }

}
