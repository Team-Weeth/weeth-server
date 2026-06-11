#!/bin/bash
# PostToolUse hook: .kt/.kts 파일 수정 시 해당 파일만 ktlint 포맷
# 전체 ktlintFormat은 편집마다 Gradle 기동 비용이 들고, 수정하지 않은 파일까지
# 포맷되어 diff가 오염되므로 단일 파일만 처리한다.

FILE_PATH=$(cat | jq -r '.tool_input.file_path // empty')

[ -z "$FILE_PATH" ] && exit 0

case "$FILE_PATH" in
  *.kt | *.kts) ;;
  *) exit 0 ;;
esac

[ -f "$FILE_PATH" ] || exit 0

# build.gradle.kts의 ktlint { version } 과 동일하게 유지할 것
KTLINT_VERSION="1.8.0"
CACHE_DIR="$HOME/.cache/ktlint"
KTLINT_BIN="$CACHE_DIR/ktlint-$KTLINT_VERSION"

# 최초 1회만 다운로드 (이후 캐시 사용)
if [ ! -x "$KTLINT_BIN" ]; then
  mkdir -p "$CACHE_DIR"
  curl -fsSL --connect-timeout 10 \
    -o "$KTLINT_BIN" \
    "https://github.com/pinterest/ktlint/releases/download/$KTLINT_VERSION/ktlint" \
    && chmod +x "$KTLINT_BIN"
fi

cd "$CLAUDE_PROJECT_DIR" || exit 0

if [ -x "$KTLINT_BIN" ]; then
  OUTPUT=$("$KTLINT_BIN" -F "$FILE_PATH" 2>&1)
  if [ $? -ne 0 ]; then
    # 자동 수정 불가능한 lint 오류는 Claude에게 피드백 (exit 2 = stderr 전달)
    echo "ktlint: 자동 수정되지 않은 오류가 있습니다:" >&2
    echo "$OUTPUT" >&2
    exit 2
  fi
else
  # 다운로드 실패 시 기존 Gradle 방식으로 폴백
  ./gradlew ktlintFormat 2>&1 >&2
fi

exit 0
