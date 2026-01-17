package com.cotalk.domain.port.outbound;

/**
 * 이메일 발송 아웃바운드 포트.
 * 이메일 발송을 위한 인터페이스를 정의한다.
 *
 * @author seunggu.lee
 */
public interface EmailSender {

    /**
     * 이메일을 발송한다.
     *
     * @param to      수신자 이메일 주소
     * @param subject 이메일 제목
     * @param body    이메일 본문 (HTML 지원)
     */
    void send(String to, String subject, String body);

    /**
     * 비밀번호 재설정 이메일을 발송한다.
     *
     * @param to        수신자 이메일 주소
     * @param resetLink 비밀번호 재설정 링크
     */
    void sendPasswordResetEmail(String to, String resetLink);
}
