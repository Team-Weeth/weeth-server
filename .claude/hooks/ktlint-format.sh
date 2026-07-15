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
# 공급망 변조 차단용 고정 체크섬. 버전 변경 시 함께 갱신할 것.
# 산출: curl -fsSL .../$KTLINT_VERSION/ktlint | shasum -a 256
KTLINT_SHA256="a3fd620207d5c40da6ca789b95e7f823c54e854b7fade7f613e91096a3706d75"
CACHE_DIR="$HOME/.cache/ktlint"
KTLINT_BIN="$CACHE_DIR/ktlint-$KTLINT_VERSION"

# 최초 1회만 다운로드 (이후 캐시 사용)
if [ ! -x "$KTLINT_BIN" ]; then
  mkdir -p "$CACHE_DIR"
  # 임시 파일에 받아 검증 통과 후에만 캐시 경로로 이동 — 변조/부분 다운로드된
  # 바이너리가 캐시에 남아 다음 실행에서 그대로 실행되는 것을 방지한다.
  TMP_BIN=$(mktemp "$CACHE_DIR/ktlint-$KTLINT_VERSION.XXXXXX")
  if curl -fsSL --connect-timeout 10 -o "$TMP_BIN" \
       "https://github.com/pinterest/ktlint/releases/download/$KTLINT_VERSION/ktlint"; then
    # macOS에는 sha256sum이 기본 미설치 — 없으면 shasum으로 폴백
    if command -v sha256sum >/dev/null 2>&1; then
      echo "$KTLINT_SHA256  $TMP_BIN" | sha256sum -c - >/dev/null 2>&1
    else
      echo "$KTLINT_SHA256  $TMP_BIN" | shasum -a 256 -c - >/dev/null 2>&1
    fi
    if [ $? -eq 0 ]; then
      chmod +x "$TMP_BIN" && mv -f "$TMP_BIN" "$KTLINT_BIN"
    else
      echo "ktlint: 다운로드 바이너리 SHA-256 검증 실패 — 폐기 후 Gradle로 폴백합니다." >&2
      rm -f "$TMP_BIN"
    fi
  else
    rm -f "$TMP_BIN"
  fi
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
