#!/bin/bash
# ===========================================
# Co-Talk PostgreSQL 백업 스크립트
# ===========================================
# 사용법: ./backup.sh
# 환경변수:
#   - POSTGRES_HOST: PostgreSQL 호스트 (기본값: postgres)
#   - POSTGRES_DB: 데이터베이스 이름 (기본값: cotalk)
#   - POSTGRES_USER: 사용자명 (기본값: cotalk)
#   - PGPASSWORD: 비밀번호 (필수)
#   - BACKUP_RETENTION_DAYS: 백업 보관 기간 (기본값: 7)
# ===========================================

set -e

# 환경변수 기본값
POSTGRES_HOST=${POSTGRES_HOST:-postgres}
POSTGRES_DB=${POSTGRES_DB:-cotalk}
POSTGRES_USER=${POSTGRES_USER:-cotalk}
BACKUP_RETENTION_DAYS=${BACKUP_RETENTION_DAYS:-7}
BACKUP_DIR=/backups

# 백업 파일명 (타임스탬프 포함)
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/${POSTGRES_DB}_${TIMESTAMP}.sql.gz"

echo "=========================================="
echo "Co-Talk PostgreSQL 백업 시작"
echo "=========================================="
echo "시간: $(date)"
echo "호스트: ${POSTGRES_HOST}"
echo "데이터베이스: ${POSTGRES_DB}"
echo "백업 파일: ${BACKUP_FILE}"
echo ""

# 비밀번호 확인
if [ -z "$PGPASSWORD" ]; then
    echo "오류: PGPASSWORD 환경변수가 설정되지 않았습니다."
    exit 1
fi

# 백업 디렉토리 생성
mkdir -p ${BACKUP_DIR}

# pg_dump 실행 (압축)
echo "백업 진행 중..."
pg_dump -h ${POSTGRES_HOST} -U ${POSTGRES_USER} -d ${POSTGRES_DB} \
    --format=plain \
    --no-owner \
    --no-privileges \
    | gzip > ${BACKUP_FILE}

# 백업 파일 크기 확인
BACKUP_SIZE=$(ls -lh ${BACKUP_FILE} | awk '{print $5}')
echo "백업 완료: ${BACKUP_FILE} (${BACKUP_SIZE})"

# 오래된 백업 파일 삭제
echo ""
echo "오래된 백업 정리 중 (${BACKUP_RETENTION_DAYS}일 이상)..."
find ${BACKUP_DIR} -name "*.sql.gz" -type f -mtime +${BACKUP_RETENTION_DAYS} -delete -print

# 현재 백업 목록
echo ""
echo "현재 백업 목록:"
ls -lh ${BACKUP_DIR}/*.sql.gz 2>/dev/null || echo "백업 파일 없음"

echo ""
echo "=========================================="
echo "백업 완료: $(date)"
echo "=========================================="
