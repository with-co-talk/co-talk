package com.cotalk.adapter.inbound.rest.user;

import com.cotalk.application.dto.request.UpdateProfileRequest;
import com.cotalk.application.dto.response.ApiResponse;
import com.cotalk.application.dto.response.UserResponse;
import com.cotalk.application.service.user.SearchUsersService;
import com.cotalk.application.service.user.UpdateUserProfileService;
import com.cotalk.domain.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 사용자 REST API 컨트롤러.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SearchUsersService searchUsersService;
    private final UpdateUserProfileService updateUserProfileService;

    /**
     * 사용자 검색
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @RequestParam String keyword,
            Authentication authentication) {
        UUID currentUserId = (UUID) authentication.getPrincipal();
        List<User> users = searchUsersService.searchUsers(keyword, currentUserId);
        List<UserResponse> responses = users.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 프로필 수정
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        User user = updateUserProfileService.updateProfile(
                userId, request.getNickname(), request.getAvatarUrl());
        return ResponseEntity.ok(ApiResponse.success(toUserResponse(user)));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl()
        );
    }
}
