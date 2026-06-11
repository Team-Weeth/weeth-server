# Git Conventions Rules

## Commit Convention

Format: `type: message` — type is lowercase English; message is imperative mood, ≤50 chars, no trailing period. Reference issues when applicable: `fix: Resolve login bug (#123)`.

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `style`, `chore`, `perf`, `ci`, `build`

## Branch Convention

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `feat/{ticket}-description` | `feat/WTH-123-user-login` |
| Bugfix | `fix/{ticket}-description` | `fix/WTH-456-token-expiry` |
| Refactor | `refactor/{ticket}-description` | `refactor/WTH-789-cleanup` |
| Hotfix | `hotfix/description` | `hotfix/critical-auth-bug` |
| Release | `release/version` | `release/v1.2.0` |

## Branch Update Policy

- Sync local branches with **merge** (`git merge origin/{target-branch}`), not rebase — do not rewrite shared branch history.

## Pre-commit Checklist

1. `./gradlew ktlintFormat`
2. `./gradlew test`
3. Verify commit message format and review changed files
4. Check for sensitive data (.env, credentials)
