package com.cotalk.application.service;

import com.cotalk.domain.entity.TermsAgreement;
import com.cotalk.domain.entity.TermsAgreement.TermsType;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.AgreeToTermsUseCase;
import com.cotalk.domain.port.outbound.TermsAgreementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AgreeToTermsService implements AgreeToTermsUseCase {

    private final TermsAgreementRepository termsAgreementRepository;

    @Value("${app.terms.service-version:1.0}")
    private String serviceTermsVersion;

    @Value("${app.terms.privacy-version:1.0}")
    private String privacyTermsVersion;

    @Override
    public void agreeToTerms(TermsAgreementCommand command) {
        // 필수 약관 동의 확인
        validateRequiredTerms(command.agreements());

        List<TermsAgreement> agreements = command.agreements().stream()
                .map(item -> TermsAgreement.create(
                        command.userId(),
                        item.termsType(),
                        item.version(),
                        item.agreed(),
                        command.ipAddress()
                ))
                .toList();

        termsAgreementRepository.saveAll(agreements);
        log.info("Terms agreements saved for user: {}", command.userId());
    }

    @Override
    public void withdrawMarketingAgreement(Long userId) {
        termsAgreementRepository.findByUserIdAndTermsType(userId, TermsType.MARKETING)
                .ifPresent(agreement -> {
                    agreement.withdraw();
                    termsAgreementRepository.save(agreement);
                    log.info("Marketing agreement withdrawn for user: {}", userId);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<TermsAgreementStatus> getAgreementStatus(Long userId) {
        List<TermsAgreement> agreements = termsAgreementRepository.findByUserId(userId);
        
        List<TermsAgreementStatus> statusList = new ArrayList<>();
        
        // 모든 약관 타입에 대해 상태 반환
        for (TermsType type : TermsType.values()) {
            TermsAgreement agreement = agreements.stream()
                    .filter(a -> a.getTermsType() == type)
                    .findFirst()
                    .orElse(null);

            String version = getVersionForType(type);
            boolean agreed = agreement != null && agreement.isAgreed();
            boolean required = type == TermsType.SERVICE || type == TermsType.PRIVACY;

            statusList.add(new TermsAgreementStatus(type, version, agreed, required));
        }
        
        return statusList;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAgreedToRequiredTerms(Long userId) {
        return termsAgreementRepository.hasAgreedToRequiredTerms(userId);
    }

    private void validateRequiredTerms(List<TermsAgreementItem> items) {
        boolean serviceAgreed = items.stream()
                .anyMatch(i -> i.termsType() == TermsType.SERVICE && i.agreed());
        boolean privacyAgreed = items.stream()
                .anyMatch(i -> i.termsType() == TermsType.PRIVACY && i.agreed());

        if (!serviceAgreed) {
            throw new DomainException("서비스 이용약관에 동의해야 합니다.");
        }
        if (!privacyAgreed) {
            throw new DomainException("개인정보 처리방침에 동의해야 합니다.");
        }
    }

    private String getVersionForType(TermsType type) {
        return switch (type) {
            case SERVICE -> serviceTermsVersion;
            case PRIVACY -> privacyTermsVersion;
            case MARKETING -> "1.0";
        };
    }
}
