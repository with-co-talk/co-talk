package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.common.MessageResponse;
import com.cotalk.adapter.inbound.rest.dto.user.SearchUserResponse;
import com.cotalk.adapter.inbound.rest.dto.user.UpdateOnlineStatusRequest;
import com.cotalk.adapter.inbound.rest.dto.user.UpdateProfileRequest;
import com.cotalk.adapter.inbound.rest.dto.user.UserDto;
import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.user.SearchUserUseCase;
import com.cotalk.domain.port.inbound.user.UpdateProfileUseCase;
import com.cotalk.domain.port.inbound.user.UpdateUserOnlineStatusUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    /**
     * 닉네임으로 사용자를 검색한다.
     *
     * @param nickname 검색할 닉네임 키워드
     * @return 검색된 사용자 목록
     */
    @Operation(summary = "닉네임으로 사용자 검색", description = "닉네임에 포함된 키워드로 사용자를 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<SearchUserResponse> searchByNickname(@RequestParam String nickname) {
        List<User> users = searchUserUseCase.searchByNickname(nickname);
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
        updateUserOnlineStatusUseCase.updateLastActiveAt(userId);
        return ResponseEntity.ok(MessageResponse.of("마지막 접속 시간이 업데이트되었습니다."));
    }
}
