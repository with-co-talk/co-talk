package com.cotalk.domain.entity;

import com.cotalk.domain.model.Email;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 비밀번호 재설정 토큰 엔티티.
 * 사용자의 비밀번호 재설정 요청에 대한 토큰 정보를 나타낸다.
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PasswordResetToken extends BaseEntity {

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

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "verification_code", length = 6)
    private String verificationCode;

    /**
     * 비밀번호 재설정 토큰을 생성한다.
     *
     * @param userId 사용자 ID
     * @param email 이메일 값 객체
     * @param expirationMinutes 만료 시간 (분 단위)
     * @param now 현재 시간
     * @return 생성된 PasswordResetToken 인스턴스
     */
    public static PasswordResetToken create(Long userId, Email email, int expirationMinutes, LocalDateTime now) {
        return PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .email(email)
                .expiresAt(now.plusMinutes(expirationMinutes))
                .build();
    }

    /**
     * 6자리 인증 코드가 포함된 비밀번호 재설정 토큰을 생성한다.
     *
     * @param userId 사용자 ID
     * @param email 이메일 값 객체
     * @param expirationMinutes 만료 시간 (분 단위)
     * @param now 현재 시간
     * @return 생성된 PasswordResetToken 인스턴스 (6자리 인증 코드 포함)
     */
    public static PasswordResetToken createWithCode(Long userId, Email email, int expirationMinutes, LocalDateTime now) {
        String code = generateVerificationCode();
        return PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .email(email)
                .verificationCode(code)
                .expiresAt(now.plusMinutes(expirationMinutes))
                .build();
    }

    /**
     * 6자리 숫자 인증 코드를 생성한다.
     *
     * @return 6자리 숫자 문자열
     */
    private static String generateVerificationCode() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
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
     * 토큰이 사용되었는지 확인한다.
     *
     * @return 사용되었으면 true, 그렇지 않으면 false
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * 토큰이 유효한지 확인한다.
     * 만료되지 않고 사용되지 않은 경우 유효하다.
     *
     * @param now 현재 시간
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public boolean isValid(LocalDateTime now) {
        return !isExpired(now) && !isUsed();
    }

    /**
     * 토큰을 사용됨으로 표시한다.
     *
     * @param now 현재 시간
     */
    public void markAsUsed(LocalDateTime now) {
        this.usedAt = now;
    }
}
