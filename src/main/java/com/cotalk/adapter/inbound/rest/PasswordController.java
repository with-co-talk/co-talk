package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.port.inbound.RequestPasswordResetUseCase;
import com.cotalk.domain.port.inbound.ResetPasswordUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Password", description = "비밀번호 관리 API")
@RestController
@RequestMapping("/api/v1/password")
@RequiredArgsConstructor
public class PasswordController {

    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    @Operation(summary = "비밀번호 재설정 요청", description = "이메일로 비밀번호 재설정 링크를 발송합니다.")
    @PostMapping("/reset-request")
    public ResponseEntity<PasswordResetResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        requestPasswordResetUseCase.requestPasswordReset(request.email());
        // 보안상 이메일 존재 여부와 관계없이 동일하게 응답
        return ResponseEntity.ok(new PasswordResetResponse(
                "비밀번호 재설정 링크가 이메일로 발송되었습니다. 이메일을 확인해주세요."
        ));
    }

    @Operation(summary = "비밀번호 재설정 토큰 검증", description = "비밀번호 재설정 토큰의 유효성을 검증합니다.")
    @GetMapping("/reset-validate")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestParam String token) {
        boolean isValid = resetPasswordUseCase.validateToken(token);
        return ResponseEntity.ok(new TokenValidationResponse(isValid));
    }

    @Operation(summary = "비밀번호 재설정", description = "새 비밀번호로 변경합니다.")
    @PostMapping("/reset")
    public ResponseEntity<PasswordResetResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(new PasswordResetResponse("비밀번호가 성공적으로 변경되었습니다."));
    }

    // Request DTOs
    public record PasswordResetRequest(
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "올바른 이메일 형식이 아닙니다.")
            String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "토큰은 필수입니다.")
            String token,

            @NotBlank(message = "새 비밀번호는 필수입니다.")
            @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다.")
            String newPassword
    ) {}

    // Response DTOs
    public record PasswordResetResponse(String message) {}
    public record TokenValidationResponse(boolean valid) {}
}
