package com.cotalk.domain.validator;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 사용자 유효성 검증기.
 * <p>
 * 사용자 관련 입력값의 유효성을 검증한다.
 * 이메일 형식, 비밀번호 강도, 닉네임 등의 유효성을 검증하는 역할을 담당한다.
 * </p>
 *
 * @author seunggu.lee
 */
@Component
public class UserValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final int MIN_PASSWORD_LENGTH = 8;

    /**
     * 이메일 형식을 검증합니다.
     *
     * @param email 검증할 이메일
     * @throws IllegalArgumentException 이메일 형식이 올바르지 않은 경우
     */
    public void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }
    }

    /**
     * 비밀번호 길이를 검증합니다.
     *
     * @param password 검증할 비밀번호
     * @throws IllegalArgumentException 비밀번호가 최소 길이 미만인 경우
     */
    public void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다.");
        }
    }

    /**
     * 닉네임이 비어있지 않은지 검증합니다.
     *
     * @param nickname 검증할 닉네임
     * @throws IllegalArgumentException 닉네임이 비어있는 경우
     */
    public void validateNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 비어있을 수 없습니다.");
        }
    }
}
