#!/bin/bash
# 세션 트랜스크립트(~/.claude/projects/<slug>/*.jsonl)에서 skill 호출 이력을 추출한다.
# context-update의 Skill Invocation Audit 단계에서 "실제 호출 여부"의 객관적 근거로 사용.
#
# 기본: 현재 세션($CLAUDE_CODE_SESSION_ID)만 읽는다 — 감사 대상은 지금 세션이고,
#       과거 세션까지 읽으면 컨텍스트만 낭비되기 때문.
# 사용법:
#   audit-skill-invocations.sh             # 현재 세션만
#   audit-skill-invocations.sh --last 5    # 최근 5개 세션 (과거 추세 확인용)

set -u

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"
LAST_N=0
if [ "${1:-}" = "--last" ]; then
  LAST_N="${2:?--last 뒤에 세션 개수를 지정하세요}"
fi

python3 - "$PROJECT_DIR" "$LAST_N" "${CLAUDE_CODE_SESSION_ID:-}" <<'EOF'
import json, glob, os, re, sys
from collections import Counter

project_dir, last_n, session_id = sys.argv[1], int(sys.argv[2]), sys.argv[3]

# 프로젝트 경로 → 트랜스크립트 디렉터리 슬러그 (영숫자 외 문자는 '-')
slug = re.sub(r"[^A-Za-z0-9]", "-", project_dir)
base = os.path.expanduser(f"~/.claude/projects/{slug}")

if last_n > 0:
    files = sorted(glob.glob(f"{base}/*.jsonl"), key=os.path.getmtime)[-last_n:]
else:
    current = f"{base}/{session_id}.jsonl"
    if session_id and os.path.isfile(current):
        files = [current]
    else:  # 세션 ID를 모르면 최신 파일로 폴백
        files = sorted(glob.glob(f"{base}/*.jsonl"), key=os.path.getmtime)[-1:]

if not files:
    print(f"트랜스크립트 없음: {base}")
    sys.exit(0)

cmd_pattern = re.compile(r"<command-name>/?([\w:-]+)</command-name>")

for f in files:
    model_skills, user_commands = Counter(), Counter()
    for line in open(f):
        try:
            d = json.loads(line)
        except (json.JSONDecodeError, UnicodeDecodeError):
            continue
        content = (d.get("message") or {}).get("content")
        texts = []
        if isinstance(content, list):
            for b in content:
                if not isinstance(b, dict):
                    continue
                if b.get("type") == "tool_use" and b.get("name") == "Skill":
                    model_skills[b.get("input", {}).get("skill", "?")] += 1
                elif b.get("type") == "text":
                    texts.append(b.get("text", ""))
        elif isinstance(content, str):
            texts.append(content)
        # 로컬 명령(/context 등)은 type=system, subtype=local_command 로 최상위 content에 기록됨
        if d.get("type") == "system" and d.get("subtype") == "local_command":
            texts.append(d.get("content") or "")
        if d.get("type") in ("user", "system"):
            for t in texts:
                for m in cmd_pattern.findall(t):
                    user_commands[m] += 1

    print(f"## {os.path.basename(f)}")
    print(f"  모델 Skill 호출: {dict(model_skills) or '없음'}")
    print(f"  사용자 /명령:   {dict(user_commands) or '없음'}")
EOF
