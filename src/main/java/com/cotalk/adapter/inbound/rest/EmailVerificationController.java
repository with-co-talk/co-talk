package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.auth.ResendVerificationRequest;
import com.cotalk.adapter.inbound.rest.dto.common.MessageResponse;
import com.cotalk.domain.port.inbound.auth.ResendVerificationUseCase;
import com.cotalk.domain.port.inbound.auth.VerifyEmailUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이메일 인증 REST 컨트롤러.
 * 이메일 인증 및 인증 이메일 재발송 기능을 제공한다.
 *
 * @author seunggu.lee
 */
@Tag(name = "Email Verification", description = "이메일 인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final VerifyEmailUseCase verifyEmailUseCase;
    private final ResendVerificationUseCase resendVerificationUseCase;

    /**
     * 토큰을 사용하여 이메일 인증을 완료한다.
     *
     * @param token 이메일 인증 토큰
     * @return 인증 완료 메시지
     */
    @Operation(summary = "이메일 인증", description = "토큰을 사용하여 이메일 인증을 완료합니다.")
    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
        verifyEmailUseCase.verifyEmail(token);
        return ResponseEntity.ok(MessageResponse.of("이메일 인증이 완료되었습니다. 이제 로그인할 수 있습니다."));
    }

    /**
     * 이메일 인증 이메일을 재발송한다.
     * 보안상 이메일 존재 여부와 관계없이 동일한 응답을 반환한다.
     *
     * @param request 재발송 요청 정보 (이메일)
     * @return 발송 완료 메시지
     */
    @Operation(summary = "인증 이메일 재발송", description = "이메일 인증 이메일을 재발송합니다.")
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        resendVerificationUseCase.resendVerification(request.email());
        return ResponseEntity.ok(MessageResponse.of("인증 이메일이 발송되었습니다. 이메일을 확인해주세요."));
    }
}
