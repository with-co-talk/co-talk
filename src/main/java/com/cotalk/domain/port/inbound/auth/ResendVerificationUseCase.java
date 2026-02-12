package com.cotalk.domain.port.inbound.auth;

/**
 * 인증 이메일 재발송 유스케이스.
 * 이메일 인증이 완료되지 않은 사용자에게 인증 이메일을 재발송한다.
 *
 * @author seunggu.lee
 */
public interface ResendVerificationUseCase {

    /**
     * 이메일 인증 이메일을 재발송한다.
     *
     * @param email 수신자 이메일 주소
     */
    void resendVerification(String email);
}
