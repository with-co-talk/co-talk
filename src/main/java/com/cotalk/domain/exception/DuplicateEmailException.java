package com.cotalk.domain.exception;

/**
 * 중복된 이메일일 때 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class DuplicateEmailException extends DomainException {

    public DuplicateEmailException(String email) {
        super("이미 존재하는 이메일입니다: " + email);
    }

    public DuplicateEmailException() {
        super("이미 존재하는 이메일입니다.");
    }
}
