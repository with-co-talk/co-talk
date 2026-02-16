#!/usr/bin/env bash

# ===========================================
# Co-Talk Canary Rolling Deployment Script
# ===========================================
# Zero-downtime canary deployment for 3-instance setup
#
# Usage:
#   ./scripts/deploy.sh                                    # Local build deployment
#   ./scripts/deploy.sh --pull                             # Pull remote image (for NAS)
#   ./scripts/deploy.sh --rollback                         # Rollback all to previous image
#   ./scripts/deploy.sh -f docker-compose.nas.yml --pull   # NAS compose file
#
# Deployment Flow:
#   1. Tag current image as :previous (backup)
#   2. Pull/build new image
#   3. Canary: update app-1 only → health check → wait 60s → verify metrics
#   4. If canary fails → rollback app-1 to :previous
#   5. If canary passes → roll out app-2, app-3 sequentially
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

CANARY_INSTANCE="app-1"
ALL_INSTANCES=("app-1" "app-2" "app-3")
REMAINING_INSTANCES=("app-2" "app-3")

CANARY_WAIT_SECONDS=30
ERROR_RATE_THRESHOLD=5  # percent

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
# All 3 instances (app-1, app-2, app-3) run without profiles
dc() {
    docker compose -f "$COMPOSE_FILE" "$@"
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

# Query Prometheus for canary error rate metrics
# Waits CANARY_WAIT_SECONDS before querying to allow metric accumulation
# Returns 0 (pass) if error rate is below threshold or metrics are unavailable
# Returns 1 (fail) if error rate exceeds ERROR_RATE_THRESHOLD
check_canary_metrics() {
    log_info "Waiting ${CANARY_WAIT_SECONDS}s for canary metrics collection..."
    sleep "$CANARY_WAIT_SECONDS"

    log_info "Querying Prometheus for canary error rate..."

    # Query: 5xx error rate for canary instance over last 1 minute
    local query='sum(rate(http_server_requests_seconds_count{instance=~"app-1.*",status=~"5.."}[1m])) / sum(rate(http_server_requests_seconds_count{instance=~"app-1.*"}[1m]))'
    local encoded_query
    encoded_query=$(python3 -c "import urllib.parse; print(urllib.parse.quote('${query}'))" 2>/dev/null || echo "")

    if [ -z "$encoded_query" ]; then
        log_warn "python3 not available for URL encoding, skipping metric check"
        log_info "Relying on health check only"
        return 0
    fi

    local result
    result=$(dc exec -T prometheus wget -qO- \
        "http://localhost:9090/api/v1/query?query=${encoded_query}" 2>/dev/null || echo "")

    if [ -z "$result" ]; then
        log_warn "Could not query Prometheus (may not be running). Skipping metric check."
        return 0
    fi

    # Parse the Prometheus response and extract the error rate value
    local value
    value=$(echo "$result" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    results = data.get('data', {}).get('result', [])
    if not results:
        print('NO_DATA')
    else:
        print(results[0]['value'][1])
except:
    print('PARSE_ERROR')
" 2>/dev/null || echo "PARSE_ERROR")

    case "$value" in
        NO_DATA)
            log_info "No request data for canary (zero traffic). Skipping error rate check."
            return 0
            ;;
        PARSE_ERROR)
            log_warn "Failed to parse Prometheus response. Skipping metric check."
            return 0
            ;;
        NaN)
            log_info "Error rate is NaN (likely zero requests). Skipping error rate check."
            return 0
            ;;
        *)
            # Convert to percentage and compare against threshold
            local error_pct
            error_pct=$(python3 -c "print(round(float('${value}') * 100, 2))" 2>/dev/null || echo "0")
            log_info "Canary error rate: ${error_pct}%"

            local threshold_exceeded
            threshold_exceeded=$(python3 -c "print('yes' if float('${error_pct}') > ${ERROR_RATE_THRESHOLD} else 'no')" 2>/dev/null || echo "no")

            if [ "$threshold_exceeded" = "yes" ]; then
                log_error "Canary error rate ${error_pct}% exceeds threshold ${ERROR_RATE_THRESHOLD}%!"
                return 1
            fi

            log_success "Canary error rate ${error_pct}% is within threshold"
            return 0
            ;;
    esac
}

