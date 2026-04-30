#!/bin/bash
# PostToolUse hook: .kt 파일 수정 시 ktlint 자동 포맷

FILE_PATH=$(cat | jq -r '.tool_input.file_path // empty')

if [ -z "$FILE_PATH" ]; then
  exit 0
fi

# .kt 파일만 처리
if [[ "$FILE_PATH" != *.kt ]]; then
  exit 0
fi

cd "$CLAUDE_PROJECT_DIR" || exit 0
./gradlew ktlintFormat 2>&1 >&2
