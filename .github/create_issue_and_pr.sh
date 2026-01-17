#!/bin/bash

# GitHub CLI 인증 확인
if ! gh auth status &> /dev/null; then
    echo "❌ GitHub CLI 인증이 필요합니다."
    echo "다음 명령어로 인증해주세요: gh auth login"
    exit 1
fi

# 이슈 생성
echo "📝 이슈 생성 중..."
ISSUE_NUMBER=$(gh issue create \
    --title "[REFACTOR] BaseEntity 적용 및 주요 기능 개선" \
    --body-file .github/issue_content.md \
    --label "refactor" \
    --assignee "@me" \
    --repo with-co-talk/co-talk | grep -oE '[0-9]+' | head -1)

if [ -z "$ISSUE_NUMBER" ]; then
    echo "❌ 이슈 생성 실패"
    exit 1
fi

echo "✅ 이슈 생성 완료: #$ISSUE_NUMBER"

# PR 본문에 이슈 번호 추가
sed "s/Closes #\[이슈번호\]/Closes #$ISSUE_NUMBER/" .github/pr_content.md > .github/pr_content_final.md

# PR 생성
echo "📝 PR 생성 중..."
gh pr create \
    --title "[REFACTOR] BaseEntity 적용 및 주요 기능 개선" \
    --body-file .github/pr_content_final.md \
    --base main \
    --head refactor/base-entity-and-feature-improvements \
    --assignee "@me" \
    --repo with-co-talk/co-talk

# 임시 파일 삭제
rm -f .github/pr_content_final.md

echo "✅ PR 생성 완료"
