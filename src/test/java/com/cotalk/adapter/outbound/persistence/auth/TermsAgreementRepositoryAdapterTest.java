package com.cotalk.adapter.outbound.persistence.auth;

import com.cotalk.domain.entity.TermsAgreement;
import com.cotalk.domain.entity.TermsAgreement.TermsType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TermsAgreementRepositoryAdapter 단위 테스트.
 *
 * @author seunggu.lee
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TermsAgreementRepositoryAdapter")
class TermsAgreementRepositoryAdapterTest {

    @Mock
    private TermsAgreementJpaRepository jpaRepository;

    @InjectMocks
    private TermsAgreementRepositoryAdapter adapter;

    private TermsAgreement serviceAgreement;
    private TermsAgreement privacyAgreement;

    @BeforeEach
    void setUp() {
        serviceAgreement = TermsAgreement.builder()
                .id(1L)
                .userId(100L)
                .termsType(TermsType.SERVICE)
                .termsVersion("1.0")
                .agreed(true)
                .agreedAt(LocalDateTime.now())
                .build();

        privacyAgreement = TermsAgreement.builder()
                .id(2L)
                .userId(100L)
                .termsType(TermsType.PRIVACY)
                .termsVersion("1.0")
                .agreed(true)
                .agreedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("save 메서드")
    class SaveMethod {

        @Test
        @DisplayName("약관 동의를 저장하고 반환한다")
        void should_saveAgreement_when_agreementProvided() {
            // given
            when(jpaRepository.save(serviceAgreement)).thenReturn(serviceAgreement);

            // when
            TermsAgreement result = adapter.save(serviceAgreement);

            // then
            assertThat(result).isEqualTo(serviceAgreement);
            verify(jpaRepository).save(serviceAgreement);
        }
    }

    @Nested
    @DisplayName("saveAll 메서드")
    class SaveAllMethod {

        @Test
        @DisplayName("여러 약관 동의를 일괄 저장한다")
        void should_saveAllAgreements_when_agreementsProvided() {
            // given
            List<TermsAgreement> agreements = List.of(serviceAgreement, privacyAgreement);
            when(jpaRepository.saveAll(agreements)).thenReturn(agreements);

            // when
            List<TermsAgreement> result = adapter.saveAll(agreements);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(serviceAgreement, privacyAgreement);
            verify(jpaRepository).saveAll(agreements);
        }
    }

    @Nested
    @DisplayName("findByUserIdAndTermsType 메서드")
    class FindByUserIdAndTermsTypeMethod {

        @Test
        @DisplayName("사용자 ID와 약관 유형으로 조회한다")
        void should_findAgreement_when_userIdAndTermsTypeProvided() {
            // given
            Long userId = 100L;
            when(jpaRepository.findByUserIdAndTermsType(userId, TermsType.SERVICE))
                    .thenReturn(Optional.of(serviceAgreement));

            // when
            Optional<TermsAgreement> result = adapter.findByUserIdAndTermsType(userId, TermsType.SERVICE);

            // then
            assertThat(result).contains(serviceAgreement);
            verify(jpaRepository).findByUserIdAndTermsType(userId, TermsType.SERVICE);
        }

        @Test
        @DisplayName("존재하지 않으면 빈 Optional 반환")
        void should_returnEmpty_when_agreementNotExists() {
            // given
            Long userId = 100L;
            when(jpaRepository.findByUserIdAndTermsType(userId, TermsType.MARKETING))
                    .thenReturn(Optional.empty());

            // when
            Optional<TermsAgreement> result = adapter.findByUserIdAndTermsType(userId, TermsType.MARKETING);

            // then
            assertThat(result).isEmpty();
            verify(jpaRepository).findByUserIdAndTermsType(userId, TermsType.MARKETING);
        }
    }

    @Nested
    @DisplayName("findByUserId 메서드")
    class FindByUserIdMethod {

        @Test
        @DisplayName("사용자 ID로 모든 약관 동의 목록을 조회한다")
        void should_findAllAgreements_when_userIdProvided() {
            // given
            Long userId = 100L;
            List<TermsAgreement> agreements = List.of(serviceAgreement, privacyAgreement);
            when(jpaRepository.findByUserId(userId)).thenReturn(agreements);

            // when
            List<TermsAgreement> result = adapter.findByUserId(userId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(serviceAgreement, privacyAgreement);
            verify(jpaRepository).findByUserId(userId);
        }

        @Test
        @DisplayName("약관 동의가 없으면 빈 목록 반환")
        void should_returnEmptyList_when_noAgreements() {
            // given
            Long userId = 999L;
            when(jpaRepository.findByUserId(userId)).thenReturn(List.of());

            // when
            List<TermsAgreement> result = adapter.findByUserId(userId);

            // then
            assertThat(result).isEmpty();
            verify(jpaRepository).findByUserId(userId);
        }
    }

    @Nested
    @DisplayName("hasAgreedToRequiredTerms 메서드")
    class HasAgreedToRequiredTermsMethod {

        @Test
        @DisplayName("필수 약관에 모두 동의하면 true 반환")
        void should_returnTrue_when_agreedToAllRequiredTerms() {
            // given
            Long userId = 100L;
            when(jpaRepository.hasAgreedToRequiredTerms(userId)).thenReturn(true);

            // when
            boolean result = adapter.hasAgreedToRequiredTerms(userId);

            // then
            assertThat(result).isTrue();
            verify(jpaRepository).hasAgreedToRequiredTerms(userId);
        }

        @Test
        @DisplayName("필수 약관에 동의하지 않으면 false 반환")
        void should_returnFalse_when_notAgreedToAllRequiredTerms() {
            // given
            Long userId = 100L;
            when(jpaRepository.hasAgreedToRequiredTerms(userId)).thenReturn(false);

            // when
            boolean result = adapter.hasAgreedToRequiredTerms(userId);

            // then
            assertThat(result).isFalse();
            verify(jpaRepository).hasAgreedToRequiredTerms(userId);
        }
    }

    @Nested
    @DisplayName("deleteByUserId 메서드")
    class DeleteByUserIdMethod {

        @Test
        @DisplayName("사용자 ID로 모든 약관 동의를 삭제한다")
        void should_deleteAllAgreements_when_userIdProvided() {
            // given
            Long userId = 100L;

            // when
            adapter.deleteByUserId(userId);

            // then
            verify(jpaRepository).deleteByUserId(userId);
        }
    }
}
