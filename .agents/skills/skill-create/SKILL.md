---
name: skill-create
description: Create new Codex skills. Use when asked to "create skill", "new skill", "add skill", or when a reusable workflow pattern (3+ steps) is identified.
---

# Skill Create

Create reusable Codex skills with progressive disclosure structure.

## When to Create a Skill

- Workflow repeats 3+ times
- Has clear trigger phrases
- Benefits from bundled scripts/references
- Not too project-specific

## Directory Structure

```
.agents/skills/{skill-name}/
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
| `name` | Skill identifier; use kebab-case |
| `description` | Triggers auto-invoke; include user phrases |

Keep frontmatter minimal. Codex uses `name` and `description` to decide when the skill applies.

## Example

Creating a "db-migration" skill:

```markdown
---
name: db-migration
description: Create database migrations. Use when asked to "create migration", "add column", "change schema".
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
- [ ] Similar skill exists? Check `.agents/skills/`

## Reference

Use the Codex repo-scoped skill format: `.agents/skills/{skill-name}/SKILL.md`.
Keep `SKILL.md` concise and move only task-specific details into `references/`, `scripts/`, or `assets/` when they are genuinely needed.
