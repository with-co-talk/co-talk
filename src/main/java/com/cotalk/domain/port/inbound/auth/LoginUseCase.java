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
     * @return 로그인 결과 (Access Token과 사용자 ID)
     * @throws com.cotalk.domain.exception.InvalidCredentialsException 인증 실패 시
     */
    LoginResult login(String email, String password);

    /**
     * 이메일로 사용자 ID를 조회한다.
     *
     * @param email 사용자 이메일
     * @return 사용자 ID
     * @throws com.cotalk.domain.exception.UserNotFoundException 사용자가 존재하지 않는 경우
     */
    Long getUserIdByEmail(String email);
}
