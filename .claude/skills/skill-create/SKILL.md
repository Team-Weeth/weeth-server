---
name: skill-create
description: Create new Claude Code skills. Use when asked to "create skill", "new skill", "add skill", or when a reusable workflow pattern (3+ steps) is identified.
allowed-tools: Read, Write, Glob, Grep, Bash
---

# Skill Create

Create reusable Claude Code skills with progressive disclosure structure.

## When to Create a Skill

- Workflow repeats 3+ times
- Has clear trigger phrases
- Benefits from bundled scripts/references
- Not too project-specific

## Directory Structure

```
claude/skills/{skill-name}/
├── SKILL.md              # Required: main instructions
├── scripts/              # Optional: executable code
│   └── {script}.py
└── references/           # Optional: detailed docs
    └── {topic}.md
```

Naming: kebab-case folder, `SKILL.md` exactly (case-sensitive)

## SKILL.md Structure

```markdown
---
name: {skill-name}
description: {What it does}. Use when {trigger phrases}. Do NOT use for {negative triggers}.
allowed-tools: Read, Write, Edit, Bash
---

# {Skill Name}

## Instructions

### Step 1: {Action}
{Clear instruction with expected outcome}

### Step 2: {Action}
{Continue...}

## Examples

### Example: {Scenario}
User says: "{trigger phrase}"
Actions:
1. {step}
2. {step}
Result: {outcome}

## Troubleshooting

### Error: {Common error}
**Cause**: {Why}
**Solution**: {Fix}
```

## Key Fields

| Field | Purpose |
|-------|---------|
| `description` | Triggers auto-invoke; include user phrases |
| `allowed-tools` | Tools without permission prompt |
| `disable-model-invocation: true` | User-only (for side effects) |
| `user-invocable: false` | Claude-only (background knowledge) |
| `context: fork` | Run in subagent |

## Example

Creating a "db-migration" skill:

```markdown
---
name: db-migration
description: Create database migrations. Use when asked to "create migration", "add column", "change schema".
disable-model-invocation: true
---

# DB Migration

## Instructions

### Step 1: Generate Migration File
```bash
./gradlew generateMigration -Pname="$ARGUMENTS"
```

### Step 2: Edit Migration
Add SQL for the schema change.

### Step 3: Validate
```bash
./gradlew validateMigration
```

## Examples

### Example: Add Column
```
User: `/db-migration add-user-email`
Result: Creates `V{timestamp}__add_user_email.sql`
```

## Checklist

Before creating:
- [ ] Is this reusable? (Not one-off)
- [ ] Has clear triggers?
- [ ] 3+ steps or needs scripts?
- [ ] Similar skill exists? Check `claude/skills/`

## Reference
- [Skill Generation Guide](references/README.md)

