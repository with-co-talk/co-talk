package com.cotalk.domain.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 이용약관 동의 도메인 엔티티.
 * 이용약관 및 개인정보처리방침 동의 기록을 나타낸다.
 * 순수 도메인 모델이며 JPA 어노테이션은 persistence 계층에만 존재한다.
 *
 * @author seunggu.lee
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class TermsAgreement extends DomainBaseEntity {

    private Long id;

    private Long userId;

    private TermsType termsType;

    private String termsVersion;

    private boolean agreed;

    private LocalDateTime agreedAt;

    private LocalDateTime withdrawnAt;

    private String ipAddress;

    /**
     * 약관 유형을 나타내는 열거형.
     *
     * @author seunggu.lee
     */
    public enum TermsType {
        /** 서비스 이용약관 */
        SERVICE("서비스 이용약관"),
        /** 개인정보 처리방침 */
        PRIVACY("개인정보 처리방침"),
        /** 마케팅 정보 수신 */
        MARKETING("마케팅 정보 수신");

        private final String description;

        /**
         * TermsType 생성자.
         *
         * @param description 약관 유형 설명
         */
        TermsType(String description) {
            this.description = description;
        }

        /**
         * 약관 유형 설명을 반환한다.
         *
         * @return 약관 유형 설명
         */
        public String getDescription() {
            return description;
        }
    }

    /**
     * 이용약관 동의 기록을 생성한다.
     *
     * @param userId 사용자 ID
     * @param type 약관 유형
     * @param version 약관 버전
     * @param agreed 동의 여부
     * @param ipAddress IP 주소
     * @param now 현재 시간
     * @return 생성된 TermsAgreement 인스턴스
     */
    public static TermsAgreement create(Long userId, TermsType type, String version, boolean agreed, String ipAddress, LocalDateTime now) {
        TermsAgreement agreement = TermsAgreement.builder()
                .userId(userId)
                .termsType(type)
                .termsVersion(version)
                .agreed(agreed)
                .ipAddress(ipAddress)
                .build();

        // 동의한 경우 동의 시간 설정
        if (agreed) {
            agreement.agreedAt = now;
        }

        return agreement;
    }

    /**
     * 약관 동의를 철회한다.
     *
     * @param now 현재 시간
     */
    public void withdraw(LocalDateTime now) {
        this.agreed = false;
        this.withdrawnAt = now;
    }

    /**
     * 필수 약관인지 확인한다.
     * 서비스 이용약관과 개인정보 처리방침은 필수 약관이다.
     *
     * @return 필수 약관이면 true, 그렇지 않으면 false
     */
    public boolean isRequired() {
        return termsType == TermsType.SERVICE || termsType == TermsType.PRIVACY;
    }
}
