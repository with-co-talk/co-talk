package com.cotalk.infrastructure.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RateLimitProperties 테스트.
 *
 * @author seunggu.lee
 */
@DisplayName("RateLimitProperties")
class RateLimitPropertiesTest {

    private RateLimitProperties properties;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
    }

    @Nested
    @DisplayName("기본값 테스트")
    class DefaultValues {

        @Test
        @DisplayName("enabled 기본값은 true이다")
        void should_haveEnabledTrue_byDefault() {
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("endpoints 기본값은 빈 리스트이다")
        void should_haveEmptyEndpoints_byDefault() {
            assertThat(properties.getEndpoints()).isEmpty();
        }
    }

    @Nested
    @DisplayName("setter 테스트")
    class SetterTest {

        @Test
        @DisplayName("enabled 값을 설정할 수 있다")
        void should_setEnabled() {
            // when
            properties.setEnabled(false);

            // then
            assertThat(properties.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("endpoints 리스트를 설정할 수 있다")
        void should_setEndpoints() {
            // given
            RateLimitProperties.EndpointRateLimit endpoint = new RateLimitProperties.EndpointRateLimit();
            endpoint.setPath("/api/v1/test");
            endpoint.setRequestsPerMinute(100);

            // when
            properties.setEndpoints(List.of(endpoint));

            // then
            assertThat(properties.getEndpoints()).hasSize(1);
            assertThat(properties.getEndpoints().get(0).getPath()).isEqualTo("/api/v1/test");
        }
    }

    @Nested
    @DisplayName("EndpointRateLimit 테스트")
    class EndpointRateLimitTest {

        private RateLimitProperties.EndpointRateLimit endpoint;

        @BeforeEach
        void setUp() {
            endpoint = new RateLimitProperties.EndpointRateLimit();
        }

        @Test
        @DisplayName("기본값 확인")
        void should_haveCorrectDefaults() {
            assertThat(endpoint.getPath()).isNull();
            assertThat(endpoint.getRequestsPerHour()).isZero();
            assertThat(endpoint.getRequestsPerMinute()).isZero();
            assertThat(endpoint.getRequestsPerSecond()).isZero();
            assertThat(endpoint.isPerUser()).isFalse();
        }

        @Test
        @DisplayName("path를 설정할 수 있다")
        void should_setPath() {
            // when
            endpoint.setPath("/api/v1/messages");

            // then
            assertThat(endpoint.getPath()).isEqualTo("/api/v1/messages");
        }

        @Test
        @DisplayName("requestsPerHour를 설정할 수 있다")
        void should_setRequestsPerHour() {
            // when
            endpoint.setRequestsPerHour(1000);

            // then
            assertThat(endpoint.getRequestsPerHour()).isEqualTo(1000);
        }

        @Test
        @DisplayName("requestsPerMinute를 설정할 수 있다")
        void should_setRequestsPerMinute() {
            // when
            endpoint.setRequestsPerMinute(100);

            // then
            assertThat(endpoint.getRequestsPerMinute()).isEqualTo(100);
        }

        @Test
        @DisplayName("requestsPerSecond를 설정할 수 있다")
        void should_setRequestsPerSecond() {
            // when
            endpoint.setRequestsPerSecond(10);

            // then
            assertThat(endpoint.getRequestsPerSecond()).isEqualTo(10);
        }

        @Test
        @DisplayName("perUser를 설정할 수 있다")
        void should_setPerUser() {
            // when
            endpoint.setPerUser(true);

            // then
            assertThat(endpoint.isPerUser()).isTrue();
        }

        @Test
        @DisplayName("모든 필드를 설정할 수 있다")
        void should_setAllFields() {
            // when
            endpoint.setPath("/api/v1/chat");
            endpoint.setRequestsPerHour(3600);
            endpoint.setRequestsPerMinute(60);
            endpoint.setRequestsPerSecond(1);
            endpoint.setPerUser(true);

            // then
            assertThat(endpoint.getPath()).isEqualTo("/api/v1/chat");
            assertThat(endpoint.getRequestsPerHour()).isEqualTo(3600);
            assertThat(endpoint.getRequestsPerMinute()).isEqualTo(60);
            assertThat(endpoint.getRequestsPerSecond()).isEqualTo(1);
            assertThat(endpoint.isPerUser()).isTrue();
        }
    }
}
