package com.cotalk.infrastructure.config.properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 모든 ConfigurationProperties를 활성화하는 중앙 설정 클래스.
 *
 * @author seunggu.lee
 */
@Configuration
@EnableConfigurationProperties({
        AppProperties.class,
        JwtProperties.class,
        MinioProperties.class,
        FirebaseProperties.class,
        SnowflakeProperties.class,
        FileUploadProperties.class
})
public class PropertiesConfig {
}
