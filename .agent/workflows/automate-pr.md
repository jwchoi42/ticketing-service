---
description: PR 생성, CI 완료 대기 후 병합 과정을 자동화합니다. (GitHub CLI 필요)
---

GitHub CLI(`gh`)를 사용하여 PR 생성부터 병합까지 자동화하는 절차입니다.

## 전제 조건
- [GitHub CLI](https://cli.github.com/)가 설치되어 있어야 합니다. (`winget install --id GitHub.cli`)
- `gh auth login`을 통해 로그인이 되어 있어야 합니다.

## 실행 단계

1. 현재 변경사항을 커밋하고 푸시합니다.
// turbo
```bash
git push origin $(git branch --show-current)
```

2. PR을 생성합니다. (`--fill` 옵션은 커밋 메시지를 바탕으로 제목과 내용을 자동 채웁니다)
// turbo
```bash
gh pr create --fill
```

3. GitHub Actions 체크가 완료될 때까지 기다립니다.
// turbo
```bash
gh pr checks --watch
```

4. 체크가 성공하면 PR을 병합하고 브랜치를 삭제합니다.
// turbo
```bash
gh pr merge --merge --delete-branch
```

---

**팁**: 위 과정을 한 번에 실행하려면 다음 라이너를 사용할 수 있습니다:
```bash
git push origin $(git branch --show-current) && gh pr create --fill && gh pr checks --watch && gh pr merge --merge --delete-branch
```
