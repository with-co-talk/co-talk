package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.entity.User.OnlineStatus;
import com.cotalk.domain.port.inbound.SearchUserUseCase;
import com.cotalk.domain.port.inbound.UpdateProfileUseCase;
import com.cotalk.domain.port.inbound.UpdateUserOnlineStatusUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "사용자", description = "사용자 검색 API")
public class UserController {

    private final SearchUserUseCase searchUserUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final UpdateUserOnlineStatusUseCase updateUserOnlineStatusUseCase;

    @Operation(summary = "닉네임으로 사용자 검색", description = "닉네임에 포함된 키워드로 사용자를 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<SearchUserResponse> searchByNickname(@RequestParam String nickname) {
        List<User> users = searchUserUseCase.searchByNickname(nickname);
        List<UserDto> userDtos = users.stream()
                .map(UserDto::from)
                .toList();
        return ResponseEntity.ok(new SearchUserResponse(userDtos));
    }

    @Operation(summary = "프로필 수정", description = "사용자 프로필(닉네임, 아바타)을 수정합니다.")
    @PutMapping("/{userId}/profile")
    public ResponseEntity<UpdateProfileResponse> updateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        updateProfileUseCase.updateProfile(userId, request.nickname(), request.avatarUrl());
        return ResponseEntity.ok(new UpdateProfileResponse("프로필이 수정되었습니다."));
    }

    @Operation(summary = "온라인 상태 업데이트", description = "사용자의 온라인 상태를 업데이트합니다.")
    @PutMapping("/{userId}/online-status")
    public ResponseEntity<UpdateOnlineStatusResponse> updateOnlineStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateOnlineStatusRequest request) {
        updateUserOnlineStatusUseCase.updateOnlineStatus(userId, request.status());
        return ResponseEntity.ok(new UpdateOnlineStatusResponse("온라인 상태가 업데이트되었습니다."));
    }

    @Operation(summary = "마지막 접속 시간 업데이트", description = "사용자의 마지막 접속 시간을 업데이트합니다.")
    @PutMapping("/{userId}/last-active")
    public ResponseEntity<UpdateLastActiveResponse> updateLastActive(@PathVariable Long userId) {
        updateUserOnlineStatusUseCase.updateLastActiveAt(userId);
        return ResponseEntity.ok(new UpdateLastActiveResponse("마지막 접속 시간이 업데이트되었습니다."));
    }

    // Response DTOs
    public record SearchUserResponse(List<UserDto> users) {}

    public record UserDto(
            Long id,
            String email,
            String nickname,
            String avatarUrl,
            OnlineStatus onlineStatus,
            java.time.LocalDateTime lastActiveAt
    ) {
        public static UserDto from(User user) {
            return new UserDto(
                    user.getId(),
                    user.getEmail(),
                    user.getNickname(),
                    user.getAvatarUrl(),
                    user.getOnlineStatus(),
                    user.getLastActiveAt()
            );
        }
    }

    // Request DTOs
    public record UpdateProfileRequest(String nickname, String avatarUrl) {}

    public record UpdateOnlineStatusRequest(
            @jakarta.validation.constraints.NotNull(message = "온라인 상태는 필수입니다.")
            OnlineStatus status
    ) {}

    // Response DTOs
    public record UpdateProfileResponse(String message) {}

    public record UpdateOnlineStatusResponse(String message) {}

    public record UpdateLastActiveResponse(String message) {}
}
