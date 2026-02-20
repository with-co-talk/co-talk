package com.cotalk.domain.entity;

import com.cotalk.domain.model.Email;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 이메일 인증 토큰 엔티티.
 * 사용자의 이메일 인증 요청에 대한 토큰 정보를 나타낸다.
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "email_verification_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EmailVerificationToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Email email;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /**
     * 이메일 인증 토큰을 생성한다.
     *
     * @param userId            사용자 ID
     * @param email             이메일 값 객체
     * @param expirationMinutes 만료 시간 (분 단위)
     * @param now               현재 시간
     * @return 생성된 EmailVerificationToken 인스턴스
     */
    public static EmailVerificationToken create(Long userId, Email email, int expirationMinutes, LocalDateTime now) {
        return EmailVerificationToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .email(email)
                .expiresAt(now.plusMinutes(expirationMinutes))
                .build();
    }

    /**
     * 토큰이 만료되었는지 확인한다.
     *
     * @param now 현재 시간
     * @return 만료되었으면 true, 그렇지 않으면 false
     */
    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    /**
     * 토큰이 인증 완료되었는지 확인한다.
     *
     * @return 인증 완료되었으면 true, 그렇지 않으면 false
     */
    public boolean isVerified() {
        return verifiedAt != null;
    }

    /**
     * 토큰이 유효한지 확인한다.
     * 만료되지 않고 인증되지 않은 경우 유효하다.
     *
     * @param now 현재 시간
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public boolean isValid(LocalDateTime now) {
        return !isExpired(now) && !isVerified();
    }

    /**
     * 토큰을 인증 완료로 표시한다.
     *
     * @param now 현재 시간
     */
    public void markAsVerified(LocalDateTime now) {
        this.verifiedAt = now;
    }
}
