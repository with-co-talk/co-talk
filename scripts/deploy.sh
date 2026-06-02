#!/usr/bin/env bash

# Ensure docker and other tools are in PATH (for GitHub Actions runner environment)
export PATH="/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH"

# ===========================================
# Co-Talk Canary Deployment Script
# ===========================================
# Canary deployment for Mac Mini setup
# (app-1, app-2, app-3 = stable / app-canary = canary)
#
# Usage:
#   ./scripts/deploy.sh --canary    # Deploy canary (10% traffic)
#   ./scripts/deploy.sh --promote   # Promote canary to stable (rolling update)
#   ./scripts/deploy.sh --rollback  # Rollback canary, restore stable-only upstream
#
# Deployment Flow (--canary):
#   1. Build cotalk-app:canary image
#   2. Start app-canary container
#   3. Health check app-canary
#   4. Switch upstream to stable(90%) + canary(10%)
#   5. nginx reload
#
# Deployment Flow (--promote):
#   1. Tag canary image as stable
#   2. Rolling update app-1/app-2/app-3 one by one
#   3. Switch upstream to stable-only
#   4. Stop app-canary
#
# Deployment Flow (--rollback):
#   1. Switch upstream to stable-only
#   2. nginx reload
#   3. Stop app-canary
# ===========================================

set -euo pipefail

# ===========================================
# Configuration
# ===========================================
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${PROJECT_ROOT}/docker-compose.yml"
UPSTREAM_CONF="${PROJECT_ROOT}/docker/nginx/upstream.conf"
DEPLOY_PHASE_FILE="${PROJECT_ROOT}/.deploy-phase"
ENV_FILE="${COTALK_ENV_FILE:-${PROJECT_ROOT}/.env}"
COMPOSE_ENV_ARGS=()

if [ -f "$ENV_FILE" ]; then
    COMPOSE_ENV_ARGS=(--env-file "$ENV_FILE")
fi

HEALTH_CHECK_TIMEOUT=180
HEALTH_CHECK_INTERVAL=5

# ===========================================
# Color Output
# ===========================================
COLOR_GREEN='\033[0;32m'
COLOR_YELLOW='\033[1;33m'
COLOR_RED='\033[0;31m'
COLOR_BLUE='\033[0;34m'
COLOR_RESET='\033[0m'

log_info() {
    echo -e "${COLOR_BLUE}[INFO]${COLOR_RESET} $*"
}

log_success() {
    echo -e "${COLOR_GREEN}[SUCCESS]${COLOR_RESET} $*"
}

log_warn() {
    echo -e "${COLOR_YELLOW}[WARN]${COLOR_RESET} $*"
}

log_error() {
    echo -e "${COLOR_RED}[ERROR]${COLOR_RESET} $*"
}

# ===========================================
# Docker Compose Helper
# ===========================================
dc() {
    docker compose "${COMPOSE_ENV_ARGS[@]}" -f "$COMPOSE_FILE" "$@"
}

dc_canary() {
    docker compose "${COMPOSE_ENV_ARGS[@]}" -f "$COMPOSE_FILE" --profile canary "$@"
}

