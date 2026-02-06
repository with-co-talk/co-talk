#!/bin/bash
# ===========================================
# Co-Talk PostgreSQL 복원 스크립트
# ===========================================
# 사용법: ./restore.sh <백업_파일.sql.gz>
# 환경변수:
#   - POSTGRES_HOST: PostgreSQL 호스트 (기본값: postgres)
#   - POSTGRES_DB: 데이터베이스 이름 (기본값: cotalk)
#   - POSTGRES_USER: 사용자명 (기본값: cotalk)
#   - PGPASSWORD: 비밀번호 (필수)
# ===========================================

set -e

# 환경변수 기본값
POSTGRES_HOST=${POSTGRES_HOST:-postgres}
POSTGRES_DB=${POSTGRES_DB:-cotalk}
POSTGRES_USER=${POSTGRES_USER:-cotalk}
BACKUP_DIR=/backups

# 백업 파일 확인
if [ -z "$1" ]; then
    echo "사용법: ./restore.sh <백업_파일.sql.gz>"
    echo ""
    echo "사용 가능한 백업 파일:"
    ls -lh ${BACKUP_DIR}/*.sql.gz 2>/dev/null || echo "백업 파일 없음"
    exit 1
fi

BACKUP_FILE=$1

# 파일 존재 확인
if [ ! -f "${BACKUP_FILE}" ]; then
    # 백업 디렉토리에서 찾기
    if [ -f "${BACKUP_DIR}/${BACKUP_FILE}" ]; then
        BACKUP_FILE="${BACKUP_DIR}/${BACKUP_FILE}"
    else
        echo "오류: 백업 파일을 찾을 수 없습니다: ${BACKUP_FILE}"
        exit 1
    fi
fi

# 비밀번호 확인
if [ -z "$PGPASSWORD" ]; then
    echo "오류: PGPASSWORD 환경변수가 설정되지 않았습니다."
    exit 1
fi

echo "=========================================="
echo "Co-Talk PostgreSQL 복원 시작"
echo "=========================================="
echo "시간: $(date)"
echo "호스트: ${POSTGRES_HOST}"
echo "데이터베이스: ${POSTGRES_DB}"
echo "백업 파일: ${BACKUP_FILE}"
echo ""

# 경고
echo "경고: 이 작업은 현재 데이터베이스를 덮어씁니다!"
echo "계속하시겠습니까? (yes/no)"
read -r CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "복원이 취소되었습니다."
    exit 0
fi

# 복원 실행
echo ""
echo "복원 진행 중..."
gunzip -c ${BACKUP_FILE} | psql -h ${POSTGRES_HOST} -U ${POSTGRES_USER} -d ${POSTGRES_DB}

echo ""
echo "=========================================="
echo "복원 완료: $(date)"
echo "=========================================="
