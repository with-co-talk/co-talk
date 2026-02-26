# commands

Cursor와 Claude Code(OMC)에서 같이 쓰는 명령/스킬의 **원본 파일**을 두는 폴더입니다.  
한 파일만 고치면 Cursor 커맨드와 OMC 스킬 둘 다 반영되도록 심볼릭 링크로 연결해 두었습니다.

## 구조

```
commands/
├── README.md                      # 이 설명
└── pr-review-github-inline.md    # PR 코드리뷰 → GitHub 인라인 리뷰 (원본)

.cursor/commands/
└── pr-review-github-inline.md -> ../commands/pr-review-github-inline.md  # Cursor 커맨드
```

- **Cursor**: `.cursor/commands/pr-review-github-inline.md`가 `commands/pr-review-github-inline.md`를 가리킵니다.  
  슬래시 커맨드(예: `/sc:pr-review-github-inline`) 또는 “PR N번 리뷰 인라인으로 해줘”처럼 요청하면 이 내용을 참고해 동작합니다.
- **OMC(Claude Code)**: 로컬 스킬로 같은 파일을 참조합니다.  
  `~/.claude/.../skills/pr-review-github-inline/SKILL.md` → `commands/pr-review-github-inline.md` (절대 경로 심볼릭 링크).  
  OMC는 이 스킬을 자동 매칭에 사용합니다.

## 수정 방법

- **항상 여기만 고치면 됩니다.**  
  `commands/pr-review-github-inline.md`만 수정하세요.  
  Cursor와 OMC 둘 다 이 파일을 보므로 따로 두 군데 수정할 필요 없습니다.

## OMC 스킬 링크 (이 프로젝트 기준)

다른 PC나 경로에서 쓰려면 OMC 스킬 쪽 심볼릭 링크를 다시 걸어줘야 할 수 있습니다.

```bash
# 예: co-talk 클론 경로가 다를 때
ln -sf "$(pwd)/commands/pr-review-github-inline.md" \
  ~/.claude/plugins/cache/omc/oh-my-claudecode/3.7.0/skills/pr-review-github-inline/SKILL.md
```

(OMC 버전/경로가 다르면 해당 스킬 디렉터리로 경로만 바꾸면 됩니다.)
