# Claude Code Rules & Memory (CLAUDE.md) Guide

## 1. Core Concepts

**Memory** in Claude Code refers to persistent instructions that Claude remembers across sessions. The primary mechanism is **CLAUDE.md** files - markdown files that contain project context, coding standards, and workflow instructions.

### Why Use Memory?

| Benefit | Description |
|---------|-------------|
| **Consistency** | Same instructions applied every session |
| **Context Efficiency** | Avoid re-explaining preferences repeatedly |
| **Team Alignment** | Share coding standards via version control |
| **Hierarchical Control** | Different scopes for different needs |

---

## 2. Memory Locations

Claude Code offers a hierarchical memory structure. Files higher in the hierarchy take precedence:

| Priority | Memory Type | Location | Purpose | Shared With |
|----------|-------------|----------|---------|-------------|
| 1 | **Managed Policy** | `/Library/Application Support/ClaudeCode/CLAUDE.md` (macOS) | Organization-wide instructions | All users in org |
| 2 | **Project Memory** | `./CLAUDE.md` or `./.claude/CLAUDE.md` | Team-shared project instructions | Team via source control |
| 3 | **Project Rules** | `./.claude/rules/*.md` | Modular, topic-specific instructions | Team via source control |
| 4 | **User Memory** | `~/.claude/CLAUDE.md` | Personal preferences for all projects | Just you (all projects) |
| 5 | **Project Local** | `./CLAUDE.local.md` | Personal project-specific preferences | Just you (current project) |

**Platform-specific Managed Policy locations:**
- macOS: `/Library/Application Support/ClaudeCode/CLAUDE.md`
- Linux: `/etc/claude-code/CLAUDE.md`
- Windows: `C:\Program Files\ClaudeCode\CLAUDE.md`

> **Note**: `CLAUDE.local.md` files are automatically added to `.gitignore`, ideal for private preferences.

---

## 3. How Memory Loads

Claude Code reads memory **recursively** from the current directory up to (but not including) root `/`:

```
Working in: /project/src/components/
Loads:
  1. /project/CLAUDE.md
  2. /project/src/CLAUDE.md (if exists)
  3. /project/src/components/CLAUDE.md (if exists)
```

**Nested memory discovery**: Claude also discovers CLAUDE.md files in subdirectories, but only loads them when Claude reads files in those subtrees.

### Load from Additional Directories

```bash
# Include memory from additional directories
CLAUDE_CODE_ADDITIONAL_DIRECTORIES_CLAUDE_MD=1 claude --add-dir ../shared-config
```

---

## 4. CLAUDE.md Structure

### Recommended Template

```markdown
# Project Context & Rules

## Architecture Pattern
- Next.js App Router with TypeScript
- State management: Zustand (no Redux)
- Testing: Jest + React Testing Library

## Coding Standards
- File naming: kebab-case
- All async functions require error handling
- Use named exports, not default exports

## Frequently Used Commands
- Build: `npm run build`
- Test: `npm test`
- Lint: `npm run lint`
- Dev server: `npm run dev`

## Project-Specific Context
- API base URL: https://api.example.com
- Database: PostgreSQL with Prisma ORM

## Memory Bank (External Links)
- API spec: @docs/api-spec.md
- Database schema: @docs/schema.md
- Style guide: @.claude/rules/code-style.md
```

### Import Syntax

Use `@path/to/file` to import additional files:

```markdown
See @README for project overview and @package.json for npm commands.

# Additional Instructions
- Git workflow: @docs/git-instructions.md
- API patterns: @.claude/rules/api-design.md
```

**Import rules:**
- Both relative and absolute paths allowed
- Relative paths resolve from the importing file's location
- Max recursion depth: 5 hops
- Imports inside code blocks are ignored
- First import requires approval dialog (one-time per project)

### Home Directory Imports

For multi-worktree setups, use home directory imports:

```markdown
# Individual Preferences
@~/.claude/my-project-instructions.md
```

---

## 5. Modular Rules with `.claude/rules/`

For larger projects, organize instructions into multiple files:

```
your-project/
├── .claude/
│   ├── CLAUDE.md              # Main project instructions
│   └── rules/
│       ├── code-style.md      # Code style guidelines
│       ├── testing.md         # Testing conventions
│       ├── api-design.md      # API patterns
│       ├── security.md        # Security requirements
│       └── frontend/          # Subdirectory organization
│           ├── react.md
│           └── styles.md
```

All `.md` files in `.claude/rules/` are automatically loaded with same priority as `.claude/CLAUDE.md`.

### Path-Specific Rules

Use YAML frontmatter to scope rules to specific files:

```markdown
---
paths:
  - "src/api/**/*.ts"
  - "lib/api/**/*.ts"
---

# API Development Rules

- All API endpoints must include input validation
- Use the standard error response format
- Include OpenAPI documentation comments
```

### Glob Patterns

