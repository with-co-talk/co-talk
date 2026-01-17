package com.cotalk.infrastructure.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * JWT 인증을 위한 커스텀 UserDetails 구현체.
 * SecurityContext에 저장되어 인증된 사용자 정보를 제공한다.
 *
 * @author seunggu.lee
 */
public class CustomUserPrincipal implements UserDetails {

    private final Long userId;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserPrincipal(Long userId, String role, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.role = role;
        this.authorities = authorities;
    }

    /**
     * 사용자 ID를 반환한다.
     *
     * @return 사용자 ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 사용자 역할을 반환한다.
     *
     * @return 사용자 역할 (USER, ADMIN 등)
     */
    public String getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return ""; // JWT 인증에서는 비밀번호 불필요
    }

    @Override
    public String getUsername() {
        return userId.toString();
    }
}
