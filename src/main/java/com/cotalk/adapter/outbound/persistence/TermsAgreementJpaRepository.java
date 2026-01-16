package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.TermsAgreement;
import com.cotalk.domain.entity.TermsAgreement.TermsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TermsAgreementJpaRepository extends JpaRepository<TermsAgreement, Long> {

    Optional<TermsAgreement> findByUserIdAndTermsType(Long userId, TermsType termsType);

    List<TermsAgreement> findByUserId(Long userId);

    @Query("SELECT COUNT(t) = 2 FROM TermsAgreement t WHERE t.userId = :userId " +
            "AND t.termsType IN ('SERVICE', 'PRIVACY') AND t.agreed = true")
    boolean hasAgreedToRequiredTerms(Long userId);

    void deleteByUserId(Long userId);
}
