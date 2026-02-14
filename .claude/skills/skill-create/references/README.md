# Claude Code Skills Engineering Guide

## 1. Core Concepts

A **skill** is a set of instructions - packaged as a simple folder - that teaches Claude how to handle specific tasks or workflows. Skills let you teach Claude once and benefit every time, instead of re-explaining your preferences, processes, and domain expertise in every conversation.

### Why Use Skills?

| Benefit | Description |
|---------|-------------|
| **Consistent Workflows** | Teach Claude specific workflows once, apply them consistently |
| **Context Efficiency** | Progressive disclosure minimizes token usage while maintaining expertise |
| **Composability** | Claude can load multiple skills simultaneously |
| **Portability** | Skills work across Claude.ai, Claude Code, and API |
| **Reusability** | Share skills across projects and teams |

### The MCP + Skills Relationship

| MCP (Connectivity) | Skills (Knowledge) |
|-------------------|-------------------|
| Connects Claude to your service | Teaches Claude how to use your service effectively |
| Provides real-time data access and tool invocation | Captures workflows and best practices |
| **What Claude can do** | **How Claude should do it** |

**Kitchen Analogy**: MCP provides the professional kitchen (access to tools, ingredients, equipment). Skills provide the recipes (step-by-step instructions on how to create something valuable).

---

## 2. Progressive Disclosure (3-Level System)

Skills use a three-level system to maximize context efficiency:

| Level | Component | When Loaded | Purpose |
|-------|-----------|-------------|---------|
| **Level 1** | YAML Frontmatter | Always in system prompt | Just enough info for Claude to know when to use the skill |
| **Level 2** | SKILL.md Body | When skill is invoked | Full instructions and guidance |
| **Level 3** | Linked Files | On-demand during execution | Reference docs Claude navigates as needed |

This progressive disclosure minimizes token usage while maintaining specialized expertise.

---

## 3. Skill Locations & Scopes

Where you store a skill determines who can use it. When skills share the same name, higher-priority locations win.

| Priority | Scope | Location | Use Case |
|----------|-------|----------|----------|
| 1 (Highest) | Enterprise | Managed settings | All users in organization |
| 2 | Personal | `~/.claude/skills/<skill-name>/SKILL.md` | All your projects |
| 3 | Project | `.claude/skills/<skill-name>/SKILL.md` | **[Recommended]** This project only |
| 4 (Lowest) | Plugin | `<plugin>/skills/<skill-name>/SKILL.md` | Where plugin is enabled |

> **Note**: `.claude/commands/` files still work and create `/command-name` shortcuts. Skills take precedence if both exist with the same name.

---

## 4. Directory Structure

```
your-skill-name/              # Required: kebab-case (no spaces, no capitals)
├── SKILL.md                  # Required: exact spelling, case-sensitive
├── scripts/                  # Optional: executable code
│   ├── validate.sh
│   └── process_data.py
├── references/               # Optional: documentation loaded as needed
│   ├── api-guide.md
│   └── examples.md
└── assets/                   # Optional: templates, icons for output
    └── report-template.md
```

### Critical Rules

| Rule | Correct | Incorrect |
|------|---------|-----------|
| Skill folder naming | `notion-project-setup` | `Notion Project Setup`, `notion_project_setup` |
| Main file naming | `SKILL.md` | `skill.md`, `SKILL.MD`, `Skill.md` |
| README inside skill | Do NOT include | - |

> **Warning**: Do NOT include `README.md` inside your skill folder. All documentation goes in `SKILL.md` or `references/`. (Repo-level README for human visitors is separate.)

---

## 5. Frontmatter Configuration

### Required Fields

```yaml
---
name: your-skill-name
description: What it does and when to use it. Include specific trigger phrases.
---
```

### All Supported Fields

| Field | Required | Description |
|-------|----------|-------------|
| `name` | Recommended | Unique identifier (kebab-case, max 64 chars). Defaults to directory name |
| `description` | Recommended | What the skill does and when to use it (max 1024 chars) |
| `argument-hint` | No | Hint shown during autocomplete. Example: `[issue-number]` |
| `disable-model-invocation` | No | Set `true` to prevent Claude from auto-loading (manual `/name` only) |
| `user-invocable` | No | Set `false` to hide from `/` menu (Claude-only invocation) |
| `allowed-tools` | No | Tools Claude can use without permission when skill is active |
| `model` | No | Model to use when skill is active |
| `context` | No | Set to `fork` to run in a forked subagent context |
| `agent` | No | Which subagent type to use when `context: fork` is set |
| `hooks` | No | Hooks scoped to this skill's lifecycle |
| `license` | No | License for open-source skills (e.g., MIT, Apache-2.0) |
| `compatibility` | No | Environment requirements (1-500 chars) |
| `metadata` | No | Custom key-value pairs (author, version, mcp-server, etc.) |

