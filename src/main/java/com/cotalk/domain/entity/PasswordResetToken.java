package com.cotalk.domain.entity;

import com.cotalk.domain.model.Email;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 비밀번호 재설정 토큰 도메인 엔티티.
 * 사용자의 비밀번호 재설정 요청에 대한 토큰 정보를 나타낸다.
 * 순수 도메인 모델이며 JPA 어노테이션은 persistence 계층에만 존재한다.
 *
 * @author seunggu.lee
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class PasswordResetToken extends DomainBaseEntity {

    private Long id;

    private String token;

    private Long userId;

    private Email email;

    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;

    private String verificationCode;

    /**
     * 인증 코드 입력 실패 횟수.
     * 최대 허용 횟수를 초과하면 토큰이 무효화된다.
     */
    @Builder.Default
    private int failedAttempts = 0;

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

    /**
     * 인증 코드 입력 실패 횟수를 1 증가시킨다.
     */
    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }

    /**
     * 주어진 인증 코드가 이 토큰의 인증 코드와 일치하는지 확인한다.
     * <p>
     * 인증 코드를 조회 키로 사용하지 않고 도메인에서 비교함으로써,
     * 잘못된 코드 입력도 실패 횟수에 집계하여 무차별 대입을 차단한다.
     * </p>
     *
     * @param code 사용자가 입력한 인증 코드
     * @return 인증 코드가 존재하고 일치하면 true, 그렇지 않으면 false
     */
    public boolean matchesCode(String code) {
        return this.verificationCode != null && this.verificationCode.equals(code);
    }

    /**
     * 최대 허용 실패 횟수를 초과했는지 확인한다.
     *
     * @param maxAttempts 최대 허용 실패 횟수
     * @return 초과했으면 true, 그렇지 않으면 false
     */
    public boolean isMaxAttemptsExceeded(int maxAttempts) {
        return this.failedAttempts >= maxAttempts;
    }
}
