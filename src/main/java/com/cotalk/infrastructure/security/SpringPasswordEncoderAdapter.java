package com.cotalk.infrastructure.security;

import com.cotalk.domain.port.outbound.PasswordEncoderPort;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordEncoderPort의 Spring Security PasswordEncoder 구현 어댑터.
 */
@Component
public class SpringPasswordEncoderAdapter implements PasswordEncoderPort {

    private final PasswordEncoder delegate;

    public SpringPasswordEncoderAdapter(PasswordEncoder delegate) {
        this.delegate = delegate;
    }

    @Override
    public String encode(String rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return delegate.matches(rawPassword, encodedPassword);
    }
}
