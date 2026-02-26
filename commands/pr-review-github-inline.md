---
name: pr-review-github-inline
description: PR 코드리뷰를 GitHub PR에 직접 인라인 코멘트로 남긴다. gh CLI로 리뷰 본문과 라인별 코멘트를 POST한다.
---

# PR 코드리뷰 → GitHub 인라인 리뷰

## Triggers (Cursor) / When to use (OMC)

- 사용자가 "PR N번 코드리뷰", "N번 PR 리뷰 인라인으로 해줘", "GitHub에 직접 리뷰 남겨줘" 등으로 요청할 때
- PR 번호가 대화 또는 브랜치에서 유추 가능할 때

## Usage (Cursor)

```
/sc:pr-review-github-inline [<PR 번호>]
# PR 번호 생략 시 현재 브랜치가 원격 PR과 연결돼 있으면 해당 PR 사용, 또는 사용자에게 번호 요청
```

## 원칙: 인라인 vs 본문

| 구분 | 내용 |
|------|------|
| **인라인 코멘트** | 고칠 점, 제안, 질문만. 해당 라인에서 액션 가능한 피드백. |
| **리뷰 본문** | 변경 요약, 좋은 점/잘한 부분, 전반 평가, APPROVE/REQUEST_CHANGES 결론. |

좋은 점을 인라인으로 달면 노이즈가 되므로 본문에만 쓴다.

## 목표

1. 해당 PR의 diff를 기준으로 코드리뷰 수행
2. 본문에는 요약·긍정·결론, 인라인에는 수정/제안/질문만 작성
3. GitHub API(`gh api repos/OWNER/REPO/pulls/PR/reviews`)로 한 번에 제출

## 전제 조건

- `gh` CLI 설치 및 인증 완료 (`gh auth status`)
- 저장소가 `gh pr view <N>` 가능한 상태 (origin 연결)

## 절차

### 1. PR 번호·저장소·헤드 커밋 확보

```bash
# PR 번호: 사용자 입력 또는 현재 브랜치에서 추론
gh pr view --json number,headRefOid,baseRefOid,state,title

# 예시 출력에서 headRefOid(리뷰 타깃 커밋), number 사용
# 저장소: git remote get-url origin → owner/repo 추출
```

- `headRefOid`: 리뷰 API의 `commit_id`로 사용
- PR이 없으면 "PR 번호를 알려주세요" 또는 브랜치 지정 요청

### 2. diff 및 변경 파일 분석

```bash
git fetch origin pull/<PR>/head:pr-<PR>   # 필요 시
git diff main...pr-<PR> --stat
git diff main...pr-<PR>   # 전체 diff
```

- **인라인용**: 고칠 점·제안·질문만 정리 (어디를 어떻게 바꾸면 좋은지, 확인이 필요한지)
- **본문용**: 좋은 점·전반 평가는 리뷰 본문에만 쓴다. 인라인에는 넣지 않는다.
- 인라인 코멘트를 남길 **파일 경로**와 **헤드 커밋 기준 라인 번호** 확인  
  - 라인 번호는 `git show pr-<PR>:path/to/file | cat -n` 등으로 헤드 버전 기준으로 확인

### 3. 리뷰 본문 작성

- 마크다운으로 요약: 변경 요약, **좋은 점/잘한 부분**, 전반 의견, 결론(APPROVE / REQUEST_CHANGES / COMMENT)
- 칭찬·긍정 피드백은 여기만 쓴다. 인라인에는 쓰지 않는다.
- 4000자 이내 권장 (API 제한 고려)

### 4. 인라인 코멘트 목록 작성

**인라인 = 액션 가능한 피드백만** (고칠 점, 제안, 질문). 좋은 점은 본문에만 쓴다.

각 코멘트 형식:

- `path`: 저장소 루트 기준 파일 경로 (예: `src/main/java/.../SecurityConfig.java`)
- `line`: 헤드 커밋 기준 1-based 라인 번호
- `body`: 구체적 수정 제안·질문·개선 아이디어 (마크다운 가능)

고칠 게 없으면 인라인 0개로 두고 본문만 제출해도 된다. 개수는 필요한 만큼만 (보통 0~10개).

### 5. GitHub API로 리뷰 제출

```bash
# 리뷰 JSON 생성 (commit_id, event, body, comments[])
# event: "COMMENT" | "APPROVE" | "REQUEST_CHANGES"

gh api repos/<owner>/<repo>/pulls/<PR>/reviews -X POST --input - <<'JSON'
{
  "commit_id": "<headRefOid>",
  "event": "COMMENT",
  "body": "## 리뷰 요약\n\n...",
  "comments": [
    { "path": "src/.../File.java", "line": 67, "body": "코멘트 내용" }
  ]
}
JSON
```

- `commit_id`는 반드시 해당 PR의 `headRefOid`
- `comments`의 `line`은 **헤드 커밋** 버전 파일의 라인 번호
- JSON 이스케이프(따옴표, 줄바꿈) 주의

### 6. 결과 확인

- API 성공 시 리뷰 URL 출력
- `gh api repos/<owner>/<repo>/pulls/<PR>/comments` 로 인라인 코멘트 목록 확인 가능

## 구현 시 유의사항

- **한 번에 제출**: 리뷰 본문 + 인라인 코멘트를 한 번의 `POST .../pulls/<PR>/reviews`로 전달
- **라인 번호**: diff의 왼쪽(OLD)이 아니라 **오른쪽(NEW, 헤드)** 파일 기준
- **긴 본문**: `--input -` 과 heredoc 또는 임시 파일로 JSON 전달 (쉘 이스케이프 주의)
- **인증**: `gh`가 이미 로그인돼 있어야 함

## Will

- PR diff 기준으로 코드리뷰 후 GitHub에 리뷰 본문 + 인라인 코멘트 제출
- `gh api`로만 수행 (웹 UI 수동 작업 없음)
- PR 번호/브랜치에서 PR 식별 가능하면 자동 사용

## Will Not

- 리뷰 내용을 채팅에만 쓰고 GitHub에는 안 남기기
- 사용자 인증 없이 `gh` 대신 다른 수단으로 API 호출하기
