package com.cotalk.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정.
 * 엔티티의 생성 시간과 수정 시간을 자동으로 관리한다.
 *
 * @author seunggu.lee
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
