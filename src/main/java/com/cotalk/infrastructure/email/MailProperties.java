package com.cotalk.infrastructure.email;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SMTP 메일 발송에 필요한 설정 프로퍼티.
 * {@code spring.mail} 프리픽스의 속성을 타입 안전하게 바인딩한다.
 *
 * <p>{@code @ConditionalOnProperty(name = "spring.mail.host")} 조건에 의해
 * SMTP가 설정된 환경에서만 활성화되므로 별도의 {@code @Validated} 검증은 사용하지 않는다.
 *
 * @author seunggu.lee
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.mail")
public class MailProperties {

    /**
     * SMTP 서버 호스트.
     */
    private String host;

    /**
     * SMTP 서버 포트.
     */
    private int port = 587;

    /**
     * SMTP 인증 사용자명 (발신자 이메일 주소로도 사용).
     */
    private String username;

    /**
     * SMTP 인증 비밀번호.
     */
    private String password;

    /**
     * 메일 From 주소. 비어 있으면 username을 사용한다.
     */
    private String fromAddress;

    /**
     * 메일 From 표시명.
     */
    private String fromName;
}
