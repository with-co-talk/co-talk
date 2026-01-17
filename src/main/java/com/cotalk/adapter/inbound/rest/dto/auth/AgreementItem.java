package com.cotalk.adapter.inbound.rest.dto.auth;

import com.cotalk.domain.entity.TermsAgreement.TermsType;
import jakarta.validation.constraints.NotNull;

/**
 * 개별 약관 동의 항목 DTO.
 *
 * @param termsType 약관 타입
 * @param version   약관 버전
 * @param agreed    동의 여부
 * @author seunggu.lee
 */
public record AgreementItem(
        @NotNull(message = "약관 타입은 필수입니다.")
        TermsType termsType,

        @NotNull(message = "약관 버전은 필수입니다.")
        String version,

        boolean agreed
) {}
