#!/usr/bin/env bash
# chmod +x k6/run.sh 실행 필요
#
# k6 테스트 실행 래퍼 스크립트
# - 자동 결과 저장 (JSON + HTML Web Dashboard)
# - Prometheus 메트릭 전송 지원 (--prometheus 플래그)
# - 타임스탬프 기반 파일명
# - 환경변수 전달 지원
#
# 사용법:
#   ./k6/run.sh rest-api                                # 로컬, JSON + Web Dashboard
#   ./k6/run.sh websocket-chat --prometheus              # NAS, + Prometheus 전송
#   ./k6/run.sh rest-api --env BASE_URL=https://... --env K6_TOKEN=...

set -euo pipefail

# 색상 코드
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 기본값
SCENARIO="${1:-rest-api}"
PROMETHEUS_FLAG=false
K6_ARGS=()

# 인자 파싱
shift || true
while [[ $# -gt 0 ]]; do
  case $1 in
    --prometheus)
      PROMETHEUS_FLAG=true
      shift
      ;;
    *)
      K6_ARGS+=("$1")
      shift
      ;;
  esac
done

# 유효한 시나리오 목록
VALID_SCENARIOS=("rest-api" "websocket-chat" "full-flow" "spike" "stress" "breakpoint")

# 시나리오 검증
if [[ ! " ${VALID_SCENARIOS[@]} " =~ " ${SCENARIO} " ]]; then
  echo -e "${RED}Error: Invalid scenario '${SCENARIO}'${NC}"
  echo -e "${YELLOW}Valid scenarios: ${VALID_SCENARIOS[*]}${NC}"
  exit 1
fi

# 결과 디렉토리 생성
RESULTS_DIR="k6/results"
mkdir -p "$RESULTS_DIR"

# 타임스탬프 생성 (YYYYMMDD_HHmmss)
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# 결과 파일 경로
JSON_RESULT="${RESULTS_DIR}/${SCENARIO}_${TIMESTAMP}.json"
HTML_RESULT="${RESULTS_DIR}/${SCENARIO}_${TIMESTAMP}.html"

# k6 스크립트 경로
K6_SCRIPT="k6/scenarios/${SCENARIO}.js"

if [[ ! -f "$K6_SCRIPT" ]]; then
  echo -e "${RED}Error: k6 script not found: ${K6_SCRIPT}${NC}"
  exit 1
fi

# Web Dashboard 활성화
export K6_WEB_DASHBOARD=true
export K6_WEB_DASHBOARD_EXPORT="${HTML_RESULT}"

# k6 실행 명령어 구성 (플래그는 스크립트 경로 앞에 위치해야 함)
K6_FLAGS=(
  "--out" "json=${JSON_RESULT}"
)

# Prometheus 설정
if [[ "$PROMETHEUS_FLAG" == true ]]; then
  PROMETHEUS_URL="${PROMETHEUS_URL:-http://localhost:9090/api/v1/write}"
  export K6_PROMETHEUS_RW_SERVER_URL="$PROMETHEUS_URL"
  K6_FLAGS+=("--out" "experimental-prometheus-rw")
  echo -e "${BLUE}[INFO] Prometheus Remote Write enabled: ${PROMETHEUS_URL}${NC}"
fi

K6_CMD=("k6" "run" "${K6_FLAGS[@]}" "${K6_ARGS[@]}" "$K6_SCRIPT")

# 실행 정보 출력
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}k6 Test Runner${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "${BLUE}Scenario:${NC}        ${SCENARIO}"
echo -e "${BLUE}Timestamp:${NC}       ${TIMESTAMP}"
echo -e "${BLUE}JSON Output:${NC}     ${JSON_RESULT}"
echo -e "${BLUE}HTML Dashboard:${NC}  ${HTML_RESULT}"
if [[ "$PROMETHEUS_FLAG" == true ]]; then
  echo -e "${BLUE}Prometheus:${NC}      Enabled (${PROMETHEUS_URL})"
fi
echo -e "${GREEN}========================================${NC}"
echo ""

# k6 실행
echo -e "${YELLOW}[RUN] ${K6_CMD[*]}${NC}"
echo ""

"${K6_CMD[@]}"

# 결과 요약 출력
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Test Completed${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "${BLUE}Results saved to:${NC}"
echo -e "  JSON:  ${JSON_RESULT}"
echo -e "  HTML:  ${HTML_RESULT}"
echo ""
echo -e "${YELLOW}View HTML dashboard:${NC}"
echo -e "  open ${HTML_RESULT}"
echo -e "${GREEN}========================================${NC}"
