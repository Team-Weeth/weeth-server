---
name: context-update
description: Self-feedback skill that analyzes completed work and improves Claude Code context. Use when asked to "update context", "capture learnings", "improve context", or before compaction. Identifies reusable patterns, audits whether skills were actually invoked when their triggers matched, and delegates to appropriate create skills.
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Context Update

Meta-skill for continuous improvement through self-reflection.

## Purpose

After completing tasks, analyze work and:
1. Identify reusable patterns
2. Audit skill invocation quality (were the right skills actually used?)
3. Find gaps in existing context
4. Delegate to appropriate create skills
5. Generate improvement report

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

### Step 2: Audit Skill Invocations

The rules layer is intentionally thin and points to skills — this only works if skills actually fire. For every task in the session, compare against the trigger descriptions in `.claude/skills/*/SKILL.md`:

```
[ ] Did the task match any skill's trigger description?
[ ] Was that skill actually invoked?
[ ] If invoked: was its workflow followed (references read, checklist applied), or loaded and ignored?
[ ] If not invoked: did the output violate any convention the skill protects?
```

Then act per finding:

| Finding | Diagnosis | Action |
|---------|-----------|--------|
| Trigger matched, skill not invoked, output still correct | Trigger phrasing too narrow, or the always-on rules already covered it | Strengthen the skill `description` trigger phrases to match how the task was actually phrased |
| Trigger matched, skill not invoked, convention violated | Pointer failure with real cost | Strengthen the `description` AND promote the violated invariant into `.claude/rules/` (always-on) via `rule-create` |
| Skill invoked but was unnecessary | Trigger too broad | Tighten the `description`; extend its "Do NOT use" boundary |
| Skill invoked but workflow skipped/ignored | Skill body unclear or too long | Simplify the workflow; move detail into `references/` |

**How to verify invocations:** the current session's conversation is the primary source — model-invoked skills appear as `Skill` tool calls, user-typed `/skill` commands appear as `<command-name>` markers. For an objective count, run:

```bash
bash .claude/scripts/audit-skill-invocations.sh           # 현재 세션만 (기본)
bash .claude/scripts/audit-skill-invocations.sh --last 5  # 과거 추세 확인 시에만
```

Default is current-session-only to keep context usage small; pull past sessions only when investigating a trend.

Note: the transcript proves *whether* a skill fired; *whether it should have fired* and *whether its workflow was followed* require reading the conversation — that judgment is this skill's job, not a script's.

Also run the harness integrity check and include the result in the report:

```bash
bash .claude/scripts/check-harness.sh
```

### Step 3: Categorize Findings

| Signal | Category | Action |
|--------|----------|--------|
| Reusable 3+ step pattern | Skill | Invoke `skill-create` |
| Convention discovered | Rule | Invoke `rule-create` |
| One-off task | None | Document only |

### Step 4: Check for Duplicates

Before delegating, search existing context:

```bash
Glob: .claude/skills/*/SKILL.md
Glob: .claude/rules/*.md
Grep: pattern="{keyword}" path=".claude/"
```

### Step 5: Delegate Creation

For each identified improvement, invoke the appropriate skill:

- **New skill needed** → Invoke `skill-create`
- **Rule update needed** → Invoke `rule-create`
- **Neither applies** → Update MEMORY.md or document in report only

### Step 6: Generate Report

```markdown
## Context Update Report

### Session Summary
- Tasks: {list of completed tasks}
- Patterns identified: {count}
- Harness integrity check: {OK / FAIL details}

### Skill Invocation Audit

| Task | Expected Skill | Invoked? | Outcome | Action Taken |
|------|---------------|----------|---------|--------------|
| {task} | {skill or none} | ✓/✗ | OK / convention violated | description 보강 / rule 승격 / 없음 |

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

## Conflict Resolution Priority

When a newly discovered pattern conflicts with existing guidance, apply this order:

1. Follow higher-priority runtime instructions (system/developer/user for the current session).
2. Prefer existing project rules in `.claude/rules/` over ad-hoc new patterns.
3. If no rule exists, follow established skill workflows in `.claude/skills/*/SKILL.md`.
4. Treat the new pattern as a candidate update, not an immediate override.
5. If conflict remains ambiguous, do not auto-apply; add it to **Manual Follow-ups** for user decision.

Implementation guidance:
- For rule conflicts, invoke `rule-create` to update/clarify the rule with rationale.
- For skill workflow conflicts, invoke `skill-create` only if the change is broadly reusable.
- Always document why the existing guidance was kept or updated in the report.

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
