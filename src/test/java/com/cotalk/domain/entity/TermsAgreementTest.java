package com.cotalk.domain.entity;

import com.cotalk.domain.entity.TermsAgreement.TermsType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TermsAgreement 엔티티")
class TermsAgreementTest {

    @Nested
    @DisplayName("create 메서드")
    class Create {

        @Test
        @DisplayName("서비스 이용약관 동의를 생성할 수 있다")
        void should_createServiceTermsAgreement_when_agreed() {
            // given
            Long userId = 1L;
            TermsType type = TermsType.SERVICE;
            String version = "1.0";
            boolean agreed = true;
            String ipAddress = "192.168.1.1";

            // when
            TermsAgreement agreement = TermsAgreement.create(userId, type, version, agreed, ipAddress);

            // then
            assertThat(agreement.getUserId()).isEqualTo(userId);
            assertThat(agreement.getTermsType()).isEqualTo(type);
            assertThat(agreement.getTermsVersion()).isEqualTo(version);
            assertThat(agreement.isAgreed()).isTrue();
            assertThat(agreement.getIpAddress()).isEqualTo(ipAddress);
            assertThat(agreement.getAgreedAt()).isNotNull();
            assertThat(agreement.getWithdrawnAt()).isNull();
        }

        @Test
        @DisplayName("동의하지 않으면 동의 시간이 설정되지 않는다")
        void should_notSetAgreedAt_when_notAgreed() {
            // given
            Long userId = 1L;
            TermsType type = TermsType.SERVICE;
            String version = "1.0";
            boolean agreed = false;
            String ipAddress = "192.168.1.1";

            // when
            TermsAgreement agreement = TermsAgreement.create(userId, type, version, agreed, ipAddress);

            // then
            assertThat(agreement.isAgreed()).isFalse();
            assertThat(agreement.getAgreedAt()).isNull();
        }

        @Test
        @DisplayName("개인정보 처리방침 동의를 생성할 수 있다")
        void should_createPrivacyPolicyAgreement_when_agreed() {
            // given
            Long userId = 1L;
            TermsType type = TermsType.PRIVACY;
            String version = "1.0";
            boolean agreed = true;
            String ipAddress = "192.168.1.1";

            // when
            TermsAgreement agreement = TermsAgreement.create(userId, type, version, agreed, ipAddress);

            // then
            assertThat(agreement.getTermsType()).isEqualTo(TermsType.PRIVACY);
            assertThat(agreement.isAgreed()).isTrue();
        }

        @Test
        @DisplayName("마케팅 동의를 생성할 수 있다")
        void should_createMarketingAgreement_when_agreed() {
            // given
            Long userId = 1L;
            TermsType type = TermsType.MARKETING;
            String version = "1.0";
            boolean agreed = true;
            String ipAddress = "192.168.1.1";

            // when
            TermsAgreement agreement = TermsAgreement.create(userId, type, version, agreed, ipAddress);

            // then
            assertThat(agreement.getTermsType()).isEqualTo(TermsType.MARKETING);
            assertThat(agreement.isAgreed()).isTrue();
        }
    }

    @Nested
    @DisplayName("withdraw 메서드")
    class Withdraw {

        @Test
        @DisplayName("동의를 철회하면 agreed가 false가 된다")
        void should_setAgreedToFalse_when_withdrawn() {
            // given
            TermsAgreement agreement = TermsAgreement.create(
                    1L, TermsType.MARKETING, "1.0", true, "192.168.1.1"
            );

            assertThat(agreement.isAgreed()).isTrue();

            // when
            agreement.withdraw();

            // then
            assertThat(agreement.isAgreed()).isFalse();
        }

        @Test
        @DisplayName("동의를 철회하면 철회 시간이 설정된다")
        void should_setWithdrawnAt_when_withdrawn() {
            // given
            TermsAgreement agreement = TermsAgreement.create(
                    1L, TermsType.MARKETING, "1.0", true, "192.168.1.1"
            );

            assertThat(agreement.getWithdrawnAt()).isNull();
            LocalDateTime beforeWithdraw = LocalDateTime.now();

            // when
            agreement.withdraw();

            // then
            assertThat(agreement.getWithdrawnAt())
                    .isAfterOrEqualTo(beforeWithdraw)
                    .isBeforeOrEqualTo(LocalDateTime.now());
        }
    }

