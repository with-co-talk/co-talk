package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.auth.DeleteAccountRequest;
import com.cotalk.adapter.inbound.rest.dto.common.MessageResponse;
import com.cotalk.domain.port.inbound.user.DeleteAccountUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
     * @param userId  삭제할 사용자 ID
     * @param request 계정 삭제 요청 정보 (비밀번호)
     * @return 삭제 완료 메시지
     */
    @Operation(summary = "회원 탈퇴", description = "비밀번호 확인 후 계정을 삭제합니다.")
    @DeleteMapping("/{userId}")
    public ResponseEntity<MessageResponse> deleteAccount(
            @PathVariable Long userId,
            @Valid @RequestBody DeleteAccountRequest request) {
        deleteAccountUseCase.deleteAccount(userId, request.password());
        return ResponseEntity.ok(MessageResponse.of("회원 탈퇴가 완료되었습니다."));
    }
}
