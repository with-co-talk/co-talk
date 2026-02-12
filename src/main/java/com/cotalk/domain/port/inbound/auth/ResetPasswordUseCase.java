package com.cotalk.domain.port.inbound.auth;

/**
 * 비밀번호 재설정 유스케이스.
 * 토큰을 이용한 비밀번호 재설정을 처리한다.
 *
 * @author seunggu.lee
 */
public interface ResetPasswordUseCase {

    /**
     * 비밀번호를 재설정한다.
     *
     * @param token 재설정 토큰
     * @param newPassword 새 비밀번호
     */
    void resetPassword(String token, String newPassword);

    /**
     * 재설정 토큰의 유효성을 검증한다.
     *
     * @param token 재설정 토큰
     * @return 유효 여부
     */
    boolean validateToken(String token);

    /**
     * 이메일과 인증 코드의 유효성을 검증한다.
     *
     * @param email 이메일 주소
     * @param code 6자리 인증 코드
     * @return 유효 여부
     */
    boolean verifyCode(String email, String code);

    /**
     * 인증 코드를 이용하여 비밀번호를 재설정한다.
     *
     * @param email 이메일 주소
     * @param code 6자리 인증 코드
     * @param newPassword 새 비밀번호
     */
    void resetPasswordWithCode(String email, String code, String newPassword);
}
