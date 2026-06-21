package com.cotalk.infrastructure.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.DefaultHealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.SimpleStatusAggregator;
import org.springframework.boot.actuate.health.SimpleHttpCodeStatusMapper;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * <b>DB 헬스 인디케이터 → 액추에이터 헬스 엔드포인트 계약 테스트 (graceful degradation).</b>
 *
 * <p>{@link DatabaseHealthIndicatorTest}는 인디케이터가 DOWN을 반환하는지만 본다. 이 테스트는 한 걸음
 * 더 나아가, 데이터소스 쿼리가 실패할 때 그 DOWN이 <b>액추에이터 헬스 엔드포인트 계약</b>으로 어떻게
 * 표면화되는지를 검증한다:
 * <ul>
 *   <li>{@link HealthEndpoint}가 {@code db} 컴포넌트를 집계해 전체 상태를 DOWN으로 노출한다.</li>
 *   <li>기본 HTTP 코드 매핑상 DOWN → <b>HTTP 503</b>(Service Unavailable)으로 매핑된다.
 *       즉 미정의 500이 아니라 운영 LB/프로브가 인식하는 정의된 비가용 신호가 나간다.</li>
 * </ul>
 *
 * <p>전체 {@code @SpringBootTest} 컨텍스트(JPA/Flyway가 정상 DB를 요구)를 깨지 않고 결정적으로 검증하기
 * 위해, 실패하는 {@link JdbcTemplate}를 주입한 인디케이터를 실제 액추에이터 헬스 집계기에 직접
 * 등록해 엔드포인트가 만들어내는 것과 동일한 집계/HTTP 매핑을 재현한다.</p>
 *
 * @author seunggu.lee
 */
@DisplayName("DB 헬스 엔드포인트 계약 (실패 시 DOWN → 503)")
class DatabaseHealthEndpointContractTest {

    @Test
    @DisplayName("데이터소스 쿼리가 실패하면 헬스 엔드포인트가 DOWN을 노출하고 HTTP 503으로 매핑된다")
    void should_exposeDownAnd503_when_datasourceQueryFails() {
        // given: SELECT 1이 던지는 실패 데이터소스로 백킹된 실제 DatabaseHealthIndicator
        JdbcTemplate failingJdbc = mock(JdbcTemplate.class);
        given(failingJdbc.queryForObject("SELECT 1", Integer.class))
                .willThrow(new RuntimeException("Connection refused"));
        DatabaseHealthIndicator dbIndicator = new DatabaseHealthIndicator(failingJdbc);

        // 액추에이터가 엔드포인트 구성 시 쓰는 것과 동일한 집계기/HTTP 매퍼로 헬스 엔드포인트를 구성
        HealthContributorRegistry registry = new DefaultHealthContributorRegistry(
                Collections.emptyMap());
        registry.registerContributor("db", dbIndicator);

        StatusAggregator statusAggregator = new SimpleStatusAggregator();
        HttpCodeStatusMapper httpCodeStatusMapper = new SimpleHttpCodeStatusMapper();
        HealthEndpointGroups groups = HealthEndpointGroups.of(
                new TestGroup(statusAggregator, httpCodeStatusMapper), Collections.emptyMap());

        HealthEndpoint endpoint = new HealthEndpoint(registry, groups, Duration.ofSeconds(1));

        // when: 엔드포인트 전체 헬스 조회 (보안상 상세는 show-always인 테스트 그룹)
        HealthComponent aggregate = endpoint.health();

        // then: 집계 상태가 DOWN
        assertThat(aggregate.getStatus())
                .as("db 컴포넌트가 DOWN이면 전체 헬스도 DOWN이어야 함")
                .isEqualTo(Status.DOWN);

        // 그리고 DOWN은 HTTP 503으로 매핑된다 (LB/프로브가 인식하는 정의된 비가용 신호)
        assertThat(httpCodeStatusMapper.getStatusCode(Status.DOWN))
                .as("DOWN은 HTTP 503으로 매핑되어야 함")
                .isEqualTo(503);
    }

    /**
     * 테스트용 헬스 엔드포인트 그룹: 상세를 항상 노출하고 모든 컴포넌트를 포함한다.
     * 액추에이터 기본 집계기/HTTP 매퍼를 그대로 사용해 운영 엔드포인트와 동일한 계약을 재현한다.
     */
    private record TestGroup(StatusAggregator statusAggregator,
                             HttpCodeStatusMapper httpCodeStatusMapper) implements HealthEndpointGroup {

        @Override
        public boolean isMember(String name) {
            return true;
        }

        @Override
        public boolean showComponents(org.springframework.boot.actuate.endpoint.SecurityContext securityContext) {
            return true;
        }

        @Override
        public boolean showDetails(org.springframework.boot.actuate.endpoint.SecurityContext securityContext) {
            return true;
        }

        @Override
        public StatusAggregator getStatusAggregator() {
            return statusAggregator;
        }

        @Override
        public HttpCodeStatusMapper getHttpCodeStatusMapper() {
            return httpCodeStatusMapper;
        }

        @Override
        public org.springframework.boot.actuate.health.AdditionalHealthEndpointPath getAdditionalPath() {
            return null;
        }
    }
}
