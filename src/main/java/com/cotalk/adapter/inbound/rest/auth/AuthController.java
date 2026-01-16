package com.cotalk.adapter.inbound.rest.auth;

import com.cotalk.application.dto.request.LoginRequest;
import com.cotalk.application.dto.request.SignUpRequest;
import com.cotalk.application.dto.response.ApiResponse;
import com.cotalk.application.dto.response.AuthResponse;
import com.cotalk.application.dto.response.UserResponse;
import com.cotalk.application.service.auth.GetCurrentUserService;
import com.cotalk.application.service.auth.LoginService;
import com.cotalk.application.service.auth.SignUpService;
import com.cotalk.domain.entity.User;
import com.cotalk.infrastructure.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 인증 REST API 컨트롤러.
 *
 * @author Co-Talk Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SignUpService signUpService;
    private final LoginService loginService;
    private final GetCurrentUserService getCurrentUserService;
    private final JwtUtil jwtUtil;

    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        UUID userId = signUpService.signUp(request.getEmail(), request.getPassword(), request.getNickname());
        String token = loginService.login(request.getEmail(), request.getPassword());
        User user = getCurrentUserService.getCurrentUser(userId);

        AuthResponse authResponse = new AuthResponse(token, toUserResponse(user));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", authResponse));
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        String token = loginService.login(request.getEmail(), request.getPassword());
        // 토큰에서 사용자 ID 추출
        String userIdStr = jwtUtil.getUserIdFromToken(token);
        UUID userId = UUID.fromString(userIdStr);
        User user = getCurrentUserService.getCurrentUser(userId);

        AuthResponse authResponse = new AuthResponse(token, toUserResponse(user));
        return ResponseEntity.ok(ApiResponse.success(authResponse));
    }

    /**
     * 현재 사용자 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        User user = getCurrentUserService.getCurrentUser(userId);
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
