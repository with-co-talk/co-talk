package com.cotalk.domain.exception;

/**
 * 비밀번호가 일치하지 않을 때 발생하는 예외.
 * 로그인이 아닌 비밀번호 확인 시 사용 (회원탈퇴, 비밀번호 변경 등).
 *
 * @author seunggu.lee
 */
public class PasswordMismatchException extends DomainException {

    public PasswordMismatchException() {
        super("비밀번호가 일치하지 않습니다.");
    }

    public PasswordMismatchException(String message) {
        super(message);
    }
}
