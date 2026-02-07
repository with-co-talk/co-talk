# ===========================================
# Stage 1: Build
# ===========================================
FROM gradle:8.5-jdk21-alpine AS builder

WORKDIR /app

# Gradle 캐시를 위한 의존성 파일 먼저 복사
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# 의존성 다운로드 (캐시 레이어)
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사
COPY src ./src

# 빌드 (테스트 스킵)
RUN gradle build -x test --no-daemon

# ===========================================
# Stage 2: Runtime
# ===========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 보안: 비루트 사용자 생성
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# 타임존 설정
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

# 헬스체크용 curl 설치
RUN apk add --no-cache curl

# JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 소유권 변경
RUN chown -R appuser:appgroup /app

# 비루트 사용자로 전환
USER appuser

# 환경 변수
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"

# 포트 노출
EXPOSE 8080

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
