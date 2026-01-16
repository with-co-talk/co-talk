package com.cotalk.domain.exception;

public class DuplicateEmailException extends DomainException {

    public DuplicateEmailException(String email) {
        super("이미 존재하는 이메일입니다: " + email);
    }

    public DuplicateEmailException() {
        super("이미 존재하는 이메일입니다.");
    }
}