# Rollback the canary instance to the previous image
# Called when canary deployment fails health check or metric verification
rollback_canary() {
    log_warn "Rolling back canary instance ${CANARY_INSTANCE}..."

    local image_name
    image_name=$(get_image_name)

    if [ -n "$image_name" ]; then
        local base_image="${image_name%:*}"
        log_info "Restoring ${base_image}:${IMAGE_TAG_PREVIOUS} as :${IMAGE_TAG_LATEST}"
        docker tag "${base_image}:${IMAGE_TAG_PREVIOUS}" "${base_image}:${IMAGE_TAG_LATEST}"
    fi

    dc up -d --no-deps "$CANARY_INSTANCE"

    if health_check "$CANARY_INSTANCE"; then
        log_success "Canary rollback completed"
    else
        log_error "Canary rollback FAILED - ${CANARY_INSTANCE} is unhealthy after rollback!"
        log_error "Manual intervention required"
    fi
}

# Full rollback: restore all instances to the :previous image
# Used with --rollback flag for emergency rollback of all instances
rollback() {
    log_warn "=== Full Rollback: Restoring all instances to previous image ==="

    # Ensure infrastructure is running before rollback
    ensure_infrastructure

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

    for instance in "${ALL_INSTANCES[@]}"; do
        log_info "Rolling back ${instance}..."
        dc up -d --no-deps "$instance"

        if ! health_check "$instance"; then
            log_error "Rollback failed for ${instance}!"
            exit 1
        fi
        log_success "${instance} rolled back successfully"
    done

    # Ensure nginx is running
    dc up -d --no-deps nginx 2>/dev/null || log_warn "Failed to start nginx"

    log_success "=== Full rollback completed ==="
}

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

    log_info "=== Co-Talk Canary Rolling Deployment ==="
    log_info "Project root: ${PROJECT_ROOT}"
    log_info "Compose file: ${COMPOSE_FILE}"

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
        if ! dc pull "${CANARY_INSTANCE}"; then
            log_error "Failed to pull image"
            exit 1
        fi
    else
        log_info "Building new image..."
        if ! dc build "${CANARY_INSTANCE}"; then
            log_error "Failed to build image"
            exit 1
        fi
    fi
    log_success "New image ready"

    # -----------------------------------------------
    # Phase 2: Canary deployment (app-1 only)
    # -----------------------------------------------
    log_info "Phase 2: Deploying canary (${CANARY_INSTANCE})..."
    dc up -d --no-deps "$CANARY_INSTANCE"

    if ! health_check "$CANARY_INSTANCE"; then
        log_error "Canary health check failed!"
        rollback_canary
        exit 1
    fi
    log_success "Canary ${CANARY_INSTANCE} is healthy"

    # -----------------------------------------------
    # Phase 3: Metric verification
    # -----------------------------------------------
    log_info "Phase 3: Verifying canary metrics..."
    if ! check_canary_metrics; then
        log_error "Canary metric verification failed!"
        rollback_canary
        exit 1
    fi
    log_success "Canary metrics verified"

    # -----------------------------------------------
    # Phase 4: Roll out remaining instances
    # -----------------------------------------------
    log_info "Phase 4: Rolling out to remaining instances..."
    for instance in "${REMAINING_INSTANCES[@]}"; do
        log_info "Updating ${instance}..."
        dc up -d --no-deps "$instance"

        if ! health_check "$instance"; then
            log_error "${instance} health check failed! Stopping rollout."
            log_error "Manual rollback may be needed: ./scripts/deploy.sh --rollback"
            exit 1
        fi
        log_success "${instance} is healthy"
    done

    # -----------------------------------------------
    # Phase 5: Ensure nginx is running
    # -----------------------------------------------
    log_info "Phase 5: Ensuring nginx is running..."
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
    log_success "=== Canary Deployment Completed Successfully ==="
    log_success "All instances updated: ${ALL_INSTANCES[*]}"
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
