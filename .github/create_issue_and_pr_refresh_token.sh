#!/bin/bash

# GitHub CLI 인증 확인 (github.com만)
if ! gh auth status --hostname github.com &>/dev/null; then
    echo "❌ GitHub CLI 인증이 필요합니다."
    echo "다음 명령어로 인증해주세요: gh auth login --hostname github.com"
    exit 1
fi

# 사용자 이름 가져오기
USERNAME=$(gh api user --jq .login 2>/dev/null)
if [ -z "$USERNAME" ]; then
    echo "❌ 사용자 정보를 가져올 수 없습니다."
    exit 1
fi

REPO="with-co-talk/co-talk"
BRANCH_NAME="feat/refresh-token-and-distributed-systems"
ISSUE_TYPE="FEAT"
ISSUE_TITLE="Refresh Token, Redis Worker ID 할당 및 분산 락 기능 추가"
LABEL="enhancement"

# 이슈 생성
echo "📝 이슈 생성 중..."
ISSUE_NUMBER=$(gh issue create \
    --title "[${ISSUE_TYPE}] ${ISSUE_TITLE}" \
    --body-file .github/issue_body_new.md \
    --label "${LABEL}" \
    --assignee "$USERNAME" \
    --repo "$REPO" 2>&1 | grep -oE 'issues/[0-9]+' | grep -oE '[0-9]+' | head -1)

if [ -z "$ISSUE_NUMBER" ]; then
    echo "❌ 이슈 생성 실패. 수동으로 생성해주세요."
    echo "이슈 제목: [${ISSUE_TYPE}] ${ISSUE_TITLE}"
    echo "이슈 내용은 .github/issue_body_new.md 파일을 참고하세요."
    exit 1
fi

echo "✅ 이슈 생성 완료: #$ISSUE_NUMBER"

# PR 본문에 이슈 번호 추가
sed "s/Closes #\[이슈번호\]/Closes #$ISSUE_NUMBER/" .github/pr_body_new.md > .github/pr_body_final.md

# PR 생성
echo "📝 PR 생성 중..."
PR_URL=$(gh pr create \
    --title "[${ISSUE_TYPE}] ${ISSUE_TITLE}" \
    --body-file .github/pr_body_final.md \
    --base main \
    --head "$BRANCH_NAME" \
    --label "${LABEL}" \
    --assignee "$USERNAME" \
    --repo "$REPO" 2>&1)

if [ $? -eq 0 ]; then
    echo "✅ PR 생성 완료: $PR_URL"
else
    echo "❌ PR 생성 실패. 수동으로 생성해주세요."
    echo "PR 내용은 .github/pr_body_final.md 파일을 참고하세요."
fi

# 임시 파일 삭제
rm -f .github/pr_body_final.md
