#!/usr/bin/env bash
# ===========================================
# Co-Talk k6 테스트 사용자 시딩 스크립트
# ===========================================
# Rate Limit(회원가입 3/min, 로그인 5/min)을 준수하며
# 테스트 사용자를 미리 생성하고 토큰을 저장합니다.
#
# 사용법:
#   ./k6/seed.sh                                          # 로컬, 20명
#   ./k6/seed.sh https://co-talk.example.com 50           # 원격, 50명
#   ./k6/seed.sh https://co-talk.example.com 20 perftest  # 커스텀 prefix
#   NAS_SSH=user@nas-host ./k6/seed.sh https://co-talk.example.com 20
#
# 결과: k6/data/users.json 에 JWT 토큰 저장
# ===========================================

set -euo pipefail

SKIP_SIGNUP=false
while [[ "$1" == --* ]]; do
    case "$1" in
        --skip-signup) SKIP_SIGNUP=true; shift ;;
        *) shift ;;
    esac
done

BASE_URL="${1:-http://localhost:8080}"
USER_COUNT="${2:-20}"
EMAIL_PREFIX="${3:-loadtest}"
DOMAIN="test.cotalk.com"
PASSWORD="Test1234!@"
OUTPUT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/data"
OUTPUT_FILE="${OUTPUT_DIR}/users.json"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
RESET='\033[0m'

log_info()    { echo -e "${BLUE}[INFO]${RESET} $*"; }
log_success() { echo -e "${GREEN}[OK]${RESET} $*"; }
log_warn()    { echo -e "${YELLOW}[WARN]${RESET} $*"; }
log_error()   { echo -e "${RED}[ERROR]${RESET} $*"; }

# ===========================================
# 서버 상태 확인
# ===========================================
log_info "서버 연결 확인: ${BASE_URL}"
if ! curl -sf "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
    log_error "서버에 연결할 수 없습니다: ${BASE_URL}"
    log_error "서버가 실행 중인지 확인해주세요."
    exit 1
fi
log_success "서버 연결 확인됨"

mkdir -p "$OUTPUT_DIR"

signup_ok=0
signup_skip=0
signup_fail=0

if [ "$SKIP_SIGNUP" = true ]; then
    log_info "=== Phase 1: 회원가입 건너뜀 (--skip-signup) ==="
else
    # ===========================================
    # Phase 1: 회원가입 (nginx 5r/m Rate Limit 준수)
    # ===========================================
    log_info "=== Phase 1: 회원가입 (${USER_COUNT}명) ==="
    log_info "Rate Limit: nginx 5r/m → 매 요청마다 13초 대기"

    for i in $(seq 1 "$USER_COUNT"); do
        email="${EMAIL_PREFIX}+${i}@${DOMAIN}"

        status=$(curl -s -o /dev/null -w "%{http_code}" \
            -X POST "${BASE_URL}/api/v1/auth/signup" \
            -H "Content-Type: application/json" \
            -d "{\"email\":\"${email}\",\"password\":\"${PASSWORD}\",\"nickname\":\"${EMAIL_PREFIX}-user-${i}\"}")

        case "$status" in
            201) signup_ok=$((signup_ok + 1)); echo -ne "${GREEN}.${RESET}" ;;
            409) signup_skip=$((signup_skip + 1)); echo -ne "${YELLOW}s${RESET}" ;;
            429)
                echo -ne "${RED}!${RESET}"
                log_warn " Rate Limit 도달 (${i}번째). 60초 대기..."
                sleep 60
                status=$(curl -s -o /dev/null -w "%{http_code}" \
                    -X POST "${BASE_URL}/api/v1/auth/signup" \
                    -H "Content-Type: application/json" \
                    -d "{\"email\":\"${email}\",\"password\":\"${PASSWORD}\",\"nickname\":\"${EMAIL_PREFIX}-user-${i}\"}")
                if [ "$status" = "201" ]; then
                    signup_ok=$((signup_ok + 1))
                    echo -ne "${GREEN}r${RESET}"
                elif [ "$status" = "409" ]; then
                    signup_skip=$((signup_skip + 1))
                    echo -ne "${YELLOW}r${RESET}"
                else
                    signup_fail=$((signup_fail + 1))
                fi
                ;;
            *)
                signup_fail=$((signup_fail + 1))
                echo -ne "${RED}x${RESET}"
                ;;
        esac

        # nginx rate limit 준수: 매 요청 13초 대기 (5r/m = 12초에 1개)
        if (( i < USER_COUNT )); then
            sleep 13
        fi
    done

    echo ""
    log_info "회원가입 결과: 생성=${signup_ok}, 이미존재=${signup_skip}, 실패=${signup_fail}"
fi

# ===========================================
# Phase 1.5: 이메일 인증 활성화
# ===========================================
log_info "=== Phase 1.5: 이메일 인증 활성화 ==="

if [ -n "${NAS_SSH:-}" ]; then
    log_info "SSH를 통해 자동으로 이메일 인증 처리 중..."
    update_result=$(ssh "$NAS_SSH" "docker exec cotalk-postgres psql -U cotalk -d cotalk -c \"UPDATE users SET email_verified = true WHERE email LIKE '${EMAIL_PREFIX}%@${DOMAIN}'\"" 2>&1)
    rows_updated=$(echo "$update_result" | sed -n 's/.*UPDATE \([0-9]*\).*/\1/p')
    log_success "이메일 인증 완료: ${rows_updated}명"
