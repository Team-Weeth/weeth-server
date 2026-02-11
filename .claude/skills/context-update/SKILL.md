---
name: context-update
description: Self-feedback skill that analyzes completed work and improves Claude Code context. Use when asked to "update context", "capture learnings", "improve context", or before compaction. Identifies reusable patterns and delegates to appropriate create skills.
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Context Update

Meta-skill for continuous improvement through self-reflection.

## Purpose

After completing tasks, analyze work and:
1. Identify reusable patterns
2. Find gaps in existing context
3. Delegate to appropriate create skills
4. Generate improvement report

## Workflow

### Step 1: Analyze Session

Review conversation for:

```
[ ] Tasks completed
[ ] Problems solved
[ ] Patterns repeated (3+ times = skill candidate)
[ ] External knowledge needed (gap in rules)
[ ] Friction points or mistakes
[ ] Frequently used commands
```

### Step 2: Categorize Findings

| Signal | Category | Action |
|--------|----------|--------|
| Reusable 3+ step pattern | Skill | Invoke `skill-create` |
| Convention discovered | Rule | Invoke `rule-create` |
| One-off task | None | Document only |

### Step 3: Check for Duplicates

Before delegating, search existing context:

```bash
Glob: .claude/skills/*/SKILL.md
Glob: .claude/rules/*.md
Grep: pattern="{keyword}" path=".claude/"
```

### Step 4: Delegate Creation

For each identified improvement, invoke the appropriate skill:

- **New skill needed** → Invoke `skill-create`
- **Rule update needed** → Invoke `rule-create`
- **Neither applies** → Update MEMORY.md or document in report only

### Step 5: Generate Report

```markdown
## Context Update Report

### Session Summary
- Tasks: {list of completed tasks}
- Patterns identified: {count}

### Actions Taken

| Type | Name | Action | Reason |
|------|------|--------|--------|
| Skill | {name} | Created | {why} |
| Rule | {file} | Updated | {why} |

### Skipped (No Action)

| Pattern | Reason |
|---------|--------|
| {pattern} | One-off / Too specific / Already exists |

### Manual Follow-ups
- {Any suggestions requiring user decision}
```

## Decision Criteria

### Create When:
- Pattern used 3+ times
- Would save significant time if reused
- Not too project-specific
- Clear trigger phrases exist

### Skip When:
- One-off task
- Too project-specific
- Already documented
- Requires user decision (suggest instead)

## Example Session Analysis

**Observed**: Created API endpoint 4 times with same structure.

**Analysis**:
- Repeated pattern? ✓ (4 times)
- Multi-step? ✓ (Controller, Service, DTO, tests)
- Reusable? ✓ (applies to any endpoint)

**Action**: Check if `api-create` skill exists → Already exists, no action.

---

**Observed**: Had to look up soft-delete query pattern twice.

**Analysis**:
- Caused friction? ✓
- Convention exists? Partially in entity-repository.md

**Action**: Invoke `rule-create` to update relevant rule with explicit example.

---

**Observed**: Wrote one-time data migration script.

**Analysis**:
- Repeated? ✗ (one-off)

**Action**: None - too specific.
