package com.cotalk.adapter.inbound.rest.dto.auth;

/**
 * 필수 약관 동의 확인 응답 DTO.
 *
 * @param agreedToRequiredTerms 필수 약관 동의 여부
 * @author seunggu.lee
 */
public record RequiredTermsCheckResponse(boolean agreedToRequiredTerms) {

    /**
     * RequiredTermsCheckResponse를 생성한다.
     *
     * @param agreedToRequiredTerms 필수 약관 동의 여부
     * @return RequiredTermsCheckResponse 인스턴스
     */
    public static RequiredTermsCheckResponse of(boolean agreedToRequiredTerms) {
        return new RequiredTermsCheckResponse(agreedToRequiredTerms);
    }
}
