package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.EmailVerificationToken;

import java.util.Optional;

/**
 * 이메일 인증 토큰 레포지토리 포트.
 * 이메일 인증 토큰 데이터 저장 및 조회를 위한 인터페이스를 정의한다.
 *
 * @author seunggu.lee
 */
public interface EmailVerificationTokenRepository {

    /**
     * 이메일 인증 토큰을 저장한다.
     *
     * @param token 저장할 이메일 인증 토큰
     * @return 저장된 이메일 인증 토큰
     */
    EmailVerificationToken save(EmailVerificationToken token);

    /**
     * 토큰 문자열로 이메일 인증 토큰을 조회한다.
     *
     * @param token 토큰 문자열
     * @return 조회된 이메일 인증 토큰 (Optional)
     */
    Optional<EmailVerificationToken> findByToken(String token);

    /**
     * 사용자 ID로 가장 최근 이메일 인증 토큰을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 가장 최근 이메일 인증 토큰 (Optional)
     */
    Optional<EmailVerificationToken> findLatestByUserId(Long userId);

    /**
     * 특정 사용자의 모든 이메일 인증 토큰을 삭제한다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);

    /**
     * 만료된 모든 이메일 인증 토큰을 삭제한다.
     */
    void deleteExpiredTokens();
}
