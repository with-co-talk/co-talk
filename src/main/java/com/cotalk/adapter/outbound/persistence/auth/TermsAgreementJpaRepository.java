package com.cotalk.adapter.outbound.persistence.auth;

import com.cotalk.adapter.outbound.persistence.entity.TermsAgreementJpaEntity;
import com.cotalk.domain.entity.TermsAgreement.TermsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * 약관 동의 JPA 리포지토리.
 * persistence 계층 전용이며, 도메인 반환은 Adapter에서 매핑한다.
 *
 * @author seunggu.lee
 */
public interface TermsAgreementJpaRepository extends JpaRepository<TermsAgreementJpaEntity, Long> {

    /**
     * 사용자 ID와 약관 유형으로 약관 동의 정보를 조회한다.
     *
     * @param userId 사용자 ID
     * @param termsType 약관 유형
     * @return 약관 동의 정보 (Optional)
     */
    Optional<TermsAgreementJpaEntity> findByUserIdAndTermsType(Long userId, TermsType termsType);

    /**
     * 사용자 ID로 모든 약관 동의 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 약관 동의 목록
     */
    List<TermsAgreementJpaEntity> findByUserId(Long userId);

    /**
     * 사용자가 필수 약관(서비스, 개인정보)에 모두 동의했는지 확인한다.
     *
     * @param userId 사용자 ID
     * @return 필수 약관 동의 여부
     */
    @Query("SELECT COUNT(t) = 2 FROM TermsAgreementJpaEntity t WHERE t.userId = :userId " +
            "AND t.termsType IN ('SERVICE', 'PRIVACY') AND t.agreed = true")
    boolean hasAgreedToRequiredTerms(Long userId);

    /**
     * 사용자 ID로 모든 약관 동의 정보를 삭제한다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);
}
