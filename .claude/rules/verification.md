# Verification Rules

Creator-evaluator separation: the session that implements never grades its own work.

## Role Contract

1. **Task file first**: for work at the scale of a new feature, domain logic change,
   behavior-changing refactor, or multi-file change — copy `.claude/tasks/template.md`
   to `.claude/tasks/current-task.md` and fill it in at the START of the work (planning
   stage), before implementing. Never write it retroactively.
2. **No self-PASS**: when an active task file exists, the completion report MUST quote the
   `verify-implementation` evaluation result. The builder never declares PASS itself.
3. **Clean up after evaluation**: `current-task.md` is per-task and not committed (only
   `template.md` is tracked). An *active* task file = `current-task.md` with a populated
   `## 수용 기준` section. A bare/placeholder file does not gate evaluation.

## Evaluation Gate

| Scope | Task file | Verification path |
|-------|-----------|-------------------|
| New feature / endpoint, domain logic change, behavior-changing refactor, multi-file change | Required | implement → `verify-implementation` skill (auto, before reporting done) |
| Docs/comments, config values, typo/single-line fix, test-only tweak, formatting | None | ktlint hook + compile + CI (no evaluator spawn) |

Borderline → write the task file (err toward evaluation). The user can always invoke
`/verify-implementation` manually. When skipping evaluation on borderline work, append
`{"date":"...","task":"...","tier":"skipped"}` to `.claude/metrics/eval-log.jsonl`.

## Task File Format (`.claude/tasks/current-task.md`)

```markdown
# <task title>

## 사용자 요청 (원문 인용)
> <verbatim quote — not a summary>

## 수용 기준
- [ ] <verifiable statement, e.g. "POST /api/v4/... returns 201 with CommonResponse">
- [ ] ...

## 제외 범위
- <explicitly out of scope>
```

Acceptance criteria must be checkable statements — the evaluator grades against them
literally. The task file is overwritten per task and lands in the PR diff, so human
review sees it alongside the code.
