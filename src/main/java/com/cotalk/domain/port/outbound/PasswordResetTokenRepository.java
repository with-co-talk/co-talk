package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenRepository {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUserId(Long userId);

    void deleteExpiredTokens();
}
