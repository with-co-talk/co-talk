# Co-Talk 문서 목차

> `co-talk/docs/` 디렉터리 문서 인덱스. 역할별로 구분되어 있습니다.

---

## 📌 기획·백로그

| 문서 | 설명 |
|------|------|
| [BACKLOG_AND_WORKPLAN.md](./BACKLOG_AND_WORKPLAN.md) | 미구현/부분구현 기능 목록, 버전별 작업계획(Phase 1~5), 기술 부채 참고사항. **기능 기획·우선순위 참고용.** |

---

## 🚀 출시·운영

| 문서 | 설명 |
|------|------|
| [PRODUCTION_READINESS.md](./PRODUCTION_READINESS.md) | 프로덕션 준비 가이드: 아키텍처 요약, P0/P1 이슈, 배포·운영·보안 체크리스트. **배포 전 참고.** |
| [AUDIT_REPORT_2026_02.md](./AUDIT_REPORT_2026_02.md) | 2026년 2월 프로덕션 감사 보고서. 보안/안정성 이슈 및 수정 내역. **감사·검증 이력 참고.** |

---

## 📖 기능 가이드

| 문서 | 설명 |
|------|------|
| [READ_FEATURE.md](./READ_FEATURE.md) | **읽기 기능(읽음 처리)** 상세 가이드: REST API, WebSocket 이벤트, 클라이언트 구현·문제 해결. |
| [FEATURE_CHECK_SUMMARY.md](./FEATURE_CHECK_SUMMARY.md) | 읽기 기능 점검 요약(2026-01-26). 검증 결과·문서화 상태. |
| [server-team-verification-markAsRead.md](./server-team-verification-markAsRead.md) | 읽기 기능 서버팀 검증 Q&A. markAsRead 후 WebSocket 동작·unreadCount 정책 확인용. |

---

## 📋 검증·변경 이력

| 문서 | 설명 |
|------|------|
| [VERIFICATION_REPORT.md](./VERIFICATION_REPORT.md) | 기능 검증 보고서(2026-02-07): 테스트/TDD/헥사고날 아키텍처 검토, 권장 조치. |
| [CHANGES_2026-02-07.md](./CHANGES_2026-02-07.md) | 2026-02-07 변경 기록: Flutter 스피너 버그, 서버 레이턴시 최적화, 무중단 배포·CI/CD. |
| [fix-report-push-notification-and-optimization.md](./fix-report-push-notification-and-optimization.md) | 푸시 알림·WebSocket 연결 수정 및 N+1 최적화 보고서(2026-02-07). |

---

## 🔧 리팩토링·설계

| 문서 | 설명 |
|------|------|
| [REFACTORING_GUIDE.md](./REFACTORING_GUIDE.md) | 리팩토링 가이드: BaseEntity, UseCase 구조, 테스트·의존성 등. **구조 개선 시 참고.** |
| [DTO_REFACTORING_ANALYSIS.md](./DTO_REFACTORING_ANALYSIS.md) | DTO 분리·정리 분석(WebSocket vs REST, 중복 구조). 리팩토링 시 DTO 작업 참고. |

---

## 문서 역할 요약

- **기능 이해가 필요할 때** → `READ_FEATURE.md`, `BACKLOG_AND_WORKPLAN.md`
- **배포·운영 준비** → `PRODUCTION_READINESS.md`, 루트의 `RELEASE_CHECKLIST.md`·`RELEASE_PRE_CHECKLIST.md`
- **과거 변경·검증 이력** → `CHANGES_2026-02-07.md`, `VERIFICATION_REPORT.md`, `AUDIT_REPORT_2026_02.md`
- **코드 구조 개선** → `REFACTORING_GUIDE.md`, `DTO_REFACTORING_ANALYSIS.md`

---

*마지막 정리: 2026-02-17*
