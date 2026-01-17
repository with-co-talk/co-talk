package com.cotalk.domain.port.inbound.auth;

/**
 * 로그인 유스케이스.
 * 사용자 인증을 처리한다.
 *
 * @author seunggu.lee
 */
public interface LoginUseCase {

    /**
     * 이메일과 비밀번호로 로그인한다.
     *
     * @param email 사용자 이메일
     * @param password 비밀번호
     * @return JWT 토큰
     */
    String login(String email, String password);
}
