package com.cotalk.domain.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * 강력한 비밀번호 정책 검증기.
 *
 * <p>비밀번호 요구사항:</p>
 * <ul>
 *     <li>최소 8자 이상</li>
 *     <li>대문자 1개 이상</li>
 *     <li>소문자 1개 이상</li>
 *     <li>숫자 1개 이상</li>
 *     <li>특수문자 1개 이상</li>
 * </ul>
 *
 * @author seunggu.lee
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    /**
     * 비밀번호 정규식 패턴.
     * - 최소 8자
     * - 대문자 1개 이상
     * - 소문자 1개 이상
     * - 숫자 1개 이상
     * - 특수문자 1개 이상
     */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$"
    );

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        // 초기화 로직 불필요
    }

    /**
     * 비밀번호가 강력한 비밀번호 정책을 만족하는지 검증한다.
     *
     * @param password 검증할 비밀번호
     * @param context  검증 컨텍스트
     * @return 유효하면 true, 그렇지 않으면 false
     */
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}
