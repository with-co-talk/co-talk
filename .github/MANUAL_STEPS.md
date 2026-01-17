# 이슈 및 PR 생성 가이드

## 사전 준비
1. GitHub CLI 인증 확인
```bash
gh auth status
```

2. 인증이 안 되어 있다면
```bash
gh auth login
```

3. 브랜치 푸시 (아직 안 했다면)
```bash
git push -u origin refactor/base-entity-and-feature-improvements
```

## 방법 1: 스크립트 사용 (권장)
```bash
./.github/create_issue_and_pr.sh
```

## 방법 2: 수동 생성

### 이슈 생성
```bash
gh issue create \
  --title "[REFACTOR] BaseEntity 적용 및 주요 기능 개선" \
  --body-file .github/issue_content.md \
  --label "refactor" \
  --assignee "@me" \
  --repo with-co-talk/co-talk
```

생성된 이슈 번호를 확인한 후, PR 생성 시 사용하세요.

### PR 생성
```bash
# 이슈 번호를 확인한 후 아래 명령어에서 [이슈번호]를 실제 번호로 변경
gh pr create \
  --title "[REFACTOR] BaseEntity 적용 및 주요 기능 개선" \
  --body-file .github/pr_content.md \
  --base main \
  --head refactor/base-entity-and-feature-improvements \
  --assignee "@me" \
  --repo with-co-talk/co-talk
```

PR 본문에서 `Closes #[이슈번호]` 부분을 실제 이슈 번호로 수정해주세요.
