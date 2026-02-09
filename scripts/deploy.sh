#!/usr/bin/env bash

# ===========================================
# Co-Talk Blue-Green Deployment Script
# ===========================================
# Zero-downtime deployment using Blue-Green strategy
#
# Usage:
#   ./scripts/deploy.sh                          # Local build deployment
#   ./scripts/deploy.sh --pull                   # Pull remote image (for NAS)
#   ./scripts/deploy.sh --rollback               # Rollback to previous color
#   ./scripts/deploy.sh -f docker-compose.nas.yml --pull  # NAS compose file
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
UPSTREAM_CONF="${PROJECT_ROOT}/docker/nginx/upstream.conf"
UPSTREAM_BACKUP="${PROJECT_ROOT}/docker/nginx/upstream.conf.bak"

HEALTH_CHECK_TIMEOUT=120
HEALTH_CHECK_INTERVAL=5
HEALTH_CHECK_URL="http://localhost:8080/actuator/health"

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
# app-green uses profiles: [green], so --profile green is always needed
dc() {
    docker compose -f "$COMPOSE_FILE" --profile green "$@"
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

# Get current active color from upstream.conf
get_active_color() {
    if [ ! -f "$UPSTREAM_CONF" ]; then
        log_error "upstream.conf not found at: $UPSTREAM_CONF"
        exit 1
    fi

    if grep -q "server app-blue:8080" "$UPSTREAM_CONF"; then
        echo "blue"
    elif grep -q "server app-green:8080" "$UPSTREAM_CONF"; then
        echo "green"
    else
        log_error "Cannot determine active color from upstream.conf"
        exit 1
    fi
}

# Get target color (opposite of active)
get_target_color() {
    local active_color=$1
    if [ "$active_color" = "blue" ]; then
        echo "green"
    else
        echo "blue"
    fi
}

# Backup upstream.conf
backup_upstream_conf() {
    cp "$UPSTREAM_CONF" "$UPSTREAM_BACKUP"
    log_info "Backed up upstream.conf"
}

# Restore upstream.conf from backup
restore_upstream_conf() {
    if [ -f "$UPSTREAM_BACKUP" ]; then
        cp "$UPSTREAM_BACKUP" "$UPSTREAM_CONF"
        log_warn "Restored upstream.conf from backup"
    fi
}

# Update upstream.conf to point to new color
update_upstream_conf() {
    local color=$1

    cat > "$UPSTREAM_CONF" << EOF
# Active upstream for blue-green deployment
# This file is managed by deployment scripts
# Valid values: cotalk-blue or cotalk-green

upstream cotalk-backend {
    server app-${color}:8080 max_fails=3 fail_timeout=30s;
}
EOF

    log_info "Updated upstream.conf to point to app-${color}"
}

# Health check for container
health_check() {
    local container_name=$1
    local elapsed=0

    log_info "Starting health check for ${container_name}..."
    log_info "Waiting up to ${HEALTH_CHECK_TIMEOUT}s (checking every ${HEALTH_CHECK_INTERVAL}s)"

    while [ $elapsed -lt $HEALTH_CHECK_TIMEOUT ]; do
        # Check if container is running
        if ! dc ps "$container_name" | grep -q "Up"; then
            log_warn "Container ${container_name} is not running yet..."
            sleep $HEALTH_CHECK_INTERVAL
            elapsed=$((elapsed + HEALTH_CHECK_INTERVAL))
            continue
        fi

        # Check health endpoint inside container
        if dc exec -T "$container_name" curl -sf "$HEALTH_CHECK_URL" > /dev/null 2>&1; then
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

# Reload nginx configuration (start if not running)
reload_nginx() {
    if ! dc ps nginx 2>/dev/null | grep -q "Up\|running"; then
        log_info "Nginx is not running. Starting..."
        if dc up -d nginx; then
            log_success "Nginx started successfully"
            return 0
        else
            log_error "Failed to start nginx"
            return 1
        fi
    fi

    log_info "Reloading nginx configuration..."
    if dc exec -T nginx nginx -s reload; then
        log_success "Nginx reloaded successfully"
        return 0
    else
        log_error "Failed to reload nginx"
        return 1
    fi
}

# Stop old container gracefully
stop_old_container() {
    local container_name=$1
    log_info "Stopping old container: ${container_name}"

    if dc stop "$container_name"; then
        log_success "Stopped ${container_name} (graceful shutdown)"
    else
        log_warn "Failed to stop ${container_name}"
    fi
}

# Rollback deployment
rollback() {
    log_warn "Initiating rollback..."

    local current_color
    current_color=$(get_active_color)
    local rollback_color
    rollback_color=$(get_target_color "$current_color")

    log_info "Current active: ${current_color}"
    log_info "Rolling back to: ${rollback_color}"

    # Check if rollback target is running
    if ! dc ps "app-${rollback_color}" | grep -q "Up"; then
        log_error "Rollback target app-${rollback_color} is not running. Starting it..."
        dc up -d "app-${rollback_color}"

        if ! health_check "app-${rollback_color}"; then
            log_error "Rollback failed: app-${rollback_color} is unhealthy"
            exit 1
        fi
    fi

    # Backup and update upstream
    backup_upstream_conf
    update_upstream_conf "$rollback_color"

    # Reload nginx
    if ! reload_nginx; then
        log_error "Rollback failed: nginx reload failed"
        restore_upstream_conf
        reload_nginx
        exit 1
    fi

    log_success "Rollback completed successfully!"
    log_success "Active color: ${rollback_color}"
}

# Ensure backend infrastructure services (DB, cache, storage) are running
ensure_infrastructure() {
    local services=("postgres" "redis" "minio")

    for svc in "${services[@]}"; do
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

    # Wait for postgres and redis to be healthy before proceeding
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
            log_success "Infrastructure services are healthy"
            return 0
        fi

        sleep 3
        infra_wait=$((infra_wait + 3))
    done

    log_error "Infrastructure services failed to become healthy within ${infra_timeout}s"
    exit 1
}

# ===========================================
# Main Deployment Function
# ===========================================
deploy() {
    local use_pull=false

    # Parse arguments
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

    log_info "=== Co-Talk Blue-Green Deployment ==="
    log_info "Project root: ${PROJECT_ROOT}"
    log_info "Compose file: ${COMPOSE_FILE}"

    # Check dependencies
    check_dependencies

    # Ensure infrastructure services (nginx, postgres, redis, minio) are running
    ensure_infrastructure

    # Determine current and target colors
    local current_color
    current_color=$(get_active_color)
    local target_color
    target_color=$(get_target_color "$current_color")

    log_info "Current active color: ${COLOR_GREEN}${current_color}${COLOR_RESET}"
    log_info "Target deployment color: ${COLOR_GREEN}${target_color}${COLOR_RESET}"

    # Step 1: Build or pull new image
    if [ "$use_pull" = true ]; then
        log_info "Step 1/7: Pulling remote image for app-${target_color}..."
        if ! dc pull "app-${target_color}"; then
            log_error "Failed to pull image for app-${target_color}"
            exit 1
        fi
    else
        log_info "Step 1/7: Building new image for app-${target_color}..."
        if ! dc build "app-${target_color}"; then
            log_error "Failed to build image for app-${target_color}"
            exit 1
        fi
    fi
    log_success "Image ready for app-${target_color}"

    # Step 2: Start new container
    log_info "Step 2/7: Starting new container app-${target_color}..."
    if ! dc up -d "app-${target_color}"; then
        log_error "Failed to start app-${target_color}"
        exit 1
    fi
    log_success "Started app-${target_color}"

    # Step 3: Health check
    log_info "Step 3/7: Waiting for app-${target_color} to be healthy..."
    if ! health_check "app-${target_color}"; then
        log_error "Deployment failed: app-${target_color} is unhealthy"
        log_info "Cleaning up failed deployment..."
        dc stop "app-${target_color}"
        exit 1
    fi

    # Step 4: Backup upstream.conf
    log_info "Step 4/7: Backing up current configuration..."
    backup_upstream_conf

    # Step 5: Switch nginx to new color
    log_info "Step 5/7: Switching nginx to app-${target_color}..."
    update_upstream_conf "$target_color"

    if ! reload_nginx; then
        log_error "Failed to reload nginx. Rolling back..."
        restore_upstream_conf
        reload_nginx
        dc stop "app-${target_color}"
        exit 1
    fi

    log_success "Nginx now routing to app-${target_color}"

    # Step 6: Wait a bit to ensure traffic is flowing
    log_info "Step 6/7: Verifying traffic routing (waiting 5s)..."
    sleep 5

    # Step 7: Stop old container
    log_info "Step 7/7: Stopping old container app-${current_color}..."
    stop_old_container "app-${current_color}"

    # Cleanup backup
    rm -f "$UPSTREAM_BACKUP"

    # Success
    echo ""
    log_success "=== Deployment Completed Successfully ==="
    log_success "Active color: ${target_color}"
    log_success "Previous color: ${current_color} (stopped)"
    echo ""
    log_info "To rollback, run: $0 --rollback"
}

# ===========================================
# Main Entry Point
# ===========================================
main() {
    # Change to project root
    cd "$PROJECT_ROOT"

    # Parse global options first
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

    # Run deployment
    deploy "${args[@]+"${args[@]}"}"
}

# Run main function
main "$@"
