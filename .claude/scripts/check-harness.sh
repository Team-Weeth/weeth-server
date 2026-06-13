#!/bin/bash
# AI 하네스 무결성 검사
# rules가 skill로, skill이 references로 포인터를 두는 점진적 공개 구조에서는
# 파일 리네임/삭제 시 포인터가 조용히 썩는다. 이 스크립트가 그 드리프트를 잡는다.
#
# 검사 항목:
#   1. .agents/rules, .agents/skills 심볼릭 링크가 유효한지
#   2. 모든 SKILL.md frontmatter에 name/description이 있고 name == 디렉터리명인지
#   3. rules/agents/CLAUDE.md/AGENTS.md가 참조하는 skill 이름이 실제 존재하는지
#   4. SKILL.md가 참조하는 references/*.md 파일이 실제 존재하는지

set -u
cd "$(cd "$(dirname "$0")/../.." && pwd)" || exit 1

FAIL=0

fail() {
  echo "FAIL: $1"
  FAIL=1
}

# --- 1. 심볼릭 링크 ---
for link in .agents/rules .agents/skills; do
  if [ ! -L "$link" ]; then
    fail "$link 가 심볼릭 링크가 아닙니다"
  elif [ ! -e "$link" ]; then
    fail "$link 심볼릭 링크가 깨졌습니다 → $(readlink "$link")"
  fi
done

# --- 2. SKILL.md frontmatter ---
for skill_md in .claude/skills/*/SKILL.md; do
  dir=$(basename "$(dirname "$skill_md")")
  frontmatter=$(awk '/^---$/{c++; next} c==1{print} c>=2{exit}' "$skill_md")
  name=$(printf '%s\n' "$frontmatter" | sed -n 's/^name:[[:space:]]*//p' | head -1)
  description=$(printf '%s\n' "$frontmatter" | sed -n 's/^description:[[:space:]]*//p' | head -1)

  [ -z "$name" ] && fail "$skill_md frontmatter에 name이 없습니다"
  [ -z "$description" ] && fail "$skill_md frontmatter에 description이 없습니다"
  if [ -n "$name" ] && [ "$name" != "$dir" ]; then
    fail "$skill_md name '$name' 이 디렉터리명 '$dir' 과 다릅니다"
  fi
done

# --- 3. skill 이름 참조 검증 ---
# 관례: skill 참조는 "skill"이라는 단어가 있는 줄에서 백틱 kebab-case로 쓴다.
# (예: "Use the `api-contract-update` skill when ...")
scan_files=$(ls .claude/rules/*.md .claude/agents/*.md CLAUDE.md AGENTS.md 2>/dev/null)
refs=$(grep -hiE 'skill' $scan_files 2>/dev/null |
  grep -oE '`[a-z][a-z0-9]*(-[a-z0-9]+)+`' | tr -d '`' | sort -u)

for ref in $refs; do
  # 파일명(.md 등)은 4번에서 별도 검사
  case "$ref" in *.*) continue ;; esac
  if [ ! -f ".claude/skills/$ref/SKILL.md" ]; then
    fail "skill '$ref' 참조가 깨졌습니다 (.claude/skills/$ref/SKILL.md 없음) — 참조 위치: $(grep -lE "\`$ref\`" $scan_files | tr '\n' ' ')"
  fi
done

# --- 4. SKILL.md → references/*.md 링크 검증 ---
# 스크립트가 런타임에 생성하는 산출물은 커밋되지 않으므로 제외
GENERATED_REFS="database-manage/references/schema.md"

for skill_md in .claude/skills/*/SKILL.md; do
  skill_dir=$(dirname "$skill_md")
  skill_name=$(basename "$skill_dir")
  ref_files=$(grep -oE '(\]\(|`)references/[A-Za-z0-9._-]+\.(md|sh)' "$skill_md" | sed -E 's/^(\]\(|`)//' | sort -u)
  for ref_file in $ref_files; do
    case " $GENERATED_REFS " in
      *" $skill_name/$ref_file "*) continue ;;
    esac
    if [ ! -f "$skill_dir/$ref_file" ]; then
      fail "$skill_md 가 참조하는 $ref_file 이 없습니다"
    fi
  done
done

# --- 5. .claude/scripts·hooks/*.sh 참조 검증 ---
# settings.json과 에이전트 frontmatter가 참조하는 훅 스크립트도 포함
script_refs=$(grep -rhoE '\.claude/(scripts|hooks)/[A-Za-z0-9._-]+\.sh' \
  .claude/rules .claude/skills/*/SKILL.md .claude/agents .claude/settings.json \
  CLAUDE.md AGENTS.md 2>/dev/null | sort -u)
for script in $script_refs; do
  [ -f "$script" ] || fail "스크립트 참조가 깨졌습니다: $script"
  [ -x "$script" ] || fail "스크립트에 실행 권한이 없습니다: $script"
done

if [ "$FAIL" -eq 0 ]; then
  echo "OK: 하네스 무결성 검사 통과"
fi
exit "$FAIL"
