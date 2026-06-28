plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    // OWASP Dependency-Check: 의존성 CVE 스캔 (OWASP A06). dependencyCheckAnalyze 태스크 제공.
    id("org.owasp.dependencycheck") version "12.1.0"
}

group = "com.cotalk"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

// Spring Boot는 spring-jcl을 사용. commons-logging.jar가 있으면 충돌하므로 제외
configurations.all {
    exclude(group = "commons-logging", module = "commons-logging")
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2") // 개발/테스트용

    // Database Migration
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // QueryDSL
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // API Documentation (Swagger / OpenAPI)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14")

    // Firebase Admin SDK (FCM)
    implementation("com.google.firebase:firebase-admin:9.2.0")

    // AWS S3 SDK (MinIO 호환)
    implementation(platform("software.amazon.awssdk:bom:2.25.16"))
    implementation("software.amazon.awssdk:s3")

    // HTML Parsing (URL Preview)
    implementation("org.jsoup:jsoup:1.17.2")

    // Rate Limiting (Bucket4j)
    implementation("com.bucket4j:bucket4j-redis:8.10.1")
    implementation("com.bucket4j:bucket4j-core:8.10.1")

    // Redisson (Distributed Lock)
    implementation("org.redisson:redisson-spring-boot-starter:3.51.0")

    // Observability - Micrometer & Tracing
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")

    // Logging - Loki (Logback appender)
    implementation("com.github.loki4j:loki-logback-appender:1.4.2")

    // Logging - JSON format (prod 프로필에서 사용)
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.1"))
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql") // 암호화 ON 검색 통합테스트 (prod 동등 Flyway 검증)
    testImplementation("org.awaitility:awaitility:4.2.0") // 비동기 테스트 대기
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // ArchUnit (Architecture Testing)
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// QueryDSL 설정
val querydslDir = layout.buildDirectory.dir("generated/querydsl")

sourceSets {
    main {
        java {
            srcDirs(querydslDir)
        }
    }
}

tasks.withType<JavaCompile> {
    options.generatedSourceOutputDirectory.set(querydslDir.get().asFile)
}

tasks.named("clean") {
    doLast {
        querydslDir.get().asFile.deleteRecursively()
    }
}

// JaCoCo 설정
jacoco {
    toolVersion = "0.8.14"
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/entity/Q*.class",  // QueryDSL Q클래스 제외
                    "**/config/**",
                    "**/CoTalkApplication.class"
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            element = "CLASS"

            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.60".toBigDecimal()
            }

            excludes = listOf(
                "*.entity.Q*",
                "*.config.*",
                "*.CoTalkApplication"
            )
        }
    }
}

// ----------------------------------------------------------------------------
// OWASP Dependency-Check (의존성 CVE 스캔, OWASP A06)
//
// - `./gradlew dependencyCheckAnalyze` 로 의존성 트리의 알려진 CVE를 스캔한다.
// - 기본 `test` 태스크와 분리되어 있어 일반 개발 빌드 속도에 영향을 주지 않는다.
// - 빌드 실패 게이트(failBuildOnCVSS)는 CI에서만 활성화한다:
//     로컬:  게이트 비활성(11.0) → NVD DB가 없어도 개발 빌드가 깨지지 않음
//     CI  :  `-PdependencyCheckCI=true` 전달 시 CVSS 7.0 이상에서 빌드 실패
// - NVD API 키는 환경변수 NVD_API_KEY 로 주입한다(없으면 느리지만 동작).
// ----------------------------------------------------------------------------
val dependencyCheckCI = providers.gradleProperty("dependencyCheckCI").orNull == "true"

dependencyCheck {
    // CI에서만 빌드 실패 게이트 적용. 로컬은 11.0(=사실상 비활성)으로 개발 빌드 보호.
    failBuildOnCVSS = if (dependencyCheckCI) 7.0f else 11.0f

    // 문서화된 오탐(false positive) 억제 파일
    suppressionFile = "owasp-suppressions.xml"

    // NVD API 키(있으면 빠름, 없으면 느리지만 동작)
    providers.environmentVariable("NVD_API_KEY").orNull?.let { nvd.apiKey = it }

    // 테스트 전용 의존성은 런타임 위협이 아니므로 스캔 범위에서 제외
    scanConfigurations = configurations
        .filter { it.name.startsWith("runtimeClasspath") || it.name.startsWith("compileClasspath") }
        .map { it.name }

    // 리포트 포맷(HTML + SARIF: CI/보안 도구 연동 용이)
    formats = listOf("HTML", "SARIF")

    // 오래된 분석기로 인한 오탐/잡음 감소
    analyzers.apply {
        assemblyEnabled = false
        nodeAuditEnabled = false
        nodeEnabled = false
    }
}