| Pattern | Matches |
|---------|---------|
| `**/*.ts` | All TypeScript files in any directory |
| `src/**/*` | All files under `src/` |
| `*.md` | Markdown files in project root |
| `src/components/*.tsx` | React components in specific directory |
| `src/**/*.{ts,tsx}` | Both `.ts` and `.tsx` files (brace expansion) |
| `{src,lib}/**/*.ts` | TypeScript in both `src` and `lib` |

### User-Level Rules

Personal rules that apply to all projects:

```
~/.claude/rules/
├── preferences.md    # Your coding preferences
└── workflows.md      # Your preferred workflows
```

User-level rules load before project rules (lower priority).

### Symlinks

`.claude/rules/` supports symlinks for sharing rules:

```bash
# Symlink shared rules directory
ln -s ~/shared-claude-rules .claude/rules/shared

# Symlink individual files
ln -s ~/company-standards/security.md .claude/rules/security.md
```

---

## 6. Commands

### `/memory`

Open any memory file in your system editor:

```
/memory
```

Shows list of all loaded memory files for selection.

### `/init`

Bootstrap a CLAUDE.md for your codebase:

```
/init
```

Analyzes your project and creates initial CLAUDE.md with:
- Detected tech stack
- Common commands
- Project structure

---

## 7. Template Examples

### Minimal Project CLAUDE.md

```markdown
# Project: My App

## Stack
- TypeScript, React, Node.js

## Commands
- `npm run dev` - Start dev server
- `npm test` - Run tests
- `npm run build` - Production build

## Standards
- Use functional components with hooks
- All components in `src/components/`
- API calls via `src/api/` layer
```

### Code Style Rules (.claude/rules/code-style.md)

```markdown
---
paths:
  - "**/*.{ts,tsx,js,jsx}"
---

# Code Style Guidelines

## Naming Conventions
- Files: kebab-case (`user-profile.tsx`)
- Components: PascalCase (`UserProfile`)
- Functions: camelCase (`getUserData`)
- Constants: SCREAMING_SNAKE_CASE (`MAX_RETRY_COUNT`)

## Formatting
- 2-space indentation
- Single quotes for strings
- No semicolons (Prettier handles this)
- Max line length: 100 characters

## Imports
- Group: external libs → internal modules → relative imports
- Alphabetize within groups
- Use named exports
```

### Testing Rules (.claude/rules/testing.md)

```markdown
---
paths:
  - "**/*.test.{ts,tsx}"
  - "**/*.spec.{ts,tsx}"
  - "__tests__/**/*"
---

# Testing Standards

## Structure
- Use describe/it blocks with clear descriptions
- Follow Arrange-Act-Assert pattern
- One assertion per test when possible

## Naming
- Test files: `[component].test.tsx` or `[component].spec.tsx`
- Describe blocks: component/function name
- It blocks: "should [expected behavior] when [condition]"

## Mocking
- Mock external dependencies, not internal modules
- Use `jest.mock()` at top of file
- Reset mocks in `beforeEach`
```

### API Design Rules (.claude/rules/api-design.md)

```markdown
---
paths:
  - "src/api/**/*"
  - "src/routes/**/*"
---

# API Design Guidelines

## REST Conventions
- Use plural nouns for resources (`/users`, not `/user`)
- Use HTTP verbs correctly (GET, POST, PUT, DELETE)
- Return appropriate status codes

## Response Format
```json
{
  "success": true,
  "data": {},
  "error": null
}
```

## Error Handling
- Always return structured error responses
- Include error code, message, and details
- Log errors server-side with context
```

---

## 8. Best Practices

| Practice | Description |
|----------|-------------|
| **Be Specific** | "Use 2-space indentation" > "Format code properly" |
| **Use Structure** | Bullet points + markdown headings for organization |
| **Modularize** | Split large CLAUDE.md into `.claude/rules/` files |
| **Review Periodically** | Update as project evolves to prevent context drift |
| **Scope Appropriately** | Use path-specific rules only when truly needed |
| **Keep Focused** | Each rule file should cover one topic |
| **Use Descriptive Names** | Filename should indicate what rules cover |

### What to Include

- Frequently used commands (build, test, lint)
- Code style preferences and naming conventions
- Architectural patterns and decisions
- Project-specific context (API endpoints, database info)
- Team conventions and workflows

### What NOT to Include

- Sensitive information (API keys, passwords)
- Highly volatile information that changes frequently
- Information already in README or other docs (use imports instead)

---

## 9. Organization-Wide Memory

Deploy centrally managed CLAUDE.md via:

1. Create file at Managed Policy location
2. Deploy via configuration management (MDM, Group Policy, Ansible)
3. All users automatically receive organization standards

---

## References

- [Official Claude Code Memory Documentation](https://code.claude.com/docs/en/memory)
- [Claude Code Settings Documentation](https://code.claude.com/docs/en/settings)