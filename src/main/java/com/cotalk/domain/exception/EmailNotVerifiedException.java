package com.cotalk.domain.exception;

/**
 * 이메일 미인증 시 발생하는 예외.
 *
 * @author seunggu.lee
 */
public class EmailNotVerifiedException extends DomainException {

    private final String email;

    public EmailNotVerifiedException(String email) {
        super("이메일 인증이 완료되지 않았습니다. 이메일을 확인해주세요.", "EMAIL_NOT_VERIFIED", HttpStatusHint.FORBIDDEN);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
