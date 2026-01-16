package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.TermsAgreement;
import com.cotalk.domain.entity.TermsAgreement.TermsType;

import java.util.List;
import java.util.Optional;

public interface TermsAgreementRepository {

    TermsAgreement save(TermsAgreement agreement);

    List<TermsAgreement> saveAll(List<TermsAgreement> agreements);

    Optional<TermsAgreement> findByUserIdAndTermsType(Long userId, TermsType termsType);

    List<TermsAgreement> findByUserId(Long userId);

    boolean hasAgreedToRequiredTerms(Long userId);

    void deleteByUserId(Long userId);
}
