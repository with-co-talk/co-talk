package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.auth.ChangePasswordRequest;
import com.cotalk.adapter.inbound.rest.dto.auth.PasswordResetCodeRequest;
import com.cotalk.adapter.inbound.rest.dto.auth.PasswordResetRequest;
import com.cotalk.adapter.inbound.rest.dto.auth.ResetPasswordRequest;
import com.cotalk.adapter.inbound.rest.dto.auth.ResetPasswordWithCodeRequest;
import com.cotalk.adapter.inbound.rest.dto.auth.TokenValidationResponse;
import com.cotalk.adapter.inbound.rest.dto.auth.VerifyCodeRequest;
import com.cotalk.adapter.inbound.rest.dto.common.MessageResponse;
import com.cotalk.domain.port.inbound.auth.ChangePasswordUseCase;
import com.cotalk.domain.port.inbound.auth.RequestPasswordResetUseCase;
import com.cotalk.domain.port.inbound.auth.ResetPasswordUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 비밀번호 관리를 위한 REST 컨트롤러.
 * 비밀번호 재설정 요청, 토큰 검증, 비밀번호 변경 등의 기능을 제공한다.
 *
 * @author seunggu.lee
 */
@Tag(name = "Password", description = "비밀번호 관리 API")
@RestController
@RequestMapping("/api/v1/password")
@RequiredArgsConstructor
public class PasswordController {

    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;

    /**
     * 이메일로 비밀번호 재설정 링크를 발송한다.
     * 보안상 이메일 존재 여부와 관계없이 동일한 응답을 반환한다.
     *
     * @param request 비밀번호 재설정 요청 정보 (이메일)
     * @return 발송 완료 메시지
     */
    @Operation(summary = "비밀번호 재설정 요청", description = "이메일로 비밀번호 재설정 링크를 발송합니다.")
    @PostMapping("/reset-request")
    public ResponseEntity<MessageResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        requestPasswordResetUseCase.requestPasswordReset(request.email());
        // 보안상 이메일 존재 여부와 관계없이 동일하게 응답
        return ResponseEntity.ok(MessageResponse.of(
                "비밀번호 재설정 링크가 이메일로 발송되었습니다. 이메일을 확인해주세요."
        ));
    }

    /**
     * 비밀번호 재설정 토큰의 유효성을 검증한다.
     *
     * @param token 검증할 토큰
     * @return 토큰 유효성 여부
     */
    @Operation(summary = "비밀번호 재설정 토큰 검증", description = "비밀번호 재설정 토큰의 유효성을 검증합니다.")
    @GetMapping("/reset-validate")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestParam String token) {
        boolean isValid = resetPasswordUseCase.validateToken(token);
        return ResponseEntity.ok(TokenValidationResponse.of(isValid));
    }

    /**
     * 토큰을 사용하여 새 비밀번호로 변경한다.
     *
     * @param request 비밀번호 재설정 정보 (토큰, 새 비밀번호)
     * @return 변경 완료 메시지
     */
    @Operation(summary = "비밀번호 재설정", description = "새 비밀번호로 변경합니다.")
    @PostMapping("/reset")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(MessageResponse.of("비밀번호가 성공적으로 변경되었습니다."));
    }

    /**
     * 현재 비밀번호를 확인 후 새 비밀번호로 변경한다.
     * 인증된 사용자만 사용할 수 있다.
     *
     * @param userId 인증된 사용자 ID
     * @param request 비밀번호 변경 요청 정보 (현재 비밀번호, 새 비밀번호)
     * @return 변경 완료 메시지
     */
    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호를 확인 후 새 비밀번호로 변경합니다.")
    @PutMapping("/change")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        changePasswordUseCase.changePassword(userId, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(MessageResponse.of("비밀번호가 성공적으로 변경되었습니다."));
    }

    @Operation(summary = "비밀번호 재설정 코드 발송", description = "이메일로 6자리 인증 코드를 발송합니다.")
    @PostMapping("/reset-request-code")
    public ResponseEntity<MessageResponse> requestPasswordResetCode(
            @Valid @RequestBody PasswordResetCodeRequest request) {
        requestPasswordResetUseCase.requestPasswordResetWithCode(request.email());
        return ResponseEntity.ok(MessageResponse.of(
                "인증 코드가 이메일로 발송되었습니다. 이메일을 확인해주세요."
        ));
    }

    /**
     * 이메일과 6자리 인증 코드를 검증한다.
     * 코드가 유효하지 않은 경우 사유별 예외가 발생한다.
     *
     * @param request 인증 코드 검증 요청 (이메일, 코드)
     * @return 검증 성공 메시지
     */
    @Operation(summary = "인증 코드 검증", description = "이메일과 6자리 인증 코드를 검증합니다.")
    @PostMapping("/verify-code")
    public ResponseEntity<MessageResponse> verifyCode(
            @Valid @RequestBody VerifyCodeRequest request) {
        resetPasswordUseCase.verifyCode(request.email(), request.code());
        return ResponseEntity.ok(MessageResponse.of("인증 코드가 확인되었습니다."));
    }

    @Operation(summary = "인증 코드로 비밀번호 재설정", description = "인증 코드를 이용하여 새 비밀번호로 변경합니다.")
    @PostMapping("/reset-with-code")
    public ResponseEntity<MessageResponse> resetPasswordWithCode(
            @Valid @RequestBody ResetPasswordWithCodeRequest request) {
        resetPasswordUseCase.resetPasswordWithCode(request.email(), request.code(), request.newPassword());
        return ResponseEntity.ok(MessageResponse.of("비밀번호가 성공적으로 변경되었습니다."));
    }
}
