package com.cotalk.domain.port.inbound.auth;

/**
 * 비밀번호 재설정 요청 유스케이스.
 * 비밀번호 재설정 이메일 발송을 처리한다.
 *
 * @author seunggu.lee
 */
public interface RequestPasswordResetUseCase {

    /**
     * 비밀번호 재설정 이메일을 발송한다.
     *
     * @param email 사용자 이메일
     */
    void requestPasswordReset(String email);
}
