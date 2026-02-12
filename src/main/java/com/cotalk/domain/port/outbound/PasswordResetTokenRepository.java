package com.cotalk.domain.port.outbound;

import com.cotalk.domain.entity.PasswordResetToken;

import java.util.Optional;

/**
 * 비밀번호 재설정 토큰 레포지토리 포트.
 * 비밀번호 재설정 토큰 데이터 저장 및 조회를 위한 인터페이스를 정의한다.
 *
 * @author seunggu.lee
 */
public interface PasswordResetTokenRepository {

    /**
     * 비밀번호 재설정 토큰을 저장한다.
     *
     * @param token 저장할 비밀번호 재설정 토큰
     * @return 저장된 비밀번호 재설정 토큰
     */
    PasswordResetToken save(PasswordResetToken token);

    /**
     * 토큰 문자열로 비밀번호 재설정 토큰을 조회한다.
     *
     * @param token 토큰 문자열
     * @return 조회된 비밀번호 재설정 토큰 (Optional)
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * 이메일과 인증 코드로 비밀번호 재설정 토큰을 조회한다.
     *
     * @param email 이메일 주소
     * @param verificationCode 6자리 인증 코드
     * @return 조회된 비밀번호 재설정 토큰 (Optional)
     */
    Optional<PasswordResetToken> findByEmailAndVerificationCode(String email, String verificationCode);

    /**
     * 특정 사용자의 모든 비밀번호 재설정 토큰을 삭제한다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);

    /**
     * 만료된 모든 비밀번호 재설정 토큰을 삭제한다.
     */
    void deleteExpiredTokens();
}
