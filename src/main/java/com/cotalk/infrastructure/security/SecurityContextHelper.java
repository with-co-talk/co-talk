package com.cotalk.infrastructure.security;

import com.cotalk.domain.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Spring Security Context 헬퍼.
 * 현재 인증된 사용자 정보에 접근하기 위한 유틸리티 클래스.
 *
 * @author seunggu.lee
 */
@Component
public class SecurityContextHelper {

    /**
     * 현재 인증된 사용자의 ID를 반환한다.
     *
     * @return 현재 사용자 ID
     * @throws UnauthorizedException 인증되지 않은 경우
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("인증이 필요합니다.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserPrincipal customUserPrincipal) {
            return customUserPrincipal.getUserId();
        }

        if (principal instanceof Long) {
            return (Long) principal;
        }

        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                throw new UnauthorizedException("유효하지 않은 인증 정보입니다.");
            }
        }

        throw new UnauthorizedException("유효하지 않은 인증 정보입니다.");
    }

    /**
     * 현재 사용자가 인증되어 있는지 확인한다.
     *
     * @return 인증되어 있으면 true, 그렇지 않으면 false
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }
}