### Invocation Control

| Configuration | You Can Invoke | Claude Can Invoke | Use Case |
|--------------|----------------|-------------------|----------|
| (default) | Yes | Yes | General-purpose skills |
| `disable-model-invocation: true` | Yes | No | Side-effect workflows (`/deploy`, `/commit`) |
| `user-invocable: false` | No | Yes | Background knowledge, domain context |

### Security Restrictions

**Forbidden in frontmatter:**
- XML angle brackets (`<` `>`) - security restriction
- Skills named with "claude" or "anthropic" prefix (reserved)

**Why**: Frontmatter appears in Claude's system prompt. Malicious content could inject instructions.

---

## 6. Writing Effective Skills

### Description Field Structure

```
[What it does] + [When to use it] + [Key capabilities]
```

**Good Examples:**

```yaml
# Specific and actionable
description: Analyzes Figma design files and generates developer handoff documentation. Use when user uploads .fig files, asks for "design specs", "component documentation", or "design-to-code handoff".

# Includes trigger phrases
description: Manages Linear project workflows including sprint planning, task creation, and status tracking. Use when user mentions "sprint", "Linear tasks", "project planning", or asks to "create tickets".

# Includes negative triggers
description: Advanced data analysis for CSV files. Use for statistical modeling, regression, clustering. Do NOT use for simple data exploration (use data-viz skill instead).
```

**Bad Examples:**

```yaml
# Too vague
description: Helps with projects.

# Missing triggers
description: Creates sophisticated multi-page documentation systems.

# Too technical, no user triggers
description: Implements the Project entity model with hierarchical relationships.
```

### Main Instructions Structure

```yaml
---
name: your-skill
description: [What + When + Capabilities]
---

# Your Skill Name

## Instructions

### Step 1: [First Major Step]
Clear explanation of what happens.

Example:
```bash
python scripts/fetch_data.py --project-id PROJECT_ID
```
Expected output: [describe what success looks like]

## Examples

### Example 1: [Common scenario]
User says: "Set up a new marketing campaign"
Actions:
1. Fetch existing campaigns via MCP
2. Create new campaign with provided parameters
Result: Campaign created with confirmation link

## Troubleshooting

### Error: [Common error message]
**Cause**: [Why it happens]
**Solution**: [How to fix]
```

### Best Practices for Instructions

| Practice | Good | Bad |
|----------|------|-----|
| Be specific | `Run python scripts/validate.py --input {filename}` | `Validate the data` |
| Include error handling | List specific errors with causes and solutions | Assume everything works |
| Reference bundled resources | `Consult references/api-patterns.md for rate limiting guidance` | Inline everything |
| Use progressive disclosure | Keep SKILL.md focused, link to `references/` | Put all docs in SKILL.md |

---

## 7. String Substitutions

Skills support dynamic value substitution:

| Variable | Description |
|----------|-------------|
| `$ARGUMENTS` | All arguments passed when invoking the skill |
| `$ARGUMENTS[N]` or `$N` | Access specific argument by 0-based index (`$0`, `$1`, etc.) |
| `${CLAUDE_SESSION_ID}` | Current session ID for logging or session-specific files |

**Example:**

```yaml
---
name: fix-issue
description: Fix a GitHub issue
disable-model-invocation: true
---

Fix GitHub issue $ARGUMENTS following our coding standards.
1. Read the issue description
2. Implement the fix
3. Write tests
4. Create a commit
```

Invoking `/fix-issue 123` replaces `$ARGUMENTS` with `123`.

---

## 8. Dynamic Context Injection

The `` !`command` `` syntax runs shell commands before skill content is sent to Claude:

```yaml
---
name: pr-summary
description: Summarize changes in a pull request
context: fork
agent: Explore
---

## Pull request context
- PR diff: !`gh pr diff`
- PR comments: !`gh pr view --comments`
- Changed files: !`gh pr diff --name-only`

## Your task
Summarize this pull request...
```

Each `` !`command` `` executes immediately, and the output replaces the placeholder. Claude only sees the final result.

---

## 9. Running Skills in Subagents

Add `context: fork` to run a skill in an isolated subagent context:

```yaml
---
name: deep-research
description: Research a topic thoroughly
context: fork
agent: Explore
---

Research $ARGUMENTS thoroughly:
1. Find relevant files using Glob and Grep
2. Read and analyze the code
3. Summarize findings with specific file references
```

