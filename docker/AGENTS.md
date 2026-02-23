<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-02-23 | Updated: 2026-02-23 -->

# docker - Docker 인프라

## 개요
Docker Compose 기반 운영/개발 환경 구성. Nginx 로드밸런싱, Prometheus/Grafana 모니터링 스택 포함.

## 디렉토리
| 디렉토리 | 용도 |
|-----------|------|
| `nginx/` | Nginx 리버스 프록시 설정 (앱 3인스턴스 로드밸런싱, Rate Limit) |
| `prometheus/` | Prometheus 스크레이핑 설정 + 알림 규칙 |
| `grafana/` | Grafana 대시보드 4개 + 데이터소스 프로비저닝 |
| `alertmanager/` | 알림 라우팅 (critical/warning 분리, 이메일 알림) |
| `loki/` | Loki 로그 집계 설정 |
| `promtail/` | Promtail 로그 수집 에이전트 설정 |
| `init-db/` | PostgreSQL 초기화 스크립트 |
| `backup/` | PostgreSQL 백업/복원 스크립트 (pg_dump 기반) |

## 주요 파일
| 파일 | 설명 |
|------|------|
| `nginx/nginx.conf` | 앱 3인스턴스 업스트림, gzip, WebSocket 업그레이드, k6 Rate Limit 바이패스 |
| `prometheus/prometheus.yml` | 5초 스크레이핑, 3개 앱 인스턴스 `/actuator/prometheus` |
| `grafana/dashboards/` | JVM+HTTP, 비즈니스 메트릭, 전체 개요, k6 부하테스트 대시보드 |
| `backup/backup.sh` | pg_dump 기반 백업 (타임스탬프 파일명, 보관 기간 설정) |
| `backup/restore.sh` | 백업 복원 스크립트 |

## AI 에이전트 가이드

### Docker Compose 파일 용도
| 파일 | 용도 |
|------|------|
| `docker-compose.yml` | 운영 환경 전체 (App×3 + DB + Redis + MinIO + Nginx) |
| `docker-compose.dev.yml` | 개발용 인프라만 (앱은 로컬 실행) |
| `docker-compose.monitoring.yml` | 모니터링 스택 (Prometheus + Grafana + Loki + Promtail + Alertmanager) |
| `docker-compose.backup.yml` | 백업 서비스 |
| `docker-compose.nas.yml` | NAS 배포 전용 |

### 배포 흐름
```
docker-compose.yml 실행
  → scripts/deploy.sh 카나리 롤링 배포
  → app-1 카나리 → 헬스체크(120초) → Prometheus 5xx 검증(<5%)
  → app-2, app-3 순차 배포
```

<!-- MANUAL: -->
