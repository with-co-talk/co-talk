package com.cotalk.domain.port.inbound.auth;

import com.cotalk.domain.entity.TermsAgreement.TermsType;

import java.util.List;

/**
 * 이용약관 동의 유스케이스.
 * 약관 동의, 철회, 상태 조회를 처리한다.
 *
 * @author seunggu.lee
 */
public interface AgreeToTermsUseCase {

    /**
     * 약관 동의를 처리한다.
     *
     * @param command 동의 명령
     */
    void agreeToTerms(TermsAgreementCommand command);

    /**
     * 마케팅 수신 동의를 철회한다.
     *
     * @param userId 사용자 ID
     */
    void withdrawMarketingAgreement(Long userId);

    /**
     * 사용자의 약관 동의 상태를 조회한다.
     *
     * @param userId 사용자 ID
     * @return 동의 상태 목록
     */
    List<TermsAgreementStatus> getAgreementStatus(Long userId);

    /**
     * 필수 약관에 동의했는지 확인한다.
     *
     * @param userId 사용자 ID
     * @return 필수 약관 동의 여부
     */
    boolean hasAgreedToRequiredTerms(Long userId);

    /**
     * 약관 동의 명령.
     *
     * @param userId 사용자 ID
     * @param agreements 동의 항목 목록
     * @param ipAddress 접속 IP 주소
     */
    record TermsAgreementCommand(
            Long userId,
            List<TermsAgreementItem> agreements,
            String ipAddress
    ) {}

    /**
     * 개별 약관 동의 항목.
     *
     * @param termsType 약관 유형
     * @param version 약관 버전
     * @param agreed 동의 여부
     */
    record TermsAgreementItem(
            TermsType termsType,
            String version,
            boolean agreed
    ) {}

    /**
     * 약관 동의 상태.
     *
     * @param termsType 약관 유형
     * @param termsVersion 약관 버전
     * @param agreed 동의 여부
     * @param required 필수 여부
     */
    record TermsAgreementStatus(
            TermsType termsType,
            String termsVersion,
            boolean agreed,
            boolean required
    ) {}
}