# ===========================================
# Dependency Check
# ===========================================
check_dependencies() {
    local missing_deps=()

    if ! command -v docker &> /dev/null; then
        missing_deps+=("docker")
    fi

    if ! docker compose version &> /dev/null; then
        missing_deps+=("docker-compose-v2")
    fi

    if ! command -v curl &> /dev/null; then
        missing_deps+=("curl")
    fi

    if [ ${#missing_deps[@]} -ne 0 ]; then
        log_error "Missing required dependencies: ${missing_deps[*]}"
        exit 1
    fi
}

env_has_value() {
    local key=$1

    if [ -n "${!key:-}" ]; then
        return 0
    fi

    if [ -f "$ENV_FILE" ]; then
        local value
        value=$(grep -E "^${key}=" "$ENV_FILE" | tail -n 1 | cut -d '=' -f 2- || true)
        if [ -n "$value" ]; then
            return 0
        fi
    fi

    return 1
}

check_runtime_env() {
    local missing_vars=()
    local required_vars=(
        DB_PASSWORD
        REDIS_PASSWORD
        MINIO_ACCESS_KEY
        MINIO_SECRET_KEY
        JWT_SECRET
        ENCRYPTION_KEY
    )

    for key in "${required_vars[@]}"; do
        if ! env_has_value "$key"; then
            missing_vars+=("$key")
        fi
    done

    if [ ${#missing_vars[@]} -ne 0 ]; then
        log_error "Missing required runtime env vars: ${missing_vars[*]}"
        log_error "Set them in ${ENV_FILE} or pass COTALK_ENV_FILE=/path/to/.env."
        exit 1
    fi

    if ! env_has_value "MAIL_HOST"; then
        log_warn "MAIL_HOST is not set. Email will fall back to console logging, not SMTP delivery."
    fi
}

# ===========================================
# Health Check
# ===========================================
health_check() {
    local container_name=$1
    local elapsed=0

    log_info "Starting health check for ${container_name}..."
    log_info "Waiting up to ${HEALTH_CHECK_TIMEOUT}s (checking every ${HEALTH_CHECK_INTERVAL}s)"

    while [ $elapsed -lt $HEALTH_CHECK_TIMEOUT ]; do
        local state health_status
        state=$(docker inspect --format='{{.State.Status}}' "$container_name" 2>/dev/null || echo "missing")

        # 컨테이너가 종료된 경우 즉시 실패
        if [ "$state" = "exited" ] || [ "$state" = "missing" ]; then
            echo ""
            log_error "Container ${container_name} has exited (status: ${state})"
            return 1
        fi

        # Docker 내장 HEALTHCHECK 상태 확인 (Dockerfile에 정의된 헬스체크 사용)
        health_status=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "none")
        if [ "$health_status" = "healthy" ]; then
            log_success "Health check passed for ${container_name} (status: healthy)"
            return 0
        fi
        if [ "$health_status" = "unhealthy" ]; then
            echo ""
            log_error "Container ${container_name} is unhealthy"
            return 1
        fi

        echo -n "."
        sleep $HEALTH_CHECK_INTERVAL
        elapsed=$((elapsed + HEALTH_CHECK_INTERVAL))
    done

    echo ""
    log_error "Health check failed for ${container_name} after ${HEALTH_CHECK_TIMEOUT}s"
    return 1
}

# ===========================================
# Upstream Config Writers
# ===========================================
write_upstream_stable() {
    cat > "$UPSTREAM_CONF" << 'EOF'
# Managed by deploy.sh - do not edit manually
# Phase: stable (canary inactive)

upstream cotalk-backend {
    server app-1:8080 weight=5 max_fails=3 fail_timeout=10s;
    server app-2:8080 weight=5 max_fails=3 fail_timeout=10s;
    server app-3:8080 weight=5 max_fails=3 fail_timeout=10s;
    keepalive 16;
}
EOF
}

write_upstream_canary() {
    cat > "$UPSTREAM_CONF" << 'EOF'
# Managed by deploy.sh - do not edit manually
# Phase: canary (10% traffic to canary)

upstream cotalk-backend {
    server app-1:8080 weight=6 max_fails=3 fail_timeout=10s;
    server app-2:8080 weight=6 max_fails=3 fail_timeout=10s;
    server app-3:8080 weight=6 max_fails=3 fail_timeout=10s;
    server app-canary:8080 weight=2 max_fails=3 fail_timeout=10s;
    keepalive 16;
}
EOF
}

write_upstream_rolling_without() {
    local excluded=$1
    if [ "$excluded" = "app-1" ]; then
        cat > "$UPSTREAM_CONF" << 'EOF'
# Managed by deploy.sh - do not edit manually
# Phase: rolling update (app-1 draining)

upstream cotalk-backend {
    server app-2:8080 weight=5 max_fails=3 fail_timeout=10s;
    server app-3:8080 weight=5 max_fails=3 fail_timeout=10s;
    keepalive 16;
}
EOF
    elif [ "$excluded" = "app-2" ]; then
        cat > "$UPSTREAM_CONF" << 'EOF'
# Managed by deploy.sh - do not edit manually
# Phase: rolling update (app-2 draining)

upstream cotalk-backend {
    server app-1:8080 weight=5 max_fails=3 fail_timeout=10s;
    server app-3:8080 weight=5 max_fails=3 fail_timeout=10s;
    keepalive 16;
}
EOF
    else
        cat > "$UPSTREAM_CONF" << 'EOF'
# Managed by deploy.sh - do not edit manually
# Phase: rolling update (app-3 draining)

upstream cotalk-backend {
    server app-1:8080 weight=5 max_fails=3 fail_timeout=10s;
    server app-2:8080 weight=5 max_fails=3 fail_timeout=10s;
    keepalive 16;
}
EOF
    fi
}

nginx_reload() {
    if dc ps nginx 2>/dev/null | grep -q "Up\|running"; then
        # Recreate instead of in-place reload because Docker Desktop file mounts
        # can keep the old inode when Actions checkout rewrites upstream.conf.
        dc up -d --force-recreate --no-deps nginx
        log_success "nginx recreated with updated config"
    else
        log_info "nginx not running, starting..."
        dc up -d --no-deps nginx
        log_success "nginx started"
    fi
}

# ===========================================
# Initial Bootstrap (first-time deploy)
# ===========================================
bootstrap() {
    log_info "=== Co-Talk Bootstrap (First-Time Deploy) ==="
    check_dependencies
    check_runtime_env

    # Already bootstrapped?
    if docker image inspect cotalk-app:stable &>/dev/null; then
        log_info "cotalk-app:stable already exists. Skipping bootstrap."
        log_info "Use --canary to deploy a new version."
        return 0
    fi

    log_info "Building cotalk-app:stable image..."
    # headless 러너(서비스 세션)에서 Docker 자격증명 헬퍼(keychain) 접근 실패 방지:
    # 공개 베이스 이미지는 인증이 불필요하므로 빈 DOCKER_CONFIG로 헬퍼 호출을 우회한다.
    DOCKER_CONFIG="$(mktemp -d)"; printf '{}' > "${DOCKER_CONFIG}/config.json"; export DOCKER_CONFIG
    docker buildx create --name ci-builder --driver docker-container --use 2>/dev/null || docker buildx use ci-builder
    docker buildx build --load -t cotalk-app:stable "${PROJECT_ROOT}"
    log_success "Image built: cotalk-app:stable"

    log_info "Starting infrastructure services (postgres, redis, minio)..."
    dc up -d postgres redis minio
    log_info "Waiting for infra to be ready (20s)..."
    sleep 20

    log_info "Starting stable app instances..."
    dc up -d --no-deps app-1 app-2 app-3

    for app in 1 2 3; do
        if ! health_check "co-talk-app-${app}"; then
            log_error "app-${app} health check failed."
            docker logs "co-talk-app-${app}" 2>&1 | tail -100 || true
            exit 1
        fi
        log_success "app-${app} healthy"
    done

    write_upstream_stable
    nginx_reload

    echo "stable" > "$DEPLOY_PHASE_FILE"
    log_success "=== Bootstrap complete. app-1, app-2, and app-3 are running. ==="
}

# ===========================================
# Canary Deploy
# ===========================================
deploy_canary() {
    log_info "=== Co-Talk Canary Deployment ==="
    check_dependencies
    check_runtime_env

    log_info "Building cotalk-app:canary image..."
    # headless 러너(서비스 세션)에서 Docker 자격증명 헬퍼(keychain) 접근 실패 방지:
    # 공개 베이스 이미지는 인증이 불필요하므로 빈 DOCKER_CONFIG로 헬퍼 호출을 우회하고
    # docker-container 드라이버 빌더로 빌드한다.
    DOCKER_CONFIG="$(mktemp -d)"; printf '{}' > "${DOCKER_CONFIG}/config.json"; export DOCKER_CONFIG
    docker buildx create --name ci-builder --driver docker-container --use 2>/dev/null || docker buildx use ci-builder
    docker buildx build --load -t cotalk-app:canary "${PROJECT_ROOT}"
    log_success "Image built: cotalk-app:canary"

    log_info "Stopping previous canary container if exists..."
    docker stop co-talk-app-canary 2>/dev/null || true
    docker rm co-talk-app-canary 2>/dev/null || true

    log_info "Starting app-canary..."
    dc_canary up -d app-canary

    if ! health_check "co-talk-app-canary"; then
        log_error "Canary health check failed. Aborting."
        log_info "=== co-talk-app-canary logs ==="
        docker logs co-talk-app-canary 2>&1 | tail -100 || true
        log_info "=== end of logs ==="
        docker stop co-talk-app-canary 2>/dev/null || true
        docker rm co-talk-app-canary 2>/dev/null || true
        exit 1
    fi

    log_info "Switching upstream to canary mode (stable 90% / canary 10%)..."
    write_upstream_canary
    nginx_reload

    echo "canary" > "$DEPLOY_PHASE_FILE"
    log_success "=== Canary deployment complete. Monitor metrics before promoting. ==="
    log_info "To promote: ./scripts/deploy.sh --promote"
    log_info "To rollback: ./scripts/deploy.sh --rollback"
}

# ===========================================
# Promote Canary to Stable
# ===========================================
promote_canary() {
    log_info "=== Promoting Canary to Stable ==="
    check_dependencies
    check_runtime_env

    if [ ! -f "$DEPLOY_PHASE_FILE" ] || [ "$(cat "$DEPLOY_PHASE_FILE")" != "canary" ]; then
        log_error "Current phase is not 'canary'. Run --canary first."
        exit 1
    fi

    log_info "Tagging cotalk-app:canary as cotalk-app:stable..."
    docker tag cotalk-app:canary cotalk-app:stable
    log_success "Image tagged: cotalk-app:stable"

    for app in app-1 app-2 app-3; do
        local container_name="co-talk-${app}"

        log_info "Rolling update: ${app}..."
        write_upstream_rolling_without "$app"
        nginx_reload
        log_info "Draining ${app} connections (5s)..."
        sleep 5

        dc stop -t 35 "$app" 2>/dev/null || true
        dc up -d --no-deps "$app"

        if ! health_check "$container_name"; then
            log_error "${app} health check failed after update. Restoring canary upstream."
            write_upstream_canary
            nginx_reload
            exit 1
        fi
        log_success "${app} updated and healthy"
    done

    log_info "Switching upstream to stable-only..."
    write_upstream_stable
    nginx_reload

    log_info "Stopping app-canary..."
    docker stop co-talk-app-canary 2>/dev/null || true
    docker rm co-talk-app-canary 2>/dev/null || true

    echo "stable" > "$DEPLOY_PHASE_FILE"
    log_success "=== Promotion complete. All stable instances running new image. ==="
}

# ===========================================
# Rollback
# ===========================================
rollback() {
    log_warn "=== Rolling Back: Restoring stable-only upstream ==="

    log_info "Switching upstream to stable-only..."
    write_upstream_stable
    nginx_reload

    log_info "Stopping app-canary..."
    docker stop co-talk-app-canary 2>/dev/null || true
    docker rm co-talk-app-canary 2>/dev/null || true

    echo "stable" > "$DEPLOY_PHASE_FILE"
    log_success "=== Rollback complete. Stable instances (app-1, app-2, app-3) handling all traffic. ==="
}

# ===========================================
# Main Entry Point
# ===========================================
main() {
    cd "$PROJECT_ROOT"

    if [ $# -eq 0 ]; then
        log_error "No action specified."
        echo "Usage: $0 --init | --canary | --promote | --rollback"
        exit 1
    fi

    case $1 in
        --init)
            bootstrap
            ;;
        --canary)
            deploy_canary
            ;;
        --promote)
            promote_canary
            ;;
        --rollback)
            rollback
            ;;
        *)
            log_error "Unknown option: $1"
            echo "Usage: $0 --init | --canary | --promote | --rollback"
            exit 1
            ;;
    esac
}

main "$@"
