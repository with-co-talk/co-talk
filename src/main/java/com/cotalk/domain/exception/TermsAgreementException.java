package com.cotalk.domain.exception;

/**
 * 약관 동의와 관련된 예외.
 *
 * @author seunggu.lee
 */
public class TermsAgreementException extends DomainException {

    public TermsAgreementException(String message) {
        super(message);
    }

    public static TermsAgreementException serviceTermsRequired() {
        return new TermsAgreementException("서비스 이용약관에 동의해야 합니다.");
    }

    public static TermsAgreementException privacyPolicyRequired() {
        return new TermsAgreementException("개인정보 처리방침에 동의해야 합니다.");
    }
}
