package com.cotalk.infrastructure.security;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

/**
 * 비밀번호 강도 검증 어노테이션.
 * 비밀번호가 보안 요구사항을 충족하는지 검증한다.
 *
 * <p>기본 요구사항:
 * <ul>
 *   <li>최소 8자 이상</li>
 *   <li>최대 128자 이하</li>
 *   <li>대문자 1개 이상</li>
 *   <li>소문자 1개 이상</li>
 *   <li>숫자 1개 이상</li>
 *   <li>특수문자 1개 이상 (@$!%*?&amp;#^()-_=+)</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Documented
@Constraint(validatedBy = PasswordValidator.PasswordConstraintValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordValidator {

    /**
     * 검증 실패 시 반환할 기본 메시지.
     *
     * @return 에러 메시지
     */
    String message() default "비밀번호는 8-128자이며, 대문자, 소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다.";

    /**
     * 검증 그룹.
     *
     * @return 검증 그룹 배열
     */
    Class<?>[] groups() default {};

    /**
     * 페이로드.
     *
     * @return 페이로드 배열
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 비밀번호 강도 검증 구현체.
     */
    class PasswordConstraintValidator implements ConstraintValidator<PasswordValidator, String> {

        // 최소 8자, 최대 128자, 대문자 1개, 소문자 1개, 숫자 1개, 특수문자 1개
        private static final Pattern PASSWORD_PATTERN = Pattern.compile(
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()\\-_=+])[A-Za-z\\d@$!%*?&#^()\\-_=+]{8,128}$"
        );

        @Override
        public void initialize(PasswordValidator constraintAnnotation) {
            // 초기화 불필요
        }

        @Override
        public boolean isValid(String password, ConstraintValidatorContext context) {
            if (password == null || password.isBlank()) {
                return false;
            }

            // 정규식에 이미 길이 제한(8-128자)이 포함되어 있어 중복 검증 제거
            return PASSWORD_PATTERN.matcher(password).matches();
        }
    }
}
