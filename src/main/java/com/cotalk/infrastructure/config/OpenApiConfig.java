package com.cotalk.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import com.cotalk.infrastructure.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI(Swagger) 문서화 설정 클래스.
 * Co-Talk API의 Swagger UI 및 API 문서를 구성한다.
 *
 * <p>주요 설정:
 * <ul>
 *   <li>API 기본 정보 (제목, 설명, 버전)</li>
 *   <li>JWT 기반 Bearer 인증 스키마</li>
 *   <li>서버 정보 (로컬, 운영)</li>
 *   <li>API 그룹별 문서화</li>
 * </ul>
 *
 * @author seunggu.lee
 */
@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    private final AppProperties appProperties;

    /**
     * OpenAPI 기본 설정을 생성한다.
     * API 정보, 서버 목록, 태그, 보안 스키마 등을 정의한다.
     *
     * @return OpenAPI 설정 객체
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .servers(createServers())
                .tags(createTags())
                .externalDocs(createExternalDocs())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(createComponents());
    }

    private Info createApiInfo() {
        return new Info()
                .title("Co-Talk API")
                .description("""
                        ## Co-Talk 실시간 채팅 애플리케이션 API

                        ### 주요 기능
                        - **인증**: 회원가입, 로그인, 소셜 로그인 (Google, Kakao, Naver)
                        - **사용자**: 프로필 관리, 차단 기능
                        - **친구**: 친구 요청, 수락/거절, 친구 목록 관리
                        - **채팅방**: 1:1 채팅, 그룹 채팅, 채팅방 관리
                        - **메시지**: 텍스트/이미지/파일 전송, 검색, 수정/삭제
                        - **알림**: 알림 설정, 푸시 알림
                        - **신고**: 사용자/메시지 신고

                        ### 인증 방식
                        JWT 토큰 기반 인증을 사용합니다. 로그인 후 발급받은 토큰을 Authorization 헤더에 포함시켜 요청합니다.
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Co-Talk Team")
                        .email("support@cotalk.com")
                        .url("https://github.com/cotalk"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private List<Server> createServers() {
        return List.of(
                new Server()
                        .url(appProperties.swagger().serverUrl())
                        .description(appProperties.swagger().serverDescription())
        );
    }

    private List<Tag> createTags() {
        return List.of(
                new Tag().name("인증").description("회원가입, 로그인, 토큰 관리"),
                new Tag().name("사용자").description("사용자 프로필 및 계정 관리"),
                new Tag().name("친구").description("친구 관계 관리"),
                new Tag().name("채팅방").description("채팅방 생성 및 관리"),
                new Tag().name("채팅 메시지").description("메시지 전송 및 조회"),
                new Tag().name("메시지 검색").description("메시지 검색 기능"),
                new Tag().name("알림 설정").description("알림 관련 설정"),
                new Tag().name("신고").description("사용자 및 메시지 신고"),
                new Tag().name("차단").description("사용자 차단 관리")
        );
    }

    private ExternalDocumentation createExternalDocs() {
        return new ExternalDocumentation()
                .description("Co-Talk 위키")
                .url("https://github.com/cotalk/wiki");
    }

    private Components createComponents() {
        return new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, createSecurityScheme())
                .addSchemas("ErrorResponse", createErrorResponseSchema());
    }

    private SecurityScheme createSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT 토큰을 입력하세요. 'Bearer ' 접두사 없이 토큰만 입력합니다.");
    }

    @SuppressWarnings("rawtypes")
    private Schema createErrorResponseSchema() {
        return new Schema<>()
                .type("object")
                .addProperty("error", new Schema<>().type("string").description("에러 메시지"))
                .addProperty("code", new Schema<>().type("string").description("에러 코드"))
                .addProperty("timestamp", new Schema<>().type("string").format("date-time").description("발생 시간"));
    }

    // API 그룹 빈 정의 - 제네릭 메서드로 중복 제거

    @Bean
    public GroupedOpenApi authApi() {
        return createApiGroup("1. 인증", "/api/v1/auth/**");
    }

    @Bean
    public GroupedOpenApi userApi() {
        return createApiGroup("2. 사용자", "/api/v1/users/**");
    }

    @Bean
    public GroupedOpenApi friendApi() {
        return createApiGroup("3. 친구", "/api/v1/friends/**");
    }

    @Bean
    public GroupedOpenApi chatApi() {
        return createApiGroup("4. 채팅", "/api/v1/chat/**", "/api/v1/messages/**");
    }

    @Bean
    public GroupedOpenApi notificationApi() {
        return createApiGroup("5. 알림", "/api/v1/notifications/**");
    }

    @Bean
    public GroupedOpenApi reportApi() {
        return createApiGroup("6. 신고", "/api/v1/reports/**");
    }

    @Bean
    public GroupedOpenApi blockApi() {
        return createApiGroup("7. 차단", "/api/v1/blocks/**");
    }

    @Bean
    public GroupedOpenApi allApi() {
        return createApiGroup("전체 API", "/api/**");
    }

    /**
     * API 그룹을 생성한다.
     *
     * @param groupName 그룹 이름
     * @param patterns  매칭할 경로 패턴들
     * @return GroupedOpenApi 설정
     */
    private GroupedOpenApi createApiGroup(String groupName, String... patterns) {
        return GroupedOpenApi.builder()
                .group(groupName)
                .pathsToMatch(patterns)
                .build();
    }
}
