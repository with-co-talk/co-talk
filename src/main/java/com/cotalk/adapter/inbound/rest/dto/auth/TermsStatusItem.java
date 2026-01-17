package com.cotalk.adapter.inbound.rest.dto.auth;

import com.cotalk.domain.entity.TermsAgreement.TermsType;

/**
 * 개별 약관 동의 상태 DTO.
 *
 * @param termsType 약관 타입
 * @param version   약관 버전
 * @param agreed    동의 여부
 * @param required  필수 여부
 * @author seunggu.lee
 */
public record TermsStatusItem(
        TermsType termsType,
        String version,
        boolean agreed,
        boolean required
) {

    /**
     * TermsStatusItem을 생성한다.
     *
     * @param termsType 약관 타입
     * @param version   약관 버전
     * @param agreed    동의 여부
     * @param required  필수 여부
     * @return TermsStatusItem 인스턴스
     */
    public static TermsStatusItem of(TermsType termsType, String version, boolean agreed, boolean required) {
        return new TermsStatusItem(termsType, version, agreed, required);
    }
}
