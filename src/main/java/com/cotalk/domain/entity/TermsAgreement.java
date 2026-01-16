package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 이용약관 및 개인정보처리방침 동의 기록
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

    public enum TermsType {
        SERVICE("서비스 이용약관"),
        PRIVACY("개인정보 처리방침"),
        MARKETING("마케팅 정보 수신");

        private final String description;

        TermsType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (agreed) {
            agreedAt = LocalDateTime.now();
        }
    }

    public static TermsAgreement create(Long userId, TermsType type, String version, boolean agreed, String ipAddress) {
        return TermsAgreement.builder()
                .userId(userId)
                .termsType(type)
                .termsVersion(version)
                .agreed(agreed)
                .ipAddress(ipAddress)
                .build();
    }

    public void withdraw() {
        this.agreed = false;
        this.withdrawnAt = LocalDateTime.now();
    }

    public boolean isRequired() {
        return termsType == TermsType.SERVICE || termsType == TermsType.PRIVACY;
    }
}
