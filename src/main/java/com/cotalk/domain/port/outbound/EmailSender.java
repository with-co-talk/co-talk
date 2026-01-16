package com.cotalk.domain.port.outbound;

/**
 * 이메일 발송 아웃바운드 포트
 */
public interface EmailSender {

    /**
     * 이메일 발송
     *
     * @param to 수신자 이메일
     * @param subject 제목
     * @param body 본문 (HTML 지원)
     */
    void send(String to, String subject, String body);

    /**
     * 비밀번호 재설정 이메일 발송
     *
     * @param to 수신자 이메일
     * @param resetLink 재설정 링크
     */
    void sendPasswordResetEmail(String to, String resetLink);
}
