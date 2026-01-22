package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.common.MessageResponse;
import com.cotalk.adapter.inbound.rest.dto.user.SearchUserResponse;
import com.cotalk.adapter.inbound.rest.dto.user.UpdateOnlineStatusRequest;
import com.cotalk.adapter.inbound.rest.dto.user.UpdateProfileRequest;
import com.cotalk.adapter.inbound.rest.dto.user.UserDto;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.exception.ResourceAccessDeniedException;
import com.cotalk.domain.exception.UserNotFoundException;
import com.cotalk.domain.port.inbound.user.SearchUserUseCase;
import com.cotalk.domain.port.inbound.user.UpdateProfileUseCase;
import com.cotalk.domain.port.inbound.user.UpdateUserOnlineStatusUseCase;
import com.cotalk.domain.port.outbound.UserRepository;
import com.cotalk.infrastructure.security.SecurityContextHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 사용자 관리를 위한 REST 컨트롤러.
 * 사용자 검색, 프로필 수정, 온라인 상태 관리 등의 기능을 제공한다.
 *
 * @author seunggu.lee
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "사용자", description = "사용자 검색 API")
public class UserController {

    private final SearchUserUseCase searchUserUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final UpdateUserOnlineStatusUseCase updateUserOnlineStatusUseCase;
    private final UserRepository userRepository;
    private final SecurityContextHelper securityContextHelper;

    /**
     * 현재 로그인한 사용자 정보를 조회한다.
     *
     * @return 현재 사용자 정보
     */
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        Long currentUserId = securityContextHelper.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));
        return ResponseEntity.ok(UserDto.from(user));
    }

    /**
     * 닉네임으로 사용자를 검색한다.
     * query 또는 nickname 파라미터를 사용할 수 있으며, query가 우선순위가 높다.
     *
     * @param query 검색할 키워드 (우선순위 높음)
     * @param nickname 검색할 닉네임 키워드 (query가 없을 때 사용)
     * @return 검색된 사용자 목록
     */
    @Operation(summary = "닉네임으로 사용자 검색", description = "닉네임에 포함된 키워드로 사용자를 검색합니다. query 또는 nickname 파라미터를 사용할 수 있습니다.")
    @GetMapping("/search")
    public ResponseEntity<SearchUserResponse> searchByNickname(
            @RequestParam(required = false)
            @Size(min = 1, max = 50, message = "검색어는 1자 이상 50자 이하여야 합니다.")
            String query,
            @RequestParam(required = false)
            @Size(min = 1, max = 50, message = "닉네임은 1자 이상 50자 이하여야 합니다.")
            String nickname) {
        // query 또는 nickname 중 하나는 필수
        String searchKeyword = (query != null && !query.isBlank()) ? query : nickname;
        if (searchKeyword == null || searchKeyword.isBlank()) {
            throw new IllegalArgumentException("query 또는 nickname 파라미터 중 하나는 필수입니다.");
        }
        
        List<User> users = searchUserUseCase.searchByNickname(searchKeyword);
        List<UserDto> userDtos = users.stream()
                .map(UserDto::from)
                .toList();
        return ResponseEntity.ok(SearchUserResponse.of(userDtos));
    }

    /**
     * 사용자 프로필을 수정한다.
     *
     * @param userId  수정할 사용자 ID
     * @param request 프로필 수정 요청 정보 (닉네임, 아바타 URL)
     * @return 수정 완료 메시지
     */
    @Operation(summary = "프로필 수정", description = "사용자 프로필(닉네임, 아바타)을 수정합니다.")
    @PutMapping("/{userId}/profile")
    public ResponseEntity<MessageResponse> updateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        validateUserAccess(userId);
        updateProfileUseCase.updateProfile(userId, request.nickname(), request.avatarUrl());
        return ResponseEntity.ok(MessageResponse.of("프로필이 수정되었습니다."));
    }

    /**
     * 사용자의 온라인 상태를 업데이트한다.
     *
     * @param userId  상태를 업데이트할 사용자 ID
     * @param request 온라인 상태 업데이트 요청 정보
     * @return 업데이트 완료 메시지
     */
    @Operation(summary = "온라인 상태 업데이트", description = "사용자의 온라인 상태를 업데이트합니다.")
    @PutMapping("/{userId}/online-status")
    public ResponseEntity<MessageResponse> updateOnlineStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateOnlineStatusRequest request) {
        validateUserAccess(userId);
        updateUserOnlineStatusUseCase.updateOnlineStatus(userId, request.status());
        return ResponseEntity.ok(MessageResponse.of("온라인 상태가 업데이트되었습니다."));
    }

    /**
     * 사용자의 마지막 접속 시간을 현재 시간으로 업데이트한다.
     *
     * @param userId 업데이트할 사용자 ID
     * @return 업데이트 완료 메시지
     */
    @Operation(summary = "마지막 접속 시간 업데이트", description = "사용자의 마지막 접속 시간을 업데이트합니다.")
    @PutMapping("/{userId}/last-active")
    public ResponseEntity<MessageResponse> updateLastActive(@PathVariable Long userId) {
        validateUserAccess(userId);
        updateUserOnlineStatusUseCase.updateLastActiveAt(userId);
        return ResponseEntity.ok(MessageResponse.of("마지막 접속 시간이 업데이트되었습니다."));
    }

    /**
     * 요청된 userId가 현재 인증된 사용자의 ID와 일치하는지 검증합니다.
     *
     * @param userId 검증할 사용자 ID
     * @throws ResourceAccessDeniedException 사용자 ID가 일치하지 않는 경우
     */
    private void validateUserAccess(Long userId) {
        Long currentUserId = securityContextHelper.getCurrentUserId();
        if (!currentUserId.equals(userId)) {
            throw new ResourceAccessDeniedException();
        }
    }
}
