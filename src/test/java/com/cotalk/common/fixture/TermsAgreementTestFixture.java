package com.cotalk.common.fixture;

import com.cotalk.domain.entity.TermsAgreement;

import java.time.LocalDateTime;

/**
 * TermsAgreement 엔티티 테스트 픽스처.
 * 테스트에서 반복적으로 사용되는 TermsAgreement 객체 생성 메서드를 제공한다.
 *
 * @author seunggu.lee
 */
public class TermsAgreementTestFixture {

    private static final Long DEFAULT_USER_ID = 1L;
    private static final String DEFAULT_VERSION = "1.0";
    private static final String DEFAULT_IP_ADDRESS = "192.168.1.1";
    private static final LocalDateTime DEFAULT_NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    /**
     * 서비스 이용약관 동의를 생성한다.
     *
     * @param userId 사용자 ID
     * @return 서비스 이용약관에 동의한 TermsAgreement 엔티티
     */
    public static TermsAgreement createServiceAgreement(Long userId) {
        return TermsAgreement.create(
                userId, TermsAgreement.TermsType.SERVICE, DEFAULT_VERSION,
                true, DEFAULT_IP_ADDRESS, DEFAULT_NOW
        );
    }

    /**
     * 개인정보 처리방침 동의를 생성한다.
     *
     * @param userId 사용자 ID
     * @return 개인정보 처리방침에 동의한 TermsAgreement 엔티티
     */
    public static TermsAgreement createPrivacyAgreement(Long userId) {
        return TermsAgreement.create(
                userId, TermsAgreement.TermsType.PRIVACY, DEFAULT_VERSION,
                true, DEFAULT_IP_ADDRESS, DEFAULT_NOW
        );
    }

    /**
     * 마케팅 동의를 생성한다.
     *
     * @param userId 사용자 ID
     * @return 마케팅에 동의한 TermsAgreement 엔티티
     */
    public static TermsAgreement createMarketingAgreement(Long userId) {
        return TermsAgreement.create(
                userId, TermsAgreement.TermsType.MARKETING, DEFAULT_VERSION,
                true, DEFAULT_IP_ADDRESS, DEFAULT_NOW
        );
    }

    /**
     * 모든 필수 약관(서비스 + 개인정보)에 동의한 목록을 생성한다.
     *
     * @param userId 사용자 ID
     * @return 필수 약관 동의 배열
     */
    public static TermsAgreement[] createRequiredAgreements(Long userId) {
        return new TermsAgreement[]{
                createServiceAgreement(userId),
                createPrivacyAgreement(userId)
        };
    }

    /**
     * 빌더 스타일로 TermsAgreement 생성을 시작한다.
     *
     * @return TermsAgreementBuilder 인스턴스
     */
    public static TermsAgreementBuilder builder() {
        return new TermsAgreementBuilder();
    }

    /**
     * TermsAgreement 테스트 빌더.
     */
    public static class TermsAgreementBuilder {
        private Long id = null;
        private Long userId = DEFAULT_USER_ID;
        private TermsAgreement.TermsType termsType = TermsAgreement.TermsType.SERVICE;
        private String termsVersion = DEFAULT_VERSION;
        private boolean agreed = true;
        private LocalDateTime agreedAt = DEFAULT_NOW;
        private LocalDateTime withdrawnAt = null;
        private String ipAddress = DEFAULT_IP_ADDRESS;

        /**
         * ID를 설정한다.
         *
         * @param id 약관 동의 ID
         * @return 빌더
         */
        public TermsAgreementBuilder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * 사용자 ID를 설정한다.
         *
         * @param userId 사용자 ID
         * @return 빌더
         */
        public TermsAgreementBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 약관 유형을 설정한다.
         *
         * @param termsType 약관 유형
         * @return 빌더
         */
        public TermsAgreementBuilder termsType(TermsAgreement.TermsType termsType) {
            this.termsType = termsType;
            return this;
        }

        /**
         * 약관 버전을 설정한다.
         *
         * @param termsVersion 약관 버전
         * @return 빌더
         */
        public TermsAgreementBuilder termsVersion(String termsVersion) {
            this.termsVersion = termsVersion;
            return this;
        }

        /**
         * 동의 여부를 설정한다.
         *
         * @param agreed 동의 여부
         * @return 빌더
         */
        public TermsAgreementBuilder agreed(boolean agreed) {
            this.agreed = agreed;
            return this;
        }

        /**
         * 동의 시간을 설정한다.
         *
         * @param agreedAt 동의 시간
         * @return 빌더
         */
        public TermsAgreementBuilder agreedAt(LocalDateTime agreedAt) {
            this.agreedAt = agreedAt;
            return this;
        }

        /**
         * 철회 시간을 설정한다.
         *
         * @param withdrawnAt 철회 시간
         * @return 빌더
         */
        public TermsAgreementBuilder withdrawnAt(LocalDateTime withdrawnAt) {
            this.withdrawnAt = withdrawnAt;
            return this;
        }

        /**
         * IP 주소를 설정한다.
         *
         * @param ipAddress IP 주소
         * @return 빌더
         */
        public TermsAgreementBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        /**
         * TermsAgreement 객체를 생성한다.
         *
         * @return 생성된 TermsAgreement 엔티티
         */
        public TermsAgreement build() {
            return TermsAgreement.builder()
                    .id(id)
                    .userId(userId)
                    .termsType(termsType)
                    .termsVersion(termsVersion)
                    .agreed(agreed)
                    .agreedAt(agreedAt)
                    .withdrawnAt(withdrawnAt)
                    .ipAddress(ipAddress)
                    .build();
        }
    }
}