| Approach | System Prompt | Task | Also Loads |
|----------|---------------|------|------------|
| Skill with `context: fork` | From agent type (Explore, Plan, etc.) | SKILL.md content | CLAUDE.md |
| Subagent with `skills` field | Subagent's markdown body | Claude's delegation message | Preloaded skills + CLAUDE.md |

> **Warning**: `context: fork` only makes sense for skills with explicit instructions. Guidelines without a task will return without meaningful output.

---

## 10. Skill Design Patterns

### Pattern 1: Sequential Workflow Orchestration

**Use when**: Multi-step processes in a specific order.

```markdown
## Workflow: Onboard New Customer

### Step 1: Create Account
Call MCP tool: `create_customer`
Parameters: name, email, company

### Step 2: Setup Payment
Call MCP tool: `setup_payment_method`
Wait for: payment method verification

### Step 3: Create Subscription
Call MCP tool: `create_subscription`
Parameters: plan_id, customer_id (from Step 1)
```

### Pattern 2: Multi-MCP Coordination

**Use when**: Workflows span multiple services.

```markdown
### Phase 1: Design Export (Figma MCP)
1. Export design assets from Figma
2. Generate design specifications

### Phase 2: Task Creation (Linear MCP)
1. Create development tasks
2. Attach asset links to tasks

### Phase 3: Notification (Slack MCP)
1. Post handoff summary to #engineering
```

### Pattern 3: Iterative Refinement

**Use when**: Output quality improves with iteration.

```markdown
### Initial Draft
1. Fetch data via MCP
2. Generate first draft report

### Quality Check
1. Run validation script: `scripts/check_report.py`
2. Identify issues

### Refinement Loop
1. Address each identified issue
2. Re-validate
3. Repeat until quality threshold met
```

### Pattern 4: Context-Aware Tool Selection

**Use when**: Same outcome, different tools depending on context.

```markdown
### Decision Tree
1. Check file type and size
2. Determine best storage location:
   - Large files (>10MB): Use cloud storage MCP
   - Collaborative docs: Use Notion/Docs MCP
   - Code files: Use GitHub MCP
```

### Pattern 5: Domain-Specific Intelligence

**Use when**: Your skill adds specialized knowledge beyond tool access.

```markdown
### Before Processing (Compliance Check)
1. Fetch transaction details via MCP
2. Apply compliance rules:
   - Check sanctions lists
   - Verify jurisdiction allowances
3. Document compliance decision

### Processing
IF compliance passed:
  - Process transaction
ELSE:
  - Flag for review
```

---

## 11. Skill Use Case Categories

### Category 1: Document & Asset Creation

Creating consistent, high-quality output (documents, presentations, code, designs).

**Key techniques:**
- Embedded style guides and brand standards
- Template structures for consistent output
- Quality checklists before finalizing
- No external tools required

### Category 2: Workflow Automation

Multi-step processes that benefit from consistent methodology.

**Key techniques:**
- Step-by-step workflow with validation gates
- Templates for common structures
- Built-in review and improvement suggestions
- Iterative refinement loops

### Category 3: MCP Enhancement

Workflow guidance to enhance MCP server tool access.

**Key techniques:**
- Coordinates multiple MCP calls in sequence
- Embeds domain expertise
- Provides context users would otherwise need to specify
- Error handling for common MCP issues

---

## 12. Testing & Iteration

### Testing Approach

#### 1. Triggering Tests

| Test | Expected |
|------|----------|
| Obvious tasks | Skill triggers |
| Paraphrased requests | Skill triggers |
| Unrelated topics | Skill does NOT trigger |

**Example test suite:**

```
Should trigger:
- "Help me set up a new ProjectHub workspace"
- "I need to create a project in ProjectHub"

Should NOT trigger:
- "What's the weather in San Francisco?"
- "Help me write Python code"
```

#### 2. Functional Tests

- Valid outputs generated
- API calls succeed
- Error handling works
- Edge cases covered

#### 3. Performance Comparison

```
Without skill:
- 15 back-and-forth messages
- 3 failed API calls requiring retry
- 12,000 tokens consumed

With skill:
- 2 clarifying questions only
- 0 failed API calls
- 6,000 tokens consumed
```

### Iteration Based on Feedback

| Signal | Symptom | Solution |
|--------|---------|----------|
| **Under-triggering** | Skill doesn't load when it should | Add more detail and trigger keywords to description |
| **Over-triggering** | Skill loads for irrelevant queries | Add negative triggers, be more specific |
| **Execution issues** | Inconsistent results, user corrections needed | Improve instructions, add error handling |

---

## 13. Troubleshooting

### Skill Won't Upload

