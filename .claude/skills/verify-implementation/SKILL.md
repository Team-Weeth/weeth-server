---
name: verify-implementation
description: Verify completed implementation work via the implementation-evaluator subagent before reporting done. Use after finishing any task that has an active task file (.claude/tasks/current-task.md with a populated 수용 기준 section), or when asked to "검증해줘"/"평가해줘". Do NOT auto-invoke for trivial work without a task file (docs, config values, typo/single-line fixes, formatting) — those are covered by ktlint hook + compile + CI.
---

# Verify Implementation

Completion gate: the builder (main session) never declares PASS on its own work.
A clean-context, read-only evaluator grades the diff against the task file.

## Step 1: Confirm an active task file

An **active** task file is `.claude/tasks/current-task.md` that exists AND has a
`## 수용 기준` section with at least one checklist item (`- [ ]` / `- [x]`). The bare
template (`.claude/tasks/template.md`) and a placeholder current-task.md with no criteria
do NOT count — they must not trigger evaluation.

- Auto-invoked path: the active task file already exists (it gated this skill).
- Manually invoked without one: copy `template.md` to `current-task.md`, fill it in
  (quote the user's request verbatim + acceptance criteria, format in
  `.claude/rules/verification.md`), then proceed.

## Step 2: Fix the diff baseline and record pre-spawn hashes

Decide the baseline (commit where this work started — usually `HEAD` if uncommitted,
or the branch point). Then record:

```bash
git diff <baseline> | shasum -a 256
git status --porcelain | shasum -a 256
```

## Step 3: Spawn the evaluator

Spawn `implementation-evaluator` via the Agent tool. Pass ONLY:

- the diff baseline (commit/branch)
- the task file path

Do NOT pass a requirements summary or any claim about what was implemented — the task
file is the sole source of requirements. On re-evaluation rounds, additionally pass the
previous findings labeled as "이전 라운드 미비점 — 고쳐졌는지 특히 확인".

## Step 4: Re-check hashes after the evaluator returns

Re-run the two hash commands from Step 2. **Any mismatch = the evaluator touched the
tree = the verdict is VOID.** Report this to the user instead of using the verdict.

## Step 5: Handle the verdict

- `PASS` → go to Step 6.
- `NEEDS_WORK` → apply the findings, then re-run from Step 2 with a **freshly spawned
  evaluator that re-grades all criteria** (fixes can regress other criteria; previous
  findings are an appendix, not the scope). Maximum 2 retries — after the 3rd
  NEEDS_WORK, stop and escalate to the user with the full findings history.

## Step 6: Report completion

Quote the evaluator's verdict and per-criterion evidence in the completion report.
Never paraphrase a NEEDS_WORK into "mostly done".

## Step 7: Append the metrics log

```bash
mkdir -p .claude/metrics
echo '{"date":"YYYY-MM-DD","task":"<task-file slug or one-line summary>","tier":"evaluated","first_verdict":"PASS|NEEDS_WORK","failed_criteria":[...],"retry_count":N}' >> .claude/metrics/eval-log.jsonl
```

This log decides later, with data instead of anecdotes: first-PASS rate (criteria too
loose/strict), escape rate (issues the evaluator missed but human PR review caught),
opus→sonnet downgrade, and whether the Stop-hook compile gate (plan phase 3) is needed.
