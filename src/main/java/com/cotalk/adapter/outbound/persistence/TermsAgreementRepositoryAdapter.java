package com.cotalk.adapter.outbound.persistence;

import com.cotalk.domain.entity.TermsAgreement;
import com.cotalk.domain.entity.TermsAgreement.TermsType;
import com.cotalk.domain.port.outbound.TermsAgreementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TermsAgreementRepositoryAdapter implements TermsAgreementRepository {

    private final TermsAgreementJpaRepository jpaRepository;

    @Override
    public TermsAgreement save(TermsAgreement agreement) {
        return jpaRepository.save(agreement);
    }

    @Override
    public List<TermsAgreement> saveAll(List<TermsAgreement> agreements) {
        return jpaRepository.saveAll(agreements);
    }

    @Override
    public Optional<TermsAgreement> findByUserIdAndTermsType(Long userId, TermsType termsType) {
        return jpaRepository.findByUserIdAndTermsType(userId, termsType);
    }

    @Override
    public List<TermsAgreement> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Override
    public boolean hasAgreedToRequiredTerms(Long userId) {
        return jpaRepository.hasAgreedToRequiredTerms(userId);
    }

    @Override
    public void deleteByUserId(Long userId) {
        jpaRepository.deleteByUserId(userId);
    }
}
