package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.port.inbound.LoginUseCase;
import com.cotalk.domain.port.inbound.SignUpUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "회원가입, 로그인 API")
public class AuthController {

    private final SignUpUseCase signUpUseCase;
    private final LoginUseCase loginUseCase;

    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        Long userId = signUpUseCase.signUp(request.email(), request.password(), request.nickname());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SignUpResponse(userId, "회원가입이 완료되었습니다."));
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = loginUseCase.login(request.email(), request.password());
        return ResponseEntity.ok(new LoginResponse(token, "Bearer"));
    }

    // Request DTOs
    public record SignUpRequest(
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "올바른 이메일 형식이 아닙니다.")
            String email,

            @NotBlank(message = "비밀번호는 필수입니다.")
            String password,

            @NotBlank(message = "닉네임은 필수입니다.")
            String nickname
    ) {}

    public record LoginRequest(
            @NotBlank(message = "이메일은 필수입니다.")
            String email,

            @NotBlank(message = "비밀번호는 필수입니다.")
            String password
    ) {}

    // Response DTOs
    public record SignUpResponse(Long userId, String message) {}
    public record LoginResponse(String token, String tokenType) {}
}
