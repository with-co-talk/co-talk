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
     * 이메일로 가장 최근에 발급된 미사용 비밀번호 재설정 토큰을 조회한다.
     * <p>
     * 인증 코드를 조회 키에서 제외하여, 코드 불일치 시에도 토큰을 찾아
     * 실패 횟수를 집계할 수 있도록 한다.
     * </p>
     *
     * @param email 이메일 주소
     * @return 조회된 비밀번호 재설정 토큰 (Optional)
     */
    Optional<PasswordResetToken> findLatestActiveByEmail(String email);

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
