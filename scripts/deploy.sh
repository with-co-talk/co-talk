#!/usr/bin/env bash

# ===========================================
# Co-Talk Blue-Green Deployment Script
# ===========================================
# Zero-downtime blue-green deployment for 1-instance NAS setup
# (Synology NAS Celeron J4125 4코어, 20GB RAM — CPU 경합 방지를 위해 단일 인스턴스 운영)
#
# Usage:
#   ./scripts/deploy.sh                                    # Local build deployment
#   ./scripts/deploy.sh --pull                             # Pull remote image (for NAS)
#   ./scripts/deploy.sh --rollback                         # Rollback to previous image
#   ./scripts/deploy.sh -f docker-compose.nas.yml --pull   # NAS compose file
#
# Deployment Flow:
#   1. Read current active instance from state file (default: app-1)
#   2. Tag current :latest as :previous (backup)
#   3. Pull/build new image
#   4. Start standby instance with --profile deploy
#   5. Health check standby
#   6. Switch upstream.conf to standby → nginx reload
#   7. Drain (3s) → stop old active
#   8. Update state file
#
# 3인스턴스 복구:
#   맥미니 이전 후 docker-compose.nas.yml에서 app-2 profiles 제거,
#   upstream.conf에 3개 서버 복원, 이 스크립트를 canary 버전으로 원복
#
# Requirements:
#   - docker compose v2
#   - curl (for health checks)
# ===========================================

set -euo pipefail

# ===========================================
# Configuration
# ===========================================
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${PROJECT_ROOT}/docker-compose.yml"

HEALTH_CHECK_TIMEOUT=120
HEALTH_CHECK_INTERVAL=5
HEALTH_CHECK_URL="http://localhost:8080/actuator/health"

# Blue-Green instances
BLUE_INSTANCE="app-1"
GREEN_INSTANCE="app-2"
STATE_FILE="${PROJECT_ROOT}/.deploy-active"
UPSTREAM_CONF="${PROJECT_ROOT}/docker/nginx/upstream.conf"

# Image tags for rollback support
IMAGE_TAG_LATEST="latest"
IMAGE_TAG_PREVIOUS="previous"

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

# Docker compose with deploy profile (for starting standby instance)
dc_deploy() {
    docker compose -f "$COMPOSE_FILE" --profile deploy "$@"
}

# ===========================================
# Utility Functions
# ===========================================

# Check if required commands exist
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

# Get the image name from compose config for app-1
# Returns empty string if no image is defined (local build mode)
get_image_name() {
    dc config --format json 2>/dev/null | \
        python3 -c "
import sys, json
try:
    cfg = json.load(sys.stdin)
    print(next(s['image'] for k, s in cfg['services'].items() if k == 'app-1' and 'image' in s))
except:
    print('')
" 2>/dev/null || echo ""
}

# Tag the current :latest image as :previous for rollback support
backup_current_image() {
    local image_name
    image_name=$(get_image_name)
    if [ -n "$image_name" ]; then
        local base_image="${image_name%:*}"
        log_info "Backing up current image as ${base_image}:${IMAGE_TAG_PREVIOUS}"
        docker tag "${base_image}:${IMAGE_TAG_LATEST}" "${base_image}:${IMAGE_TAG_PREVIOUS}" 2>/dev/null || \
            log_warn "No existing image to backup (first deployment?)"
    fi
}

