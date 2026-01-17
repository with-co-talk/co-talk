package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 이용약관 동의 엔티티.
 * 이용약관 및 개인정보처리방침 동의 기록을 나타낸다.
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "terms_agreements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TermsAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "terms_type", nullable = false)
    private TermsType termsType;

    @Column(name = "terms_version", nullable = false)
    private String termsVersion;

    @Column(nullable = false)
    private boolean agreed;

    @Column(name = "agreed_at")
    private LocalDateTime agreedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
     * 엔티티 생성 시 호출되는 콜백 메서드.
     * 생성 시간을 현재 시간으로 설정하고, 동의한 경우 동의 시간도 설정한다.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (agreed) {
            agreedAt = LocalDateTime.now();
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
     * @return 생성된 TermsAgreement 인스턴스
     */
    public static TermsAgreement create(Long userId, TermsType type, String version, boolean agreed, String ipAddress) {
        return TermsAgreement.builder()
                .userId(userId)
                .termsType(type)
                .termsVersion(version)
                .agreed(agreed)
                .ipAddress(ipAddress)
                .build();
    }

    /**
     * 약관 동의를 철회한다.
     */
    public void withdraw() {
        this.agreed = false;
        this.withdrawnAt = LocalDateTime.now();
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
