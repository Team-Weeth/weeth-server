---
name: git-prepare
description: Prepare Weeth git work. Use when asked to create a branch, sync a branch, prepare a commit, write a commit message, commit changes, or prepare a PR. Do NOT use for ordinary code edits with no git operation requested.
---

# Git Prepare

Use this skill only when the task includes a git operation.

## Workflow

### Step 1: Inspect Worktree

Run `git status --short` and identify unrelated user changes. Do not revert, restage, or include unrelated files unless the user explicitly asks.

### Step 2: Branch Naming

Use these patterns when creating branches:

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `feat/{ticket}-description` | `feat/WTH-123-user-login` |
| Bugfix | `fix/{ticket}-description` | `fix/WTH-456-token-expiry` |
| Refactor | `refactor/{ticket}-description` | `refactor/WTH-789-cleanup` |
| Hotfix | `hotfix/description` | `hotfix/critical-auth-bug` |
| Release | `release/version` | `release/v1.2.0` |

### Step 3: Sync Policy

Sync shared branches with merge:

```bash
git merge origin/{target-branch}
```

Do not rebase shared branch history.

### Step 4: Pre-commit Checks

Before committing, run the checks appropriate to the changed files:

```bash
./gradlew ktlintFormat
./gradlew test
```

Also review changed files and check for sensitive data such as `.env`, credentials, keys, or production/dev application config.

### Step 5: Commit Message

Format:

```text
type: 한글 메시지
```

Rules:

- Type is lowercase English.
- Message is Korean by default for this repository, 50 characters or fewer when practical.
- No trailing period.
- Reference issues when applicable, e.g. `fix: 로그인 오류 수정 (#123)`.

Allowed types: `feat`, `fix`, `refactor`, `test`, `docs`, `style`, `chore`, `perf`, `ci`, `build`.
