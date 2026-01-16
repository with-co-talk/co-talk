package com.cotalk.domain.exception;

/**
 * 중복된 닉네임일 때 발생하는 예외
 */
public class DuplicateNicknameException extends DomainException {

    public DuplicateNicknameException() {
        super("이미 사용 중인 닉네임입니다.");
    }

    public DuplicateNicknameException(String nickname) {
        super("이미 사용 중인 닉네임입니다: " + nickname);
    }
}
