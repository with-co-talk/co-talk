## 개요
아키텍처 리팩토링 및 git-comitter 명령 개선

## 목표
- git-comitter 명령의 브랜치 생성 로직 개선
- 아키텍처 전반 리팩토링 및 Presence 기능 개선
- Domain, Adapter, Application, Infrastructure 레이어 개선

## 주요 변경사항

### git-comitter 명령 개선
- 항상 변경사항에 맞는 새 브랜치 생성하도록 수정
- 현재 브랜치와 관계없이 무조건 새 브랜치 생성
- 브랜치명 생성 로직 개선

### 아키텍처 리팩토링
- Domain 레이어: 엔티티 및 포트 인터페이스 개선
- Adapter 레이어: Controller, DTO, Repository 개선
- Application 레이어: Service 로직 개선
- Infrastructure 레이어: Messaging 및 WebSocket 개선
- 테스트 코드 업데이트

## 통계
- 변경 파일: 52개
- 추가: 1,976줄 / 삭제: 506줄
- 커밋: 2개

## 체크리스트
- [ ] 코드 리뷰 준비 완료
- [ ] 테스트 통과
- [ ] 문서 업데이트 완료
