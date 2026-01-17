package com.cotalk.infrastructure.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

/**
 * WithMockCustomUser 어노테이션을 처리하여 SecurityContext를 생성하는 팩토리.
 */
public class WithMockCustomUserSecurityContextFactory
        implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        CustomUserPrincipal principal = new CustomUserPrincipal(
                annotation.userId(),
                annotation.role(),
                List.of(new SimpleGrantedAuthority("ROLE_" + annotation.role()))
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        context.setAuthentication(authentication);
        return context;
    }
}
