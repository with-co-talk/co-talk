package com.cotalk.domain.port.inbound;

/**
 * 비밀번호 재설정 요청 유즈케이스
 */
public interface RequestPasswordResetUseCase {

    /**
     * 비밀번호 재설정 이메일 발송 요청
     * 
     * @param email 사용자 이메일
     */
    void requestPasswordReset(String email);
}
