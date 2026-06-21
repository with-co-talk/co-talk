package com.cotalk.adapter.outbound.persistence.auth;

import com.cotalk.adapter.outbound.persistence.entity.TermsAgreementJpaEntity;
import com.cotalk.adapter.outbound.persistence.mapper.TermsAgreementMapper;
import com.cotalk.domain.entity.TermsAgreement;
import com.cotalk.domain.entity.TermsAgreement.TermsType;
import com.cotalk.domain.port.outbound.TermsAgreementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 약관 동의 영속성 어댑터.
 * JPA 엔티티와 도메인 간 매핑을 수행하며, 도메인 포트를 구현한다.
 *
 * @author seunggu.lee
 */
@Repository
@RequiredArgsConstructor
public class TermsAgreementRepositoryAdapter implements TermsAgreementRepository {

    private final TermsAgreementJpaRepository jpaRepository;
    private final TermsAgreementMapper mapper;

    /**
     * 약관 동의 정보를 저장한다.
     *
     * @param agreement 저장할 약관 동의 엔티티
     * @return 저장된 약관 동의 엔티티
     */
    @Override
    public TermsAgreement save(TermsAgreement agreement) {
        TermsAgreementJpaEntity saved = jpaRepository.save(mapper.toJpa(agreement));
        return mapper.toDomain(saved);
    }

    /**
     * 여러 약관 동의 정보를 일괄 저장한다.
     *
     * @param agreements 저장할 약관 동의 엔티티 목록
     * @return 저장된 약관 동의 엔티티 목록
     */
    @Override
    public List<TermsAgreement> saveAll(List<TermsAgreement> agreements) {
        List<TermsAgreementJpaEntity> jpaEntities = agreements.stream()
                .map(mapper::toJpa)
                .toList();
        return jpaRepository.saveAll(jpaEntities).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * 사용자 ID와 약관 유형으로 약관 동의 정보를 조회한다.
     *
     * @param userId 사용자 ID
     * @param termsType 약관 유형
     * @return 약관 동의 정보 (Optional)
     */
    @Override
    public Optional<TermsAgreement> findByUserIdAndTermsType(Long userId, TermsType termsType) {
        return jpaRepository.findByUserIdAndTermsType(userId, termsType).map(mapper::toDomain);
    }

    /**
     * 사용자 ID로 모든 약관 동의 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 약관 동의 목록
     */
    @Override
    public List<TermsAgreement> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * 사용자가 필수 약관에 모두 동의했는지 확인한다.
     *
     * @param userId 사용자 ID
     * @return 필수 약관 동의 여부
     */
    @Override
    public boolean hasAgreedToRequiredTerms(Long userId) {
        return jpaRepository.hasAgreedToRequiredTerms(userId);
    }

    /**
     * 사용자 ID로 모든 약관 동의 정보를 삭제한다.
     *
     * @param userId 사용자 ID
     */
    @Override
    public void deleteByUserId(Long userId) {
        jpaRepository.deleteByUserId(userId);
    }
}
