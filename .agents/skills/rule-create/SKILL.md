---
name: rule-create
description: Create or update Codex rules and conventions. Use when asked to "create rule", "add convention", "document pattern", or when a coding standard needs to be captured.
---

# Rule Create

Create modular project convention documents for coding standards and patterns.

Do not confuse these documents with Codex command permission rules. Codex command permission rules use `.rules` files under `.codex/rules/`; this skill writes human-readable project guidance under `.agents/rules/`.

## When to Create a Rule

- Convention discovered that should be consistent
- Pattern clarified during debugging
- Team standard needs documentation
- Gap found in existing rules

## File Location

```
.agents/rules/{topic}.md           # Project rules
~/.agents/rules/{topic}.md         # Personal convention docs, only when explicitly requested
```

## Rule Structure

```markdown
---
paths:
  - "src/**/*.{ts,tsx}"
  - "lib/**/*.ts"
---

# {Topic} Rules

## {Category}

### Pattern
{The convention or pattern}

### Rationale
{Why this matters}

### Example
```{lang}
// Good
{correct example}

// Bad
{incorrect example}
```
```

## Path Scoping

Use frontmatter to scope rules to specific files:

| Pattern | Matches |
|---------|---------|
| `**/*.ts` | All TypeScript files |
| `src/api/**/*` | All files under src/api |
| `*.md` | Markdown in root only |
| `{src,lib}/**/*.ts` | TS in both directories |

## Example

Creating an `error-handling.md` rule:

```markdown
---
paths:
  - "src/**/*.ts"
---

# Error Handling Rules

## Custom Exceptions

### Pattern
All domain exceptions extend `BaseException` with error code.

### Rationale
Consistent error responses and logging.

### Example
```kotlin
// Good
class UserNotFoundException(
    override val errorCode: ErrorCode = ErrorCode.USER_NOT_FOUND
) : BaseException()

// Bad
class UserNotFoundException : RuntimeException("User not found")
```

## API Error Response

### Pattern
Always return structured error format.

### Example
```json
{
  "success": false,
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "User with id 123 not found"
  }
}
```


## Update vs Create

**Create new file when**:
- Topic not covered by existing rules
- Would make existing file too long

**Update existing file when**:
- Adding to existing topic
- Clarifying existing pattern

## Checklist

Before creating:
- [ ] Is this a repeatable convention?
- [ ] Will it help consistency?
- [ ] Similar rule exists? Check `.agents/rules/`
- [ ] Appropriate scope (project vs personal)?

## Reference: 

Use `AGENTS.md` for always-on repository instructions. Use `.agents/rules/*.md` for longer project convention documents referenced by `AGENTS.md` or project skills.
