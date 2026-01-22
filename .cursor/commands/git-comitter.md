# /sc:git-comitter - 스마트 Git 커밋 및 이슈/PR 생성

## Triggers
- Git 변경사항을 의미있는 단위로 커밋하고 이슈/PR을 자동 생성하고 싶을 때
- 작업 완료 후 정리된 커밋 히스토리와 문서화가 필요할 때
- 브랜치, 이슈, PR을 한 번에 생성하고 싶을 때

## Usage
```
/sc:git-comitter [--base-branch <branch>] [--auto-push]
```

## Behavioral Flow

### Phase 1: 변경사항 분석 및 스마트 커밋
1. **변경사항 확인**: `git status`로 변경된 파일 확인
2. **스마트 분석**: `/sc/git --smart-commit`를 사용하여 변경사항 분석
3. **의미있는 단위 분리**: 
   - 기능별로 그룹화 (새 기능, 리팩토링, 버그 수정 등)
   - 테스트 코드는 별도 그룹
   - 문서화는 별도 그룹
   - 설정 파일은 별도 그룹
4. **커밋 생성**: 각 그룹을 Conventional Commits 형식으로 커밋
   - `feat:`, `refactor:`, `fix:`, `test:`, `docs:`, `chore:` 등

### Phase 2: 브랜치 생성
1. **변경사항 유형 판단**: 
   - 새로운 기능 추가 → `feat/`
   - 코드 리팩토링 → `refactor/`
   - 버그 수정 → `bugfix/`
   - 문서화 → `docs/`
   - 기타 → `chore/`
2. **브랜치명 생성**: `{type}/{kebab-case-description}`
3. **브랜치 생성 및 체크아웃**: `git checkout -b {branch-name}`

### Phase 3: 이슈 생성
1. **이슈 타입 결정**: 변경사항 분석 결과 기반
   - Feature: 새로운 기능 관련 변경
   - Refactor: 코드 구조 개선
   - Bug: 버그 수정
   - Docs: 문서 변경
2. **이슈 본문 생성**: 
   - 변경사항 요약 작성
   - 목표 및 주요 변경사항 정리
   - 가독성을 위해 간결하게 작성
   - 임시 파일로 저장 (커밋하지 않음)
3. **이슈 생성**:
   ```bash
   gh issue create \
     --title "[{TYPE}] {제목}" \
     --body-file .github/issue_body.md \
     --label "{라벨}"
   ```
4. **이슈 번호 저장**: 생성된 이슈 번호를 변수에 저장
5. **임시 파일 삭제**: `.github/issue_body.md` 삭제

### Phase 4: PR 생성
1. **브랜치 푸시**: PR 생성 전 브랜치를 원격에 푸시
   ```bash
   git push -u origin {branch-name}
   ```
2. **PR 본문 생성**:
   - 변경사항 상세 리스트 작성
   - 통계 정보 추가 (변경 파일 수, 추가/삭제 줄 수, 커밋 수)
   - `Closes #{이슈번호}` 자동 추가 (이슈가 생성된 경우)
   - 테스트 방법 및 리뷰 포인트 작성
   - 임시 파일로 저장 (커밋하지 않음)
3. **PR 생성**:
   ```bash
   gh pr create \
     --title "[{TYPE}] {제목}" \
     --body-file .github/pr_body.md \
     --base {base-branch} \
     --head {current-branch} \
     --label "{라벨}"
   ```
4. **임시 파일 삭제**: `.github/pr_body.md` 삭제


## Tool Coordination
- **Git Operations**: 변경사항 분석, 커밋, 브랜치 생성
- **GitHub CLI**: 이슈 및 PR 생성
- **File Operations**: 이슈/PR 본문 템플릿 생성
- **Analysis**: 변경사항 분석하여 타입, 라벨, 제목 자동 결정
- **Smart Commit**: `/sc/git --smart-commit` 활용

## Key Patterns

### 변경사항 분류 규칙
1. **기능 추가 (Feature)**
   - 새로운 서비스 클래스 추가
   - 새로운 엔티티/도메인 추가
   - 새로운 API 엔드포인트 추가
   - 라벨: `enhancement`

2. **리팩토링 (Refactor)**
   - 기존 코드 구조 개선
   - 중복 코드 제거
   - 인터페이스 도입
   - 라벨: `refactor`

3. **버그 수정 (Bug)**
   - 예외 처리 추가
   - 버그 수정
   - 라벨: `bug`

4. **문서화 (Docs)**
   - README 업데이트
   - 주석 추가
   - 문서 파일 변경
   - 라벨: `documentation`

### 브랜치명 생성 규칙
- 소문자 사용
- 단어는 하이픈(-)으로 구분
- 의미있는 설명 포함
- 예: `feat/refresh-token`, `refactor/exception-handling`

### 이슈/PR 제목 규칙
- 타입 접두사: `[FEAT]`, `[REFACTOR]`, `[BUG]`, `[DOCS]`
- 간결하고 명확한 설명
- 50자 이내 권장

### 본문 작성 규칙
- 가독성 우선: 간결하고 명확하게
- 마크다운 포맷 사용
- 이모지 활용 (📋, 🎯, ✅ 등)
- 체크리스트 포함
- 통계 정보 포함 (PR의 경우)

