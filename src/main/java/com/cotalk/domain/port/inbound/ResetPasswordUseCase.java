package com.cotalk.domain.port.inbound;

/**
 * 비밀번호 재설정 유즈케이스
 */
public interface ResetPasswordUseCase {

    /**
     * 비밀번호 재설정
     *
     * @param token 재설정 토큰
     * @param newPassword 새 비밀번호
     */
    void resetPassword(String token, String newPassword);

    /**
     * 토큰 유효성 검증
     *
     * @param token 재설정 토큰
     * @return 유효 여부
     */
    boolean validateToken(String token);
}
