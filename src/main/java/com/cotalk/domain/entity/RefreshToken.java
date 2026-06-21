package com.cotalk.domain.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Refresh Token 도메인 엔티티.
 * JWT Access Token 갱신을 위한 Refresh Token 정보를 나타낸다.
 *
 * <p>Refresh Token은 Access Token보다 긴 만료 시간을 가지며,
 * Access Token 만료 시 새로운 Access Token을 발급받는 데 사용된다.
 *
 * <p>순수 도메인 모델이며 JPA 어노테이션은 persistence 계층에만 존재한다.
 *
 * @author seunggu.lee
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class RefreshToken extends DomainBaseEntity {

    private Long id;

    private Long userId;

    private String token;

    private LocalDateTime expiresAt;

    @Builder.Default
    private boolean revoked = false;

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
     * 토큰이 유효한지 확인한다.
     * 만료되지 않고 폐기되지 않은 토큰만 유효하다.
     *
     * @param now 현재 시간
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public boolean isValid(LocalDateTime now) {
        return !isExpired(now) && !revoked;
    }

    /**
     * 토큰을 폐기한다.
     * 폐기된 토큰은 더 이상 사용할 수 없다.
     */
    public void revoke() {
        this.revoked = true;
    }
}
