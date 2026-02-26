#/sc:pr-review-github-inline - GitHub PR 인라인 코드리뷰

## Triggers
- 사용자가 "PR N번 코드리뷰", "N번 PR 리뷰 인라인으로 해줘", "GitHub에 직접 리뷰 남겨줘" 등으로 요청할 때
- PR 번호가 대화 또는 현재 브랜치에서 유추 가능할 때

## Usage
```
/sc:pr-review-github-inline [<PR 번호>]
# PR 번호를 생략하면:
# 1) 현재 브랜치에 연결된 원격 PR이 있으면 그 PR을 사용
# 2) 없으면 PR 번호를 물어본 뒤 진행
```

## Behavior
- 현재 저장소에서 대상 PR 번호와 head 커밋(`headRefOid`)을 확인한다.
- PR diff를 기준으로 코드리뷰를 수행한다.
  - **리뷰 본문**: 요약, 좋은 점/잘한 부분, 전반 평가, APPROVE/REQUEST_CHANGES/COMMENT 결론
  - **인라인 코멘트**: 고칠 점, 제안, 질문처럼 액션 가능한 피드백만 작성
- 인라인 코멘트는 GitHub PR의 **해당 파일/라인에 직접 달린 상태**로 올라간다.
- 리뷰 본문과 인라인 코멘트를 한 번의 `gh api repos/<owner>/<repo>/pulls/<PR>/reviews` 호출로 제출한다.

## Notes
- 실제 gh 호출/JSON 구성, 라인 번호 계산 등 상세 절차와 제약사항은
  `commands/pr-review-github-inline.md` 스펙을 그대로 따른다.
- 이 커맨드는 **GitHub 웹 UI 사용 없이, gh CLI만으로** 리뷰를 생성하는 흐름을 따른다.

