package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.TermsAgreement;
import com.cotalk.domain.entity.TermsAgreement.TermsType;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.auth.AgreeToTermsUseCase;
import com.cotalk.domain.port.outbound.TermsAgreementRepository;
import com.cotalk.domain.port.outbound.TimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 약관 동의 유스케이스 구현체.
 * 사용자의 서비스 이용약관, 개인정보 처리방침, 마케팅 수신 동의를 처리한다.
 *
 * @author seunggu.lee
 */
@Slf4j
@Service
@Transactional
public class AgreeToTermsService implements AgreeToTermsUseCase {

    private final TermsAgreementRepository termsAgreementRepository;
    private final TimeProvider timeProvider;
    private final String termsServiceVersion;
    private final String termsPrivacyVersion;

    /**
     * AgreeToTermsService 생성자.
     * 약관 버전 정보는 application.yml에서 주입된다.
     *
     * @param termsAgreementRepository 약관 동의 저장소
     * @param timeProvider 시간 제공자
     * @param termsServiceVersion 서비스 이용약관 버전
     * @param termsPrivacyVersion 개인정보 처리방침 버전
     */
    public AgreeToTermsService(
            TermsAgreementRepository termsAgreementRepository,
            TimeProvider timeProvider,
            @Value("${app.terms.service-version:1.0}") String termsServiceVersion,
            @Value("${app.terms.privacy-version:1.0}") String termsPrivacyVersion) {
        this.termsAgreementRepository = termsAgreementRepository;
        this.timeProvider = timeProvider;
        this.termsServiceVersion = termsServiceVersion;
        this.termsPrivacyVersion = termsPrivacyVersion;
    }

    /**
     * 약관에 동의한다.
     * 서비스 이용약관과 개인정보 처리방침은 필수로 동의해야 한다.
     *
     * @param command 약관 동의 정보 (사용자 ID, 약관 동의 항목 목록, IP 주소)
     * @throws DomainException 필수 약관에 동의하지 않은 경우
     */
    @Override
    public void agreeToTerms(TermsAgreementCommand command) {
        // 필수 약관 동의 확인
        validateRequiredTerms(command.agreements());

        var now = timeProvider.now();
        List<TermsAgreement> agreements = command.agreements().stream()
                .map(item -> TermsAgreement.create(
                        command.userId(),
                        item.termsType(),
                        item.version(),
                        item.agreed(),
                        command.ipAddress(),
                        now
                ))
                .toList();

        termsAgreementRepository.saveAll(agreements);
        log.info("Terms agreements saved for user: {}", command.userId());
    }

    /**
     * 마케팅 수신 동의를 철회한다.
     *
     * @param userId 사용자 ID
     */
    @Override
    public void withdrawMarketingAgreement(Long userId) {
        termsAgreementRepository.findByUserIdAndTermsType(userId, TermsType.MARKETING)
                .ifPresent(agreement -> {
                    agreement.withdraw(timeProvider.now());
                    termsAgreementRepository.save(agreement);
                    log.info("Marketing agreement withdrawn for user: {}", userId);
                });
    }

    /**
     * 사용자의 약관 동의 상태를 조회한다.
     *
     * @param userId 사용자 ID
     * @return 모든 약관 타입에 대한 동의 상태 목록
     */
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

    /**
     * 필수 약관에 동의했는지 확인한다.
     *
     * @param userId 사용자 ID
     * @return 필수 약관 동의 여부
     */
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
            case SERVICE -> termsServiceVersion;
            case PRIVACY -> termsPrivacyVersion;
            case MARKETING -> "1.0";
        };
    }
}
