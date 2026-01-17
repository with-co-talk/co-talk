package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.TermsAgreement;
import com.cotalk.domain.entity.TermsAgreement.TermsType;

import java.util.List;
import java.util.Optional;

/**
 * 약관 동의 레포지토리 포트.
 * 사용자 약관 동의 데이터 저장 및 조회를 위한 인터페이스를 정의한다.
 *
 * @author seunggu.lee
 */
public interface TermsAgreementRepository {

    /**
     * 약관 동의를 저장한다.
     *
     * @param agreement 저장할 약관 동의
     * @return 저장된 약관 동의
     */
    TermsAgreement save(TermsAgreement agreement);

    /**
     * 여러 약관 동의를 일괄 저장한다.
     *
     * @param agreements 저장할 약관 동의 목록
     * @return 저장된 약관 동의 목록
     */
    List<TermsAgreement> saveAll(List<TermsAgreement> agreements);

    /**
     * 사용자 ID와 약관 유형으로 약관 동의를 조회한다.
     *
     * @param userId    사용자 ID
     * @param termsType 약관 유형
     * @return 조회된 약관 동의 (Optional)
     */
    Optional<TermsAgreement> findByUserIdAndTermsType(Long userId, TermsType termsType);

    /**
     * 사용자의 모든 약관 동의를 조회한다.
     *
     * @param userId 사용자 ID
     * @return 약관 동의 목록
     */
    List<TermsAgreement> findByUserId(Long userId);

    /**
     * 사용자가 필수 약관에 모두 동의했는지 확인한다.
     *
     * @param userId 사용자 ID
     * @return 필수 약관 동의 여부
     */
    boolean hasAgreedToRequiredTerms(Long userId);

    /**
     * 특정 사용자의 모든 약관 동의를 삭제한다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);
}
