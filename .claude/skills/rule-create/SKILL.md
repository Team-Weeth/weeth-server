---
name: rule-create
description: Create or update Claude Code rules and conventions. Use when asked to "create rule", "add convention", "document pattern", or when a coding standard needs to be captured.
allowed-tools: Read, Write, Edit, Glob, Grep
---

# Rule Create

Create modular rules for coding conventions and patterns.

## When to Create a Rule

- Convention discovered that should be consistent
- Pattern clarified during debugging
- Team standard needs documentation
- Gap found in existing rules

## File Location

```
.claude/rules/{topic}.md           # Project rules
~/.claude/rules/{topic}.md         # Personal rules
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
- [ ] Similar rule exists? Check `.claude/rules/`
- [ ] Appropriate scope (project vs personal)?

## Reference: 
- [Rule Generation Guide](references/README.md)
