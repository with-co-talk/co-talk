package com.cotalk.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OpenApiConfigTest {

    @Autowired
    private OpenAPI openAPI;

    @Test
    @DisplayName("OpenAPI 빈이 정상적으로 생성됨")
    void should_createOpenAPIBean() {
        // then
        assertThat(openAPI).isNotNull();
    }

    @Test
    @DisplayName("OpenAPI에 API 정보가 설정됨")
    void should_haveApiInfo_when_openAPICreated() {
        // then
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Co-Talk API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
    }

    @Test
    @DisplayName("OpenAPI에 JWT 보안 설정이 포함됨")
    void should_haveSecurityScheme_when_openAPICreated() {
        // then
        assertThat(openAPI.getComponents()).isNotNull();
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
    }
}