| Error | Cause | Solution |
|-------|-------|----------|
| "Could not find SKILL.md" | File not named exactly `SKILL.md` | Rename to `SKILL.md` (case-sensitive) |
| "Invalid frontmatter" | YAML formatting issue | Check `---` delimiters, unclosed quotes |
| "Invalid skill name" | Name has spaces or capitals | Use kebab-case: `my-cool-skill` |

### Skill Doesn't Trigger

**Quick checklist:**
- Is description too generic? ("Helps with projects" won't work)
- Does it include trigger phrases users would actually say?
- Does it mention relevant file types if applicable?

**Debugging**: Ask Claude "When would you use the [skill name] skill?" to see how it interprets the description.

### Skill Triggers Too Often

1. Add negative triggers
2. Be more specific in description
3. Clarify scope with service/domain names

### Instructions Not Followed

| Cause | Solution |
|-------|----------|
| Instructions too verbose | Use bullet points, move details to `references/` |
| Instructions buried | Put critical instructions at top, use `## Important` headers |
| Ambiguous language | Be specific: "Verify project name is non-empty" not "validate things" |

**Pro tip**: For critical validations, bundle a script that performs checks programmatically. Code is deterministic; language interpretation isn't.

### Large Context Issues

**Symptoms**: Skill seems slow or responses degraded

**Solutions:**
1. Keep `SKILL.md` under 500 lines (ideally under 5,000 words)
2. Move detailed docs to `references/`
3. Reduce number of enabled skills (evaluate if >20-50 enabled simultaneously)

---

## 14. Template Examples

### Code Reviewer (Read-Only)

```yaml
---
name: code-reviewer
description: Reviews code changes or PRs for quality, security, and best practices. Use when asked to "review code", "check this PR", or "find security issues". Do NOT use for simple code explanation or summary requests.
allowed-tools: Read, Grep, Glob, Bash
---

# Code Reviewer

## Core Instructions
When reviewing code, follow this procedure:

1. **Pre-validation**: Run `scripts/validate-syntax.sh` to check basic syntax errors
2. **Security Analysis**: Use `grep` to identify SQL Injection and XSS patterns
3. **Detailed Review**: Analyze code according to the criteria below

## Review Criteria
For detailed guidelines, see:
- Security checklist: [references/security-checklist.md](references/security-checklist.md)
- Performance patterns: [references/performance-patterns.md](references/performance-patterns.md)

## Output Format
Use the template in `assets/review-template.md` for consistent review output.

## Troubleshooting
- If `scripts/validate-syntax.sh` fails, stop review and output error log
- For file encoding errors, try conversion with `iconv`
```

### Deploy Workflow (User-Only Invocation)

```yaml
---
name: deploy
description: Deploy the application to production
context: fork
disable-model-invocation: true
---

Deploy $ARGUMENTS to production:

1. Run the test suite
2. Build the application
3. Push to the deployment target
4. Verify the deployment succeeded
```

### API Conventions (Claude-Only Background Knowledge)

```yaml
---
name: api-conventions
description: API design patterns for this codebase
user-invocable: false
---

When writing API endpoints:
- Use RESTful naming conventions
- Return consistent error formats
- Include request validation
- Follow pagination patterns in references/api-guide.md
```

---

## 15. Quick Checklist

### Before You Start
- [ ] Identified 2-3 concrete use cases
- [ ] Tools identified (built-in or MCP)
- [ ] Planned folder structure

### During Development
- [ ] Folder named in kebab-case
- [ ] `SKILL.md` file exists (exact spelling)
- [ ] YAML frontmatter has `---` delimiters
- [ ] `name` field: kebab-case, no spaces, no capitals
- [ ] `description` includes WHAT and WHEN
- [ ] No XML tags (`<` `>`) anywhere
- [ ] Instructions are clear and actionable
- [ ] Error handling included
- [ ] References clearly linked

### Before Upload
- [ ] Tested triggering on obvious tasks
- [ ] Tested triggering on paraphrased requests
- [ ] Verified doesn't trigger on unrelated topics
- [ ] Functional tests pass

### After Upload
- [ ] Test in real conversations
- [ ] Monitor for under/over-triggering
- [ ] Iterate on description and instructions

---

## References

- [Official Claude Code Skills Documentation](https://code.claude.com/docs/en/skills)
- [The Complete Guide to Building Skills for Claude (PDF)](https://claude.com/blog/complete-guide-to-building-skills-for-claude)
- [Agent Skills Open Standard](https://agentskills.io)
- [Claude Code Subagents Documentation](https://code.claude.com/docs/en/sub-agents)
- [Claude Code Hooks Documentation](https://code.claude.com/docs/en/hooks)
- [Example Skills Repository](https://github.com/anthropics/skills)