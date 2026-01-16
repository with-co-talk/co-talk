package com.cotalk.domain.port.inbound;

import com.cotalk.domain.entity.TermsAgreement.TermsType;

import java.util.List;

/**
 * 이용약관 동의 유즈케이스
 */
public interface AgreeToTermsUseCase {

    /**
     * 약관 동의 처리
     *
     * @param command 동의 명령
     */
    void agreeToTerms(TermsAgreementCommand command);

    /**
     * 마케팅 수신 동의 철회
     *
     * @param userId 사용자 ID
     */
    void withdrawMarketingAgreement(Long userId);

    /**
     * 사용자의 약관 동의 상태 조회
     *
     * @param userId 사용자 ID
     * @return 동의 상태 목록
     */
    List<TermsAgreementStatus> getAgreementStatus(Long userId);

    /**
     * 필수 약관 동의 여부 확인
     *
     * @param userId 사용자 ID
     * @return 필수 약관 동의 여부
     */
    boolean hasAgreedToRequiredTerms(Long userId);

    record TermsAgreementCommand(
            Long userId,
            List<TermsAgreementItem> agreements,
            String ipAddress
    ) {}

    record TermsAgreementItem(
            TermsType termsType,
            String version,
            boolean agreed
    ) {}

    record TermsAgreementStatus(
            TermsType termsType,
            String termsVersion,
            boolean agreed,
            boolean required
    ) {}
}
