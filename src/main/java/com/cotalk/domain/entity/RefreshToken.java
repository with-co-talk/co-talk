package com.cotalk.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Refresh Token 엔티티.
 * JWT Access Token 갱신을 위한 Refresh Token 정보를 저장한다.
 *
 * <p>Refresh Token은 Access Token보다 긴 만료 시간을 가지며,
 * Access Token 만료 시 새로운 Access Token을 발급받는 데 사용된다.
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token_user_id", columnList = "user_id"),
        @Index(name = "idx_refresh_token_token", columnList = "token")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RefreshToken extends BaseEntity {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    /**
     * 토큰이 만료되었는지 확인한다.
     *
     * @return 만료되었으면 true, 그렇지 않으면 false
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 토큰이 유효한지 확인한다.
     * 만료되지 않고 폐기되지 않은 토큰만 유효하다.
     *
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public boolean isValid() {
        return !isExpired() && !revoked;
    }

    /**
     * 토큰을 폐기한다.
     * 폐기된 토큰은 더 이상 사용할 수 없다.
     */
    public void revoke() {
        this.revoked = true;
    }
}
