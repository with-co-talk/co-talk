package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.port.inbound.DeleteAccountUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Account", description = "계정 관리 API")
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final DeleteAccountUseCase deleteAccountUseCase;

    @Operation(summary = "회원 탈퇴", description = "비밀번호 확인 후 계정을 삭제합니다.")
    @DeleteMapping("/{userId}")
    public ResponseEntity<DeleteAccountResponse> deleteAccount(
            @PathVariable Long userId,
            @Valid @RequestBody DeleteAccountRequest request) {
        deleteAccountUseCase.deleteAccount(userId, request.password());
        return ResponseEntity.ok(new DeleteAccountResponse("회원 탈퇴가 완료되었습니다."));
    }

    // Request DTO
    public record DeleteAccountRequest(
            @NotBlank(message = "비밀번호는 필수입니다.")
            String password
    ) {}

    // Response DTO
    public record DeleteAccountResponse(String message) {}
}