else
    log_warn "NAS_SSH 환경변수가 설정되지 않았습니다."
    log_info "NAS에서 수동으로 다음 명령을 실행하세요:"
    echo ""
    echo "  docker exec cotalk-postgres psql -U cotalk -d cotalk -c \"UPDATE users SET email_verified = true WHERE email LIKE '${EMAIL_PREFIX}%@${DOMAIN}'\""
    echo ""
    read -p "명령 실행 후 Enter를 누르세요..." -r
    log_success "수동 이메일 인증 완료"
fi

# ===========================================
# Phase 2: 로그인 + 프로필 조회 (5/min Rate Limit 준수)
# ===========================================
log_info "=== Phase 2: 로그인 + 프로필 조회 (${USER_COUNT}명) ==="
log_info "Rate Limit: nginx 5r/m → 매 요청마다 13초 대기"

# JSON 배열 시작
echo "[" > "$OUTPUT_FILE"
login_ok=0
login_fail=0
first=true

for i in $(seq 1 "$USER_COUNT"); do
    email="${EMAIL_PREFIX}+${i}@${DOMAIN}"

    # Step 1: 로그인 (accessToken 획득)
    login_response=$(curl -s \
        -X POST "${BASE_URL}/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"${email}\",\"password\":\"${PASSWORD}\"}")

    access_token=$(echo "$login_response" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('accessToken', ''))
except:
    print('')
" 2>/dev/null || echo "")

    if [ -z "$access_token" ] || [ "$access_token" = "" ]; then
        # 로그인 실패 - 원인 출력
        login_fail=$((login_fail + 1))
        error_code=$(echo "$login_response" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('code', 'UNKNOWN'))
except:
    print('PARSE_ERROR')
" 2>/dev/null || echo "UNKNOWN")
        echo -ne "${RED}x(${error_code})${RESET}"
        # 첫 번째 실패 시 raw 응답 출력
        if [ "$login_fail" -eq 1 ]; then
            echo ""
            log_warn "첫 실패 응답 (user ${i}): ${login_response:0:300}"
        fi

        # Rate Limit이면 대기 후 재시도
        if echo "$login_response" | grep -qi "rate\|limit\|429\|too many"; then
            log_warn " Rate Limit 도달. 60초 대기 후 재시도..."
            sleep 60
            login_response=$(curl -s \
                -X POST "${BASE_URL}/api/v1/auth/login" \
                -H "Content-Type: application/json" \
                -d "{\"email\":\"${email}\",\"password\":\"${PASSWORD}\"}")
            access_token=$(echo "$login_response" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('accessToken', ''))
except:
    print('')
" 2>/dev/null || echo "")
            if [ -n "$access_token" ] && [ "$access_token" != "" ]; then
                login_fail=$((login_fail - 1))
            else
                # 재시도도 실패
                continue
            fi
        else
            # Rate Limit 아닌 다른 실패
            continue
        fi
    fi

    # Step 2: 프로필 조회 (userId 획득)
    profile_response=$(curl -s \
        -X GET "${BASE_URL}/api/v1/users/me" \
        -H "Authorization: Bearer ${access_token}")

    user_id=$(echo "$profile_response" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('id', d.get('userId', '')))
except:
    print('')
" 2>/dev/null || echo "")

    if [ -n "$user_id" ] && [ "$user_id" != "" ]; then
        login_ok=$((login_ok + 1))
        echo -ne "${GREEN}.${RESET}"

        if [ "$first" = true ]; then
            first=false
        else
            echo "," >> "$OUTPUT_FILE"
        fi

        cat >> "$OUTPUT_FILE" << ENTRY
  {"vuId": ${i}, "accessToken": "${access_token}", "userId": ${user_id}, "email": "${email}"}
ENTRY
    else
        # userId 조회 실패
        login_fail=$((login_fail + 1))
        echo -ne "${RED}x${RESET}"
    fi

    # nginx rate limit 준수: 매 요청 13초 대기 (5r/m = 12초에 1개)
    if (( i < USER_COUNT )); then
        sleep 13
    fi
done

echo "" >> "$OUTPUT_FILE"
echo "]" >> "$OUTPUT_FILE"

# JSON 정리
python3 -c "
import json
with open('${OUTPUT_FILE}') as f:
    data = json.load(f)
with open('${OUTPUT_FILE}', 'w') as f:
    json.dump(data, f, indent=2)
print()
" 2>/dev/null || true

echo ""
log_info "로그인 결과: 성공=${login_ok}, 실패=${login_fail}"

# ===========================================
# 결과
# ===========================================
echo ""
log_success "=== 시딩 완료 ==="
log_info "회원가입: 생성=${signup_ok}, 이미존재=${signup_skip}, 실패=${signup_fail}"
log_info "이메일 인증: $([ -n "${NAS_SSH:-}" ] && echo "SSH 자동처리" || echo "수동처리")"
log_info "로그인: 성공=${login_ok}, 실패=${login_fail}"
log_success "파일: ${OUTPUT_FILE}"
log_success "사용자: ${login_ok}명"
echo ""
log_info "k6 테스트 실행:"
log_info "  k6 run --env BASE_URL=${BASE_URL} k6/scenarios/rest-api.js"
