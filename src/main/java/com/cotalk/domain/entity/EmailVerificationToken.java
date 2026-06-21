package com.cotalk.domain.entity;

import com.cotalk.domain.model.Email;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 이메일 인증 토큰 도메인 엔티티.
 * 사용자의 이메일 인증 요청에 대한 토큰 정보를 나타낸다.
 * 순수 도메인 모델이며 JPA 어노테이션은 persistence 계층에만 존재한다.
 *
 * @author seunggu.lee
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class EmailVerificationToken extends DomainBaseEntity {

    private Long id;

    private String token;

    private Long userId;

    private Email email;

    private LocalDateTime expiresAt;

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
