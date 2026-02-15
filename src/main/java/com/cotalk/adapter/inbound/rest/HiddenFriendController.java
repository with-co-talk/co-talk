package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.friend.HiddenFriendDto;
import com.cotalk.adapter.inbound.rest.dto.friend.HiddenFriendsResponse;
import com.cotalk.domain.model.HiddenFriendInfo;
import com.cotalk.domain.port.inbound.friend.GetHiddenFriendsUseCase;
import com.cotalk.domain.port.inbound.friend.HideFriendUseCase;
import com.cotalk.domain.port.inbound.friend.UnhideFriendUseCase;
import com.cotalk.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 숨긴 친구 관리를 위한 REST 컨트롤러.
 * <p>
 * 친구 숨김, 숨김 해제, 숨긴 친구 목록 조회 기능을 제공합니다.
 *
 * @author seunggu.lee
 */
@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
@Tag(name = "숨긴 친구", description = "친구 숨김 관리 API")
public class HiddenFriendController {

    private final HideFriendUseCase hideFriendUseCase;
    private final UnhideFriendUseCase unhideFriendUseCase;
    private final GetHiddenFriendsUseCase getHiddenFriendsUseCase;

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
        List<HiddenFriendInfo> hiddenFriends = getHiddenFriendsUseCase.getHiddenFriends(principal.getUserId());
        List<HiddenFriendDto> paginatedFriends = hiddenFriends.stream()
                .skip((long) page * safeSize)
                .limit(safeSize)
                .map(info -> HiddenFriendDto.builder()
                        .id(info.id())
                        .friendId(info.friendId())
                        .nickname(info.nickname())
                        .profileImageUrl(info.profileImageUrl())
                        .hiddenAt(info.hiddenAt())
                        .build())
                .toList();
        return ResponseEntity.ok(HiddenFriendsResponse.of(paginatedFriends));
    }

}
