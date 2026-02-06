package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.auth.DeleteAccountRequest;
import com.cotalk.adapter.inbound.rest.dto.common.MessageResponse;
import com.cotalk.domain.port.inbound.user.DeleteAccountUseCase;
import com.cotalk.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 계정 관리를 위한 REST 컨트롤러.
 * 회원 탈퇴 등의 계정 관련 기능을 제공한다.
 *
 * @author seunggu.lee
 */
@Tag(name = "Account", description = "계정 관리 API")
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final DeleteAccountUseCase deleteAccountUseCase;

    /**
     * 비밀번호 확인 후 계정을 삭제한다.
     *
     * @param principal 인증된 사용자 정보
     * @param request   계정 삭제 요청 정보 (비밀번호)
     * @return 삭제 완료 메시지
     */
    @Operation(summary = "회원 탈퇴", description = "비밀번호 확인 후 계정을 삭제합니다.")
    @DeleteMapping
    public ResponseEntity<MessageResponse> deleteAccount(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody DeleteAccountRequest request) {
        deleteAccountUseCase.deleteAccount(principal.getUserId(), request.password());
        return ResponseEntity.ok(MessageResponse.of("회원 탈퇴가 완료되었습니다."));
    }
}
