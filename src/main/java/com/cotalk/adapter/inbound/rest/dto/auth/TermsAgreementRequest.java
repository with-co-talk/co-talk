package com.cotalk.adapter.inbound.rest.dto.auth;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 약관 동의 요청 DTO.
 *
 * @param userId     사용자 ID
 * @param agreements 동의 항목 목록
 * @author seunggu.lee
 */
public record TermsAgreementRequest(
        @NotNull(message = "사용자 ID는 필수입니다.")
        Long userId,

        @NotEmpty(message = "동의 항목은 필수입니다.")
        List<AgreementItem> agreements
) {}
