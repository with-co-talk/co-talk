#!/usr/bin/env bash

# Ensure docker and other tools are in PATH (for GitHub Actions runner environment)
export PATH="/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH"

# ===========================================
# Co-Talk Canary Deployment Script
# ===========================================
# Canary deployment for Mac Mini setup
# (app-1, app-2 = stable / app-canary = canary)
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
#   2. Rolling update app-1: remove from upstream → stop → start → health check → restore
#   3. Rolling update app-2: same
#   4. Switch upstream to stable-only
#   5. Stop app-canary
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

HEALTH_CHECK_TIMEOUT=120
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
    docker compose -f "$COMPOSE_FILE" "$@"
}

dc_canary() {
    docker compose -f "$COMPOSE_FILE" --profile canary "$@"
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

# ===========================================
# Health Check
# ===========================================
health_check() {
    local container_name=$1
    local elapsed=0

    log_info "Starting health check for ${container_name}..."
    log_info "Waiting up to ${HEALTH_CHECK_TIMEOUT}s (checking every ${HEALTH_CHECK_INTERVAL}s)"

    while [ $elapsed -lt $HEALTH_CHECK_TIMEOUT ]; do
        if docker exec "$container_name" curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
            log_success "Health check passed for ${container_name}"
            return 0
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
    keepalive 16;
}
EOF
}

write_upstream_canary() {
    cat > "$UPSTREAM_CONF" << 'EOF'
# Managed by deploy.sh - do not edit manually
# Phase: canary (10% traffic to canary)

upstream cotalk-backend {
    server app-1:8080 weight=9 max_fails=3 fail_timeout=10s;
    server app-2:8080 weight=9 max_fails=3 fail_timeout=10s;
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
    keepalive 16;
}
EOF
    else
        cat > "$UPSTREAM_CONF" << 'EOF'
# Managed by deploy.sh - do not edit manually
# Phase: rolling update (app-2 draining)

upstream cotalk-backend {
    server app-1:8080 weight=5 max_fails=3 fail_timeout=10s;
    keepalive 16;
}
EOF
    fi
}

nginx_reload() {
    dc exec -T nginx nginx -s reload
    log_success "nginx reloaded"
}

# ===========================================
# Canary Deploy
# ===========================================
deploy_canary() {
    log_info "=== Co-Talk Canary Deployment ==="
    check_dependencies

    log_info "Building cotalk-app:canary image..."
    docker build -t cotalk-app:canary "${PROJECT_ROOT}"
    log_success "Image built: cotalk-app:canary"

    log_info "Stopping previous canary container if exists..."
    docker stop co-talk-app-canary 2>/dev/null || true
    docker rm co-talk-app-canary 2>/dev/null || true

    log_info "Starting app-canary..."
    dc_canary up -d app-canary

    if ! health_check "co-talk-app-canary"; then
        log_error "Canary health check failed. Aborting."
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

    if [ ! -f "$DEPLOY_PHASE_FILE" ] || [ "$(cat "$DEPLOY_PHASE_FILE")" != "canary" ]; then
        log_error "Current phase is not 'canary'. Run --canary first."
        exit 1
    fi

    log_info "Tagging cotalk-app:canary as cotalk-app:stable..."
    docker tag cotalk-app:canary cotalk-app:stable
    log_success "Image tagged: cotalk-app:stable"

    # Rolling update app-1
    log_info "Rolling update: app-1..."
    write_upstream_rolling_without "app-1"
    nginx_reload
    log_info "Draining app-1 connections (5s)..."
    sleep 5

    dc stop -t 35 app-1 2>/dev/null || true
    dc up -d --no-deps app-1

    if ! health_check "co-talk-app-1"; then
        log_error "app-1 health check failed after update. Restoring canary upstream."
        write_upstream_canary
        nginx_reload
        exit 1
    fi
    log_success "app-1 updated and healthy"

    # Rolling update app-2
    log_info "Rolling update: app-2..."
    write_upstream_rolling_without "app-2"
    nginx_reload
    log_info "Draining app-2 connections (5s)..."
    sleep 5

    dc stop -t 35 app-2 2>/dev/null || true
    dc up -d --no-deps app-2

    if ! health_check "co-talk-app-2"; then
        log_error "app-2 health check failed after update. Manual intervention required."
        exit 1
    fi
    log_success "app-2 updated and healthy"

    log_info "Switching upstream to stable-only..."
    write_upstream_stable
    nginx_reload

    log_info "Stopping app-canary..."
    docker stop co-talk-app-canary 2>/dev/null || true
    docker rm co-talk-app-canary 2>/dev/null || true

    echo "stable" > "$DEPLOY_PHASE_FILE"
    log_success "=== Promotion complete. Both stable instances running new image. ==="
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
    log_success "=== Rollback complete. Stable instances (app-1, app-2) handling all traffic. ==="
}

# ===========================================
# Main Entry Point
# ===========================================
main() {
    cd "$PROJECT_ROOT"

    if [ $# -eq 0 ]; then
        log_error "No action specified."
        echo "Usage: $0 --canary | --promote | --rollback"
        exit 1
    fi

    case $1 in
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
            echo "Usage: $0 --canary | --promote | --rollback"
            exit 1
            ;;
    esac
}

main "$@"
