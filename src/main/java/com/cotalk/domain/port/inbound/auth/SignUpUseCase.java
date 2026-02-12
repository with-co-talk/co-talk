package com.cotalk.domain.port.inbound.auth;

/**
 * 회원가입 유스케이스.
 * 새로운 사용자 등록을 처리한다.
 *
 * @author seunggu.lee
 */
public interface SignUpUseCase {

    Long signUp(String email, String password, String nickname);

    default Long signUp(String email, String password, String nickname, String phoneNumber) {
        return signUp(email, password, nickname);
    }
}
