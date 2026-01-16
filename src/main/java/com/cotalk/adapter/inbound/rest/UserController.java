package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.port.inbound.SearchUserUseCase;
import com.cotalk.domain.port.inbound.UpdateProfileUseCase;
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

    @Operation(summary = "닉네임으로 사용자 검색", description = "닉네임에 포함된 키워드로 사용자를 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<SearchUserResponse> searchByNickname(@RequestParam String nickname) {
        List<User> users = searchUserUseCase.searchByNickname(nickname);
        List<UserDto> userDtos = users.stream()
                .map(user -> new UserDto(user.getId(), user.getEmail(), user.getNickname(), user.getAvatarUrl()))
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

    // Response DTOs
    public record SearchUserResponse(List<UserDto> users) {}

    public record UserDto(Long id, String email, String nickname, String avatarUrl) {}

    // Request DTOs
    public record UpdateProfileRequest(String nickname, String avatarUrl) {}

    public record UpdateProfileResponse(String message) {}
}
