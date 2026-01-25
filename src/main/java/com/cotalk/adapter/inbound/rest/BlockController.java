package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.block.BlockRequest;
import com.cotalk.adapter.inbound.rest.dto.block.BlockedUserDto;
import com.cotalk.adapter.inbound.rest.dto.block.BlockedUsersResponse;
import com.cotalk.adapter.inbound.rest.dto.common.MessageResponse;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.friend.BlockUserUseCase;
import com.cotalk.domain.port.inbound.friend.GetBlockedUsersUseCase;
import com.cotalk.domain.port.inbound.friend.UnblockUserUseCase;
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
 * 사용자 차단 관리를 위한 REST 컨트롤러.
 * <p>
 * 사용자 차단, 차단 해제, 차단 목록 조회 기능을 제공합니다.
 *
 * @author seunggu.lee
 */
@RestController
@RequestMapping("/api/v1/blocks")
@RequiredArgsConstructor
@Tag(name = "차단", description = "사용자 차단 관리 API")
public class BlockController {

    private final BlockUserUseCase blockUserUseCase;
    private final UnblockUserUseCase unblockUserUseCase;
    private final GetBlockedUsersUseCase getBlockedUsersUseCase;

    /**
     * 특정 사용자를 차단합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param request 차단 요청 (차단할 사용자 ID)
     * @return 처리 결과 메시지
     */
    @Operation(summary = "사용자 차단", description = "특정 사용자를 차단합니다.")
    @PostMapping
    public ResponseEntity<MessageResponse> blockUser(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody BlockRequest request) {
        blockUserUseCase.blockUser(principal.getUserId(), request.blockedId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MessageResponse.of("사용자를 차단했습니다."));
    }

    /**
     * 사용자 차단을 해제합니다.
     *
     * @param principal 인증된 사용자 정보
     * @param blockedId 차단된 사용자 ID
     * @return 처리 결과 메시지
     */
    @Operation(summary = "차단 해제", description = "사용자 차단을 해제합니다.")
    @DeleteMapping("/{blockedId}")
    public ResponseEntity<MessageResponse> unblockUser(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long blockedId) {
        unblockUserUseCase.unblockUser(principal.getUserId(), blockedId);
        return ResponseEntity.ok(MessageResponse.of("차단을 해제했습니다."));
    }

    /**
     * 차단한 사용자 목록을 조회합니다.
     *
     * @param principal 인증된 사용자 정보
     * @return 차단한 사용자 목록
     */
    @Operation(summary = "차단 목록 조회", description = "차단한 사용자 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<BlockedUsersResponse> getBlockedUsers(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        List<User> blockedUsers = getBlockedUsersUseCase.getBlockedUsers(principal.getUserId());
        List<BlockedUserDto> dtos = blockedUsers.stream()
                .map(BlockedUserDto::from)
                .toList();
        return ResponseEntity.ok(BlockedUsersResponse.of(dtos));
    }
}
