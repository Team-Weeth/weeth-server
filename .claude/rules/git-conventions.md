# Git Conventions Rules

# Commit Convention
## Format

```
type: message
```

- **type**: lowercase English
- **message**: Brief description (imperative mood)

## Types

| Type | Description | Example |
|------|-------------|---------|
| `feat` | New feature | `feat: Add user authentication` |
| `fix` | Bug fix | `fix: Resolve null pointer in service` |
| `refactor` | Code refactoring | `refactor: Extract validation logic` |
| `test` | Test code changes | `test: Add UserService unit tests` |
| `docs` | Documentation | `docs: Update API documentation` |
| `style` | Code formatting | `style: Apply code formatter` |
| `chore` | Maintenance | `chore: Update dependencies` |
| `perf` | Performance improvement | `perf: Optimize database queries` |
| `ci` | CI configuration | `ci: Add GitHub Actions workflow` |
| `build` | Build system | `build: Update Gradle config` |

## Examples

```bash
# New feature
feat: Add user registration endpoint

# Bug fix
fix: Handle null profile in user response

# Refactoring
refactor: Split UserService into Get/Save services

# Test
test: Add integration tests for auth flow

# Documentation
docs: Add API usage examples

# Style
style: Format code with ktlint

# Chore
chore: Upgrade Spring Boot to 3.2.0
```

## Rules

1. **No period** at the end
2. **Imperative mood** ("Add" not "Added", "Fix" not "Fixed")
3. **50 characters or less** for subject line
4. **Separate subject from body** with blank line if body needed
5. **Reference issue numbers** if applicable: `fix: Resolve login bug (#123)`

## Multi-line Commits

For detailed descriptions:

```bash
git commit -m "$(cat <<'EOF'
feat: Add user authentication

- Implement JWT token generation
- Add login/logout endpoints
- Create auth middleware

Closes #123
EOF
)"
```
---
# Branch Convention

| Type | Pattern | Example                      |
|------|---------|------------------------------|
| Feature | `feat/{ticket}-description` | `feat/WTH-123-user-login` |
| Bugfix | `fix/{ticket}-description` | `fix/WTH-456-token-expiry`   |
| Refactor | `refactor/{ticket}-description` | `refactor/WTH-789-cleanup`   |
| Hotfix | `hotfix/description` | `hotfix/critical-auth-bug`   |
| Release | `release/version` | `release/v1.2.0`             |

## Branch Update Policy

- Update local branches from the latest target branch using **merge**.
- Default command: `git merge origin/{target-branch}`.
- Do not rewrite shared branch history with rebase when syncing latest changes.

## Pre-commit Checklist

1. Run linter: `./gradlew ktlintFormat`
2. Run tests: `./gradlew test`
3. Verify commit message format
4. Review changed files
5. Check for sensitive data (.env, credentials)

## Conventional Commits (Optional)

For automated changelog generation:

```
type(scope): message

feat(auth): Add OAuth2 support
fix(api): Handle rate limiting
refactor(user): Simplify validation logic
```
