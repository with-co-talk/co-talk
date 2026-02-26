package com.cotalk.adapter.inbound.rest.dto.auth;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 약관 동의 요청 DTO.
 *
 * @param agreements 동의 항목 목록
 * @author seunggu.lee
 */
public record TermsAgreementRequest(
        @NotEmpty(message = "동의 항목은 필수입니다.")
        List<AgreementItem> agreements
) {}