## Implementation Steps

### Step 1: 사전 확인
```bash
# Git 상태 확인
git status

# 변경사항이 없으면 종료
if [ -z "$(git status --porcelain)" ]; then
  echo "변경사항이 없습니다."
  exit 0
fi
```

### Step 2: 변경사항 분석
```bash
# 변경된 파일 목록
git status --short

# 변경 내용 분석
git diff --stat
git diff --cached --stat
```

### Step 3: 스마트 커밋
- `/sc/git --smart-commit` 호출하여 의미있는 단위로 커밋
- 각 커밋은 Conventional Commits 형식 준수

### Step 4: 브랜치 생성
```bash
# 변경사항 유형에 따라 브랜치명 결정
BRANCH_TYPE="feat"  # 또는 refactor, bugfix, docs, chore
BRANCH_DESC="kebab-case-description"
BRANCH_NAME="${BRANCH_TYPE}/${BRANCH_DESC}"

# 브랜치 생성
git checkout -b "$BRANCH_NAME"
```

### Step 5: 이슈 본문 생성
- 변경사항을 분석하여 이슈 본문 작성
- 템플릿 형식에 맞춰 작성
- `.github/issue_body.md`에 저장

### Step 6: 이슈 생성
```bash
# 이슈 생성
ISSUE_NUMBER=$(gh issue create \
  --title "[${ISSUE_TYPE}] ${ISSUE_TITLE}" \
  --body-file .github/issue_body.md \
  --label "${LABEL}" \
  2>&1 | grep -oE 'issues/[0-9]+' | grep -oE '[0-9]+' | head -1)

if [ -n "$ISSUE_NUMBER" ]; then
  echo "✅ 이슈 생성 완료: #$ISSUE_NUMBER"
  # 임시 파일 삭제
  rm -f .github/issue_body.md
else
  echo "⚠️  이슈 생성 실패"
fi
```

### Step 7: 브랜치 푸시
```bash
# PR 생성 전 브랜치를 원격에 푸시
git push -u origin "$BRANCH_NAME"
```

### Step 8: PR 본문 생성
- 이슈 번호를 포함하여 PR 본문 작성
- `Closes #${ISSUE_NUMBER}` 자동 추가 (이슈가 생성된 경우)
- 통계 정보 포함
- `.github/pr_body.md`에 임시 저장

### Step 9: PR 생성
```bash
# PR 본문에 이슈 번호 추가 (이슈가 생성된 경우)
if [ -n "$ISSUE_NUMBER" ]; then
  sed "s/Closes #\[이슈번호\]/Closes #$ISSUE_NUMBER/" .github/pr_body.md > .github/pr_body_final.md
  PR_BODY_FILE=".github/pr_body_final.md"
else
  PR_BODY_FILE=".github/pr_body.md"
fi

# PR 생성
PR_URL=$(gh pr create \
  --title "[${PR_TYPE}] ${PR_TITLE}" \
  --body-file "$PR_BODY_FILE" \
  --base "${BASE_BRANCH:-main}" \
  --head "$BRANCH_NAME" \
  --label "${LABEL}" 2>&1)

if [ $? -eq 0 ]; then
  echo "✅ PR 생성 완료: $PR_URL"
  # 임시 파일 삭제
  rm -f .github/pr_body.md .github/pr_body_final.md
else
  echo "⚠️  PR 생성 실패"
fi
```

### Step 10: 푸시 (옵션)
```bash
if [ "$AUTO_PUSH" = "true" ]; then
  git push -u origin "$BRANCH_NAME"
fi
```

## Examples

### 기본 사용
```
/sc:git-comitter
# 현재 변경사항을 분석하여 자동으로 커밋, 브랜치, 이슈, PR 생성
```

### 베이스 브랜치 지정
```
/sc:git-comitter --base-branch develop
# develop 브랜치를 기준으로 PR 생성
```

### 자동 푸시 포함
```
/sc:git-comitter --auto-push
# 커밋 후 자동으로 푸시
```

## Output Files

- `.github/issue_body.md`: 생성된 이슈 본문 (임시, 생성 후 삭제)
- `.github/pr_body.md`: 생성된 PR 본문 (임시, 생성 후 삭제)
- Git에 커밋하지 않음

## Error Handling

- **Git 변경사항 없음**: 종료 메시지 출력
- **이슈 생성 실패**: 에러 메시지 출력, 임시 파일은 삭제
- **PR 생성 실패**: 에러 메시지 출력, 임시 파일은 삭제
- **브랜치 충돌**: 기존 브랜치 확인 후 처리

## Boundaries

**Will:**
- 변경사항을 분석하여 의미있는 단위로 커밋
- 변경사항에 맞는 브랜치, 이슈, PR 자동 생성
- GitHub CLI를 사용한 이슈/PR 생성
- 임시 파일은 생성 후 자동 삭제

**Will Not:**
- 강제 푸시 (force push) 수행
- 메인 브랜치에 직접 커밋
- 사용자 확인 없이 원격 저장소에 푸시 (--auto-push 옵션 제외)
- 기존 이슈/PR 수정
