package com.cotalk.adapter.inbound.rest;

import com.cotalk.adapter.inbound.rest.dto.auth.RequiredTermsCheckResponse;
import com.cotalk.adapter.inbound.rest.dto.auth.TermsAgreementRequest;
import com.cotalk.adapter.inbound.rest.dto.auth.TermsStatusItem;
import com.cotalk.adapter.inbound.rest.dto.auth.TermsStatusResponse;
import com.cotalk.adapter.inbound.rest.dto.common.MessageResponse;
import com.cotalk.domain.port.inbound.auth.AgreeToTermsUseCase;
import com.cotalk.domain.port.inbound.auth.AgreeToTermsUseCase.TermsAgreementCommand;
import com.cotalk.domain.port.inbound.auth.AgreeToTermsUseCase.TermsAgreementItem;
import com.cotalk.domain.port.inbound.auth.AgreeToTermsUseCase.TermsAgreementStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 이용약관 관리를 위한 REST 컨트롤러.
 * 약관 동의, 마케팅 수신 동의 철회, 동의 상태 조회 등의 기능을 제공한다.
 *
 * @author seunggu.lee
 */
@Tag(name = "Terms", description = "이용약관 관리 API")
@RestController
@RequestMapping("/api/v1/terms")
@RequiredArgsConstructor
public class TermsController {

    private final AgreeToTermsUseCase agreeToTermsUseCase;

    /**
     * 이용약관 및 개인정보처리방침에 동의한다.
     *
     * @param request     약관 동의 요청 정보
     * @param httpRequest HTTP 요청 (IP 주소 추출용)
     * @return 동의 완료 메시지
     */
    @Operation(summary = "약관 동의", description = "이용약관 및 개인정보처리방침에 동의합니다.")
    @PostMapping("/agree")
    public ResponseEntity<MessageResponse> agreeToTerms(
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
        return ResponseEntity.ok(MessageResponse.of("약관 동의가 완료되었습니다."));
    }

    /**
     * 마케팅 정보 수신 동의를 철회한다.
     *
     * @param userId 동의를 철회할 사용자 ID
     * @return 철회 완료 메시지
     */
    @Operation(summary = "마케팅 수신 동의 철회", description = "마케팅 정보 수신 동의를 철회합니다.")
    @DeleteMapping("/marketing/{userId}")
    public ResponseEntity<MessageResponse> withdrawMarketingAgreement(@PathVariable Long userId) {
        agreeToTermsUseCase.withdrawMarketingAgreement(userId);
        return ResponseEntity.ok(MessageResponse.of("마케팅 수신 동의가 철회되었습니다."));
    }

    /**
     * 사용자의 약관 동의 상태를 조회한다.
     *
     * @param userId 조회할 사용자 ID
     * @return 약관별 동의 상태 목록
     */
    @Operation(summary = "약관 동의 상태 조회", description = "사용자의 약관 동의 상태를 조회합니다.")
    @GetMapping("/status/{userId}")
    public ResponseEntity<TermsStatusResponse> getAgreementStatus(@PathVariable Long userId) {
        List<TermsAgreementStatus> statusList = agreeToTermsUseCase.getAgreementStatus(userId);
        List<TermsStatusItem> items = statusList.stream()
                .map(s -> TermsStatusItem.of(s.termsType(), s.termsVersion(), s.agreed(), s.required()))
                .toList();
        return ResponseEntity.ok(TermsStatusResponse.of(items));
    }

    /**
     * 필수 약관에 동의했는지 확인한다.
     *
     * @param userId 확인할 사용자 ID
     * @return 필수 약관 동의 여부
     */
    @Operation(summary = "필수 약관 동의 확인", description = "필수 약관에 동의했는지 확인합니다.")
    @GetMapping("/check/{userId}")
    public ResponseEntity<RequiredTermsCheckResponse> checkRequiredTerms(@PathVariable Long userId) {
        boolean agreed = agreeToTermsUseCase.hasAgreedToRequiredTerms(userId);
        return ResponseEntity.ok(RequiredTermsCheckResponse.of(agreed));
    }

    /**
     * HTTP 요청에서 클라이언트 IP 주소를 추출한다.
     * X-Forwarded-For 헤더가 있으면 첫 번째 IP를, 없으면 remoteAddr을 반환한다.
     *
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
