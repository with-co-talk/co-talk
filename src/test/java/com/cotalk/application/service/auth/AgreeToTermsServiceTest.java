package com.cotalk.application.service.auth;

import com.cotalk.domain.entity.TermsAgreement;
import com.cotalk.domain.entity.TermsAgreement.TermsType;
import com.cotalk.domain.exception.DomainException;
import com.cotalk.domain.port.inbound.auth.AgreeToTermsUseCase.*;
import com.cotalk.domain.port.outbound.TermsAgreementRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AgreeToTermsServiceTest {

    @Mock
    private TermsAgreementRepository termsAgreementRepository;

    private AgreeToTermsService service;

    @BeforeEach
    void setUp() {
        String termsServiceVersion = "1.0";
        String termsPrivacyVersion = "1.0";
        service = new AgreeToTermsService(termsAgreementRepository, termsServiceVersion, termsPrivacyVersion);
    }

    @Test
    @DisplayName("필수 약관 동의 성공")
    void should_agreeToTerms_when_requiredTermsAgreed() {
        // given
        Long userId = 1L;
        List<TermsAgreementItem> items = Arrays.asList(
                new TermsAgreementItem(TermsType.SERVICE, "1.0", true),
                new TermsAgreementItem(TermsType.PRIVACY, "1.0", true),
                new TermsAgreementItem(TermsType.MARKETING, "1.0", false)
        );
        TermsAgreementCommand command = new TermsAgreementCommand(userId, items, "127.0.0.1");

        given(termsAgreementRepository.saveAll(anyList())).willReturn(Collections.emptyList());

        // when
        service.agreeToTerms(command);

        // then
        verify(termsAgreementRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("서비스 이용약관 미동의 시 예외")
    void should_throwException_when_serviceTermsNotAgreed() {
        // given
        Long userId = 1L;
        List<TermsAgreementItem> items = Arrays.asList(
                new TermsAgreementItem(TermsType.SERVICE, "1.0", false), // 미동의
                new TermsAgreementItem(TermsType.PRIVACY, "1.0", true)
        );
        TermsAgreementCommand command = new TermsAgreementCommand(userId, items, "127.0.0.1");

        // when & then
        assertThatThrownBy(() -> service.agreeToTerms(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("서비스 이용약관");
    }

    @Test
    @DisplayName("개인정보 처리방침 미동의 시 예외")
    void should_throwException_when_privacyTermsNotAgreed() {
        // given
        Long userId = 1L;
        List<TermsAgreementItem> items = Arrays.asList(
                new TermsAgreementItem(TermsType.SERVICE, "1.0", true),
                new TermsAgreementItem(TermsType.PRIVACY, "1.0", false) // 미동의
        );
        TermsAgreementCommand command = new TermsAgreementCommand(userId, items, "127.0.0.1");

        // when & then
        assertThatThrownBy(() -> service.agreeToTerms(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("개인정보 처리방침");
    }

    @Test
    @DisplayName("마케팅 수신 동의 철회 성공")
    void should_withdrawMarketingAgreement_when_requested() {
        // given
        Long userId = 1L;
        TermsAgreement agreement = TermsAgreement.create(userId, TermsType.MARKETING, "1.0", true, "127.0.0.1");
        
        given(termsAgreementRepository.findByUserIdAndTermsType(userId, TermsType.MARKETING))
                .willReturn(Optional.of(agreement));
        given(termsAgreementRepository.save(any(TermsAgreement.class))).willReturn(agreement);

        // when
        service.withdrawMarketingAgreement(userId);

        // then
        assertThat(agreement.isAgreed()).isFalse();
        verify(termsAgreementRepository).save(agreement);
    }

    @Test
    @DisplayName("마케팅 동의가 없는 경우 철회 시 아무 작업도 하지 않음")
    void should_doNothing_when_marketingAgreementNotFound() {
        // given
        Long userId = 1L;
        given(termsAgreementRepository.findByUserIdAndTermsType(userId, TermsType.MARKETING))
                .willReturn(Optional.empty());

        // when
        service.withdrawMarketingAgreement(userId);

        // then
        verify(termsAgreementRepository, never()).save(any());
    }

    @Test
    @DisplayName("필수 약관 동의 여부 확인")
    void should_returnTrue_when_requiredTermsAgreed() {
        // given
        Long userId = 1L;
        given(termsAgreementRepository.hasAgreedToRequiredTerms(userId)).willReturn(true);

        // when
        boolean result = service.hasAgreedToRequiredTerms(userId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("동의 상태 조회 - 모든 약관 타입 반환")
    void should_returnAllTermsStatus_when_getAgreementStatus() {
        // given
        Long userId = 1L;
        List<TermsAgreement> agreements = Arrays.asList(
                TermsAgreement.create(userId, TermsType.SERVICE, "1.0", true, "127.0.0.1"),
                TermsAgreement.create(userId, TermsType.PRIVACY, "1.0", true, "127.0.0.1")
        );
        given(termsAgreementRepository.findByUserId(userId)).willReturn(agreements);

        // when
        List<TermsAgreementStatus> statusList = service.getAgreementStatus(userId);

        // then
        assertThat(statusList).hasSize(3); // SERVICE, PRIVACY, MARKETING
        
        TermsAgreementStatus serviceStatus = statusList.stream()
                .filter(s -> s.termsType() == TermsType.SERVICE)
                .findFirst()
                .orElseThrow();
        assertThat(serviceStatus.agreed()).isTrue();
        assertThat(serviceStatus.required()).isTrue();

        TermsAgreementStatus marketingStatus = statusList.stream()
                .filter(s -> s.termsType() == TermsType.MARKETING)
                .findFirst()
                .orElseThrow();
        assertThat(marketingStatus.agreed()).isFalse();
        assertThat(marketingStatus.required()).isFalse();
    }
}
