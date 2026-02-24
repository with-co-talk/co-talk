package com.cotalk.domain.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 강력한 비밀번호 정책 검증 어노테이션.
 *
 * <p>비밀번호 요구사항:</p>
 * <ul>
 *     <li>최소 8자 이상, 최대 128자 이하</li>
 *     <li>대문자 1개 이상</li>
 *     <li>소문자 1개 이상</li>
 *     <li>숫자 1개 이상</li>
 *     <li>특수문자 1개 이상 (@$!%*?&amp;#^()-_=+)</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    /**
     * 검증 실패 시 표시할 메시지.
     *
     * @return 기본 에러 메시지
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
}
