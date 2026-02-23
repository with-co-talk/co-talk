<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-02-23 | Updated: 2026-02-23 -->

# k8s - Kubernetes 배포

## 개요
Kustomize 기반 K8s 배포 매니페스트. base + overlays(dev/prod) 구조.

## 디렉토리
| 디렉토리 | 용도 |
|-----------|------|
| `base/` | 공통 리소스 (Deployment, Service, HPA, PDB, NetworkPolicy 등 9개) |
| `overlays/dev/` | 개발 환경 오버레이 (1 레플리카, HPA max 3, 트레이싱 100%) |
| `overlays/prod/` | 프로덕션 오버레이 (3 레플리카, HPA 3~20, PDB minAvailable 2, 트레이싱 5%) |

## 주요 파일
| 파일 | 설명 |
|------|------|
| `base/deployment.yaml` | 기본 2 레플리카, 리소스 512Mi/250m~1Gi/1000m, 3종 probe, PodAntiAffinity |
| `base/hpa.yaml` | CPU 70%/메모리 80% 기준 오토스케일링 (min 2, max 10) |
| `base/pdb.yaml` | PodDisruptionBudget (minAvailable 1) |
| `base/networkpolicy.yaml` | 인그레스: nginx/monitoring만 허용, 이그레스: DB/Redis/MinIO/Zipkin/DNS만 허용 |

## AI 에이전트 가이드

### 현재 배포 방식
현재 실제 운영은 NAS Docker Compose 기반. K8s는 구성만 준비된 상태.

### 환경별 차이
| 설정 | dev | prod |
|------|-----|------|
| 레플리카 | 1 | 3 |
| HPA | max 3 | 3~20 |
| PDB | minAvailable 1 | minAvailable 2 |
| 리소스 한도 | base 기본값 | 2Gi/2000m |
| 트레이싱 | 100% | 5% |
| 로그 레벨 | DEBUG | INFO |

<!-- MANUAL: -->