# Health check for a specific container instance
# Waits up to HEALTH_CHECK_TIMEOUT seconds, checking every HEALTH_CHECK_INTERVAL seconds
health_check() {
    local container_name=$1
    local elapsed=0

    log_info "Starting health check for ${container_name}..."
    log_info "Waiting up to ${HEALTH_CHECK_TIMEOUT}s (checking every ${HEALTH_CHECK_INTERVAL}s)"

    while [ $elapsed -lt $HEALTH_CHECK_TIMEOUT ]; do
        # Check if container is running
        if ! dc_deploy ps "$container_name" | grep -q "Up"; then
            log_warn "Container ${container_name} is not running yet..."
            sleep $HEALTH_CHECK_INTERVAL
            elapsed=$((elapsed + HEALTH_CHECK_INTERVAL))
            continue
        fi

        # Check health endpoint inside container
        if dc_deploy exec -T "$container_name" curl -sf "$HEALTH_CHECK_URL" > /dev/null 2>&1; then
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
# Blue-Green State Management
# ===========================================

# Read current active instance from state file (default: app-1)
get_active_instance() {
    if [ -f "$STATE_FILE" ]; then
        cat "$STATE_FILE"
    else
        echo "$BLUE_INSTANCE"
    fi
}

# Get the standby (inactive) instance
get_standby_instance() {
    local active
    active=$(get_active_instance)
    if [ "$active" = "$BLUE_INSTANCE" ]; then
        echo "$GREEN_INSTANCE"
    else
        echo "$BLUE_INSTANCE"
    fi
}

# Switch nginx upstream to point to the target instance
switch_upstream() {
    local target=$1
    cat > "$UPSTREAM_CONF" << EOF
# Auto-generated by deploy.sh - do not edit manually
# Active instance: ${target}

upstream cotalk-backend {
    server ${target}:8080 max_fails=10 fail_timeout=10s;
    keepalive 16;
}
EOF
    dc exec -T nginx nginx -s reload
    log_success "Upstream switched to ${target}"
}

# ===========================================
# Infrastructure Management
# ===========================================

# Ensure all infrastructure and monitoring services are running and healthy
ensure_infrastructure() {
    # Critical data services (must be healthy before app startup)
    local critical_services=("postgres" "redis" "minio")
    # Monitoring and support services (start but don't block deployment)
    local monitoring_services=("zipkin" "loki" "prometheus" "alertmanager" "promtail" "grafana")

    # --- Start critical services ---
    for svc in "${critical_services[@]}"; do
        if ! dc ps "$svc" 2>/dev/null | grep -q "Up\|running"; then
            log_warn "${svc} is not running. Starting..."
            if ! dc up -d "$svc"; then
                log_error "Failed to start ${svc}"
                exit 1
            fi
            log_success "Started ${svc}"
        else
            log_info "${svc} is already running"
        fi
    done

    # Wait for postgres and redis to become healthy before proceeding
    log_info "Waiting for database and cache to be healthy..."
    local infra_wait=0
    local infra_timeout=60
    while [ $infra_wait -lt $infra_timeout ]; do
        local pg_healthy=false
        local redis_healthy=false

        if dc ps postgres 2>/dev/null | grep -q "healthy"; then
            pg_healthy=true
        fi
        if dc ps redis 2>/dev/null | grep -q "healthy"; then
            redis_healthy=true
        fi

        if [ "$pg_healthy" = true ] && [ "$redis_healthy" = true ]; then
            log_success "Critical infrastructure services are healthy"
            break
        fi

        sleep 3
        infra_wait=$((infra_wait + 3))
    done

    if [ $infra_wait -ge $infra_timeout ]; then
        log_error "Infrastructure services failed to become healthy within ${infra_timeout}s"
        exit 1
    fi

    # --- Start monitoring services (non-blocking) ---
    # Single command lets Docker Compose handle dependency ordering
    log_info "Ensuring monitoring services are running..."
    dc up -d "${monitoring_services[@]}" 2>/dev/null || \
        log_warn "Some monitoring services failed to start (non-critical, deployment continues)"
}

# ===========================================
# Rollback Function
# ===========================================

# Blue-green rollback: swap back to the previous active instance with :previous image
rollback() {
    log_warn "=== Blue-Green Rollback: Restoring previous version ==="

    # Ensure infrastructure is running before rollback
    ensure_infrastructure

    local current_active
    current_active=$(get_active_instance)
    local standby
    standby=$(get_standby_instance)

    log_info "Current active: ${current_active}"
    log_info "Will start: ${standby} with previous image"

    local image_name
    image_name=$(get_image_name)

    if [ -n "$image_name" ]; then
        local base_image="${image_name%:*}"

        if ! docker image inspect "${base_image}:${IMAGE_TAG_PREVIOUS}" &>/dev/null; then
            log_error "No previous image found (${base_image}:${IMAGE_TAG_PREVIOUS})"
            log_error "Cannot rollback - no backup available"
            exit 1
        fi

        log_info "Restoring ${base_image}:${IMAGE_TAG_PREVIOUS} as :${IMAGE_TAG_LATEST}"
        docker tag "${base_image}:${IMAGE_TAG_PREVIOUS}" "${base_image}:${IMAGE_TAG_LATEST}"
    fi

    # Start standby with previous image
    log_info "Starting ${standby} with previous image..."
    dc_deploy up -d --no-deps "$standby"

    if ! health_check "$standby"; then
        log_error "Rollback failed - ${standby} is unhealthy!"
        log_error "Manual intervention required"
        exit 1
    fi

    # Switch traffic
    switch_upstream "$standby"

    # Drain and stop old active
    log_info "Draining connections (3s)..."
    sleep 3
    dc stop -t 35 "$current_active" 2>/dev/null || true

    # Update state
    echo "$standby" > "$STATE_FILE"

    log_success "=== Rollback completed: ${standby} is now active ==="
}

# ===========================================
# Main Deployment Function
# ===========================================
deploy() {
    local use_pull=false

    # Parse deployment-specific arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --pull)
                use_pull=true
                shift
                ;;
            *)
                log_error "Unknown option: $1"
                echo "Usage: $0 [--pull] [-f compose-file] [--rollback]"
                exit 1
                ;;
        esac
    done

    local active
    active=$(get_active_instance)
    local standby
    standby=$(get_standby_instance)

    log_info "=== Co-Talk Blue-Green Deployment ==="
    log_info "Project root: ${PROJECT_ROOT}"
    log_info "Compose file: ${COMPOSE_FILE}"
    log_info "Current active: ${active}"
    log_info "Standby target: ${standby}"

    # Pre-flight checks
    check_dependencies
    ensure_infrastructure

    # -----------------------------------------------
    # Phase 1: Backup current image + pull/build new
    # -----------------------------------------------
    log_info "Phase 1: Preparing new image..."
    backup_current_image

    if [ "$use_pull" = true ]; then
        log_info "Pulling new image..."
        if ! dc pull "$active"; then
            log_error "Failed to pull image"
            exit 1
        fi
    else
        log_info "Building new image..."
        if ! dc build "$active"; then
            log_error "Failed to build image"
            exit 1
        fi
    fi
    log_success "New image ready"

    # -----------------------------------------------
    # Phase 2: Start standby instance
    # -----------------------------------------------
    log_info "Phase 2: Starting standby instance (${standby})..."
    dc_deploy stop -t 35 "$standby" 2>/dev/null || true
    dc_deploy up -d --no-deps "$standby"

    if ! health_check "$standby"; then
        log_error "Standby health check failed! Aborting deployment."
        dc_deploy stop -t 35 "$standby" 2>/dev/null || true
        log_info "Old active (${active}) remains running. No traffic impact."
        exit 1
    fi
    log_success "Standby ${standby} is healthy"

    # -----------------------------------------------
    # Phase 3: Switch traffic to standby
    # -----------------------------------------------
    log_info "Phase 3: Switching traffic to ${standby}..."
    switch_upstream "$standby"

    # -----------------------------------------------
    # Phase 4: Drain and stop old active
    # -----------------------------------------------
    log_info "Phase 4: Draining connections (3s)..."
    sleep 3
    log_info "Stopping old active (${active})..."
    dc stop -t 35 "$active" 2>/dev/null || true
    log_success "Old active ${active} stopped"

    # -----------------------------------------------
    # Phase 5: Update state + ensure nginx
    # -----------------------------------------------
    echo "$standby" > "$STATE_FILE"
    log_info "State file updated: active=${standby}"

    if ! dc ps nginx 2>/dev/null | grep -q "Up\|running"; then
        log_info "Starting nginx..."
        dc up -d --no-deps nginx
    else
        log_info "nginx is already running"
    fi

    # -----------------------------------------------
    # Done
    # -----------------------------------------------
    echo ""
    log_success "=== Blue-Green Deployment Completed Successfully ==="
    log_success "Active instance: ${standby}"
    echo ""
    log_info "To rollback, run: $0 --rollback"
}

# ===========================================
# Main Entry Point
# ===========================================
main() {
    cd "$PROJECT_ROOT"

    # Parse global options (compose file, rollback) before passing rest to deploy
    local args=()
    while [[ $# -gt 0 ]]; do
        case $1 in
            -f)
                COMPOSE_FILE="${PROJECT_ROOT}/$2"
                shift 2
                ;;
            --rollback)
                rollback
                exit 0
                ;;
            *)
                args+=("$1")
                shift
                ;;
        esac
    done

    # Run deployment with remaining arguments
    deploy "${args[@]+"${args[@]}"}"
}

# Run main function
main "$@"
