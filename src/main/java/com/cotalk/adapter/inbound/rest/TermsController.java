package com.cotalk.adapter.inbound.rest;

import com.cotalk.domain.entity.TermsAgreement.TermsType;
import com.cotalk.domain.port.inbound.AgreeToTermsUseCase;
import com.cotalk.domain.port.inbound.AgreeToTermsUseCase.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Terms", description = "이용약관 관리 API")
@RestController
@RequestMapping("/api/v1/terms")
@RequiredArgsConstructor
public class TermsController {

    private final AgreeToTermsUseCase agreeToTermsUseCase;

    @Operation(summary = "약관 동의", description = "이용약관 및 개인정보처리방침에 동의합니다.")
    @PostMapping("/agree")
    public ResponseEntity<TermsAgreementResponse> agreeToTerms(
            @Valid @RequestBody TermsAgreementRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);

        List<TermsAgreementItem> items = request.agreements().stream()
                .map(a -> new TermsAgreementItem(a.termsType(), a.version(), a.agreed()))
                .toList();

        TermsAgreementCommand command = new TermsAgreementCommand(
                request.userId(),
                items,
                ipAddress
        );

        agreeToTermsUseCase.agreeToTerms(command);
        return ResponseEntity.ok(new TermsAgreementResponse("약관 동의가 완료되었습니다."));
    }

    @Operation(summary = "마케팅 수신 동의 철회", description = "마케팅 정보 수신 동의를 철회합니다.")
    @DeleteMapping("/marketing/{userId}")
    public ResponseEntity<TermsAgreementResponse> withdrawMarketingAgreement(@PathVariable Long userId) {
        agreeToTermsUseCase.withdrawMarketingAgreement(userId);
        return ResponseEntity.ok(new TermsAgreementResponse("마케팅 수신 동의가 철회되었습니다."));
    }

    @Operation(summary = "약관 동의 상태 조회", description = "사용자의 약관 동의 상태를 조회합니다.")
    @GetMapping("/status/{userId}")
    public ResponseEntity<TermsStatusResponse> getAgreementStatus(@PathVariable Long userId) {
        List<TermsAgreementStatus> statusList = agreeToTermsUseCase.getAgreementStatus(userId);
        List<TermsStatusItem> items = statusList.stream()
                .map(s -> new TermsStatusItem(s.termsType(), s.termsVersion(), s.agreed(), s.required()))
                .toList();
        return ResponseEntity.ok(new TermsStatusResponse(items));
    }

    @Operation(summary = "필수 약관 동의 확인", description = "필수 약관에 동의했는지 확인합니다.")
    @GetMapping("/check/{userId}")
    public ResponseEntity<RequiredTermsCheckResponse> checkRequiredTerms(@PathVariable Long userId) {
        boolean agreed = agreeToTermsUseCase.hasAgreedToRequiredTerms(userId);
        return ResponseEntity.ok(new RequiredTermsCheckResponse(agreed));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // Request DTOs
    public record TermsAgreementRequest(
            @NotNull(message = "사용자 ID는 필수입니다.")
            Long userId,

            @NotEmpty(message = "동의 항목은 필수입니다.")
            List<AgreementItem> agreements
    ) {}

    public record AgreementItem(
            @NotNull(message = "약관 타입은 필수입니다.")
            TermsType termsType,

            @NotNull(message = "약관 버전은 필수입니다.")
            String version,

            boolean agreed
    ) {}

    // Response DTOs
    public record TermsAgreementResponse(String message) {}

    public record TermsStatusResponse(List<TermsStatusItem> agreements) {}

    public record TermsStatusItem(
            TermsType termsType,
            String version,
            boolean agreed,
            boolean required
    ) {}

    public record RequiredTermsCheckResponse(boolean agreedToRequiredTerms) {}
}
