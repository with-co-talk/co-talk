package com.cotalk.infrastructure.oauth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * OAuth 제공자 API 호출용 {@link RestClient} 구성.
 *
 * <p>외부 제공자(카카오/구글/애플) 호출이 지연될 때 인증 스레드가 무한정 블로킹되지 않도록
 * 짧은 연결/읽기 타임아웃을 적용한다.</p>
 *
 * @author seunggu.lee
 */
@Configuration
public class OAuthHttpConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    /**
     * OAuth 어댑터가 공유하는 타임아웃이 적용된 RestClient 빈을 생성한다.
     *
     * @return 구성된 RestClient
     */
    @Bean("oauthRestClient")
    public RestClient oauthRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