    @Nested
    @DisplayName("isRequired 메서드")
    class IsRequired {

        @Test
        @DisplayName("서비스 이용약관은 필수이다")
        void should_returnTrue_when_serviceTerms() {
            // given
            TermsAgreement agreement = TermsAgreement.create(
                    1L, TermsType.SERVICE, "1.0", true, "192.168.1.1"
            );

            // when
            boolean result = agreement.isRequired();

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("개인정보 처리방침은 필수이다")
        void should_returnTrue_when_privacyPolicy() {
            // given
            TermsAgreement agreement = TermsAgreement.create(
                    1L, TermsType.PRIVACY, "1.0", true, "192.168.1.1"
            );

            // when
            boolean result = agreement.isRequired();

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("마케팅 동의는 필수가 아니다")
        void should_returnFalse_when_marketing() {
            // given
            TermsAgreement agreement = TermsAgreement.create(
                    1L, TermsType.MARKETING, "1.0", true, "192.168.1.1"
            );

            // when
            boolean result = agreement.isRequired();

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("TermsType 열거형")
    class TermsTypeEnum {

        @Test
        @DisplayName("모든 약관 유형이 정의되어 있다")
        void should_haveAllTypes() {
            // then
            assertThat(TermsType.values()).containsExactlyInAnyOrder(
                    TermsType.SERVICE,
                    TermsType.PRIVACY,
                    TermsType.MARKETING
            );
        }

        @Test
        @DisplayName("서비스 이용약관 설명을 반환한다")
        void should_returnDescription_forService() {
            // then
            assertThat(TermsType.SERVICE.getDescription()).isEqualTo("서비스 이용약관");
        }

        @Test
        @DisplayName("개인정보 처리방침 설명을 반환한다")
        void should_returnDescription_forPrivacy() {
            // then
            assertThat(TermsType.PRIVACY.getDescription()).isEqualTo("개인정보 처리방침");
        }

        @Test
        @DisplayName("마케팅 정보 수신 설명을 반환한다")
        void should_returnDescription_forMarketing() {
            // then
            assertThat(TermsType.MARKETING.getDescription()).isEqualTo("마케팅 정보 수신");
        }
    }

    @Nested
    @DisplayName("빌더")
    class Builder {

        @Test
        @DisplayName("빌더를 통해 모든 필드를 설정할 수 있다")
        void should_setAllFields_when_usingBuilder() {
            // given
            Long id = 1L;
            Long userId = 2L;
            TermsType termsType = TermsType.SERVICE;
            String termsVersion = "2.0";
            boolean agreed = true;
            LocalDateTime agreedAt = LocalDateTime.now().minusDays(1);
            LocalDateTime withdrawnAt = LocalDateTime.now();
            String ipAddress = "10.0.0.1";

            // when
            TermsAgreement agreement = TermsAgreement.builder()
                    .id(id)
                    .userId(userId)
                    .termsType(termsType)
                    .termsVersion(termsVersion)
                    .agreed(agreed)
                    .agreedAt(agreedAt)
                    .withdrawnAt(withdrawnAt)
                    .ipAddress(ipAddress)
                    .build();

            // then
            assertThat(agreement.getId()).isEqualTo(id);
            assertThat(agreement.getUserId()).isEqualTo(userId);
            assertThat(agreement.getTermsType()).isEqualTo(termsType);
            assertThat(agreement.getTermsVersion()).isEqualTo(termsVersion);
            assertThat(agreement.isAgreed()).isEqualTo(agreed);
            assertThat(agreement.getAgreedAt()).isEqualTo(agreedAt);
            assertThat(agreement.getWithdrawnAt()).isEqualTo(withdrawnAt);
            assertThat(agreement.getIpAddress()).isEqualTo(ipAddress);
        }
    }
}
