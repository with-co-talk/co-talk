package com.cotalk.domain.port.inbound.auth;

/**
 * 이메일 인증 유스케이스.
 * 토큰을 사용하여 이메일 인증을 처리한다.
 *
 * @author seunggu.lee
 */
public interface VerifyEmailUseCase {

    /**
     * 토큰을 사용하여 이메일 인증을 완료한다.
     *
     * @param token 이메일 인증 토큰
     */
    void verifyEmail(String token);
}
