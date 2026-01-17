package com.cotalk.domain.port.inbound.auth;

/**
 * 회원가입 유스케이스.
 * 새로운 사용자 등록을 처리한다.
 *
 * @author seunggu.lee
 */
public interface SignUpUseCase {

    /**
     * 회원가입을 처리한다.
     *
     * @param email 사용자 이메일
     * @param password 비밀번호
     * @param nickname 닉네임
     * @return 생성된 사용자 ID
     */
    Long signUp(String email, String password, String nickname);
}
