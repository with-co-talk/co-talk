package com.cotalk.domain.port.outbound;

/**
 * 비밀번호 인코딩/검증 포트.
 * 애플리케이션 계층에서 비밀번호 해싱·검증만 필요할 때 사용한다.
 * BCrypt 등 구체 구현은 인프라 어댑터에서 주입한다.
 */
public interface PasswordEncoderPort {

    /**
     * 평문 비밀번호를 인코딩한다.
     *
     * @param rawPassword 평문 비밀번호
     * @return 인코딩된 비밀번호 문자열
     */
    String encode(String rawPassword);

    /**
     * 평문 비밀번호가 저장된 인코딩 값과 일치하는지 검증한다.
     *
     * @param rawPassword 평문 비밀번호
     * @param encodedPassword 저장된 인코딩 비밀번호
     * @return 일치하면 true
     */
    boolean matches(String rawPassword, String encodedPassword);
}
