#!/bin/bash
# implementation-evaluator 전용 PreToolUse 훅: Bash를 읽기성 명령 allowlist로 제한
#
# 평가자의 "쓰기 권한 없음"은 프롬프트 약속이 아니라 이 훅으로 강제된다.
# 현실적 실패 모드는 평가자가 발견한 문제를 직접 고쳐버리는 것 — 그 순간
# 생성자-평가자 분리가 무너지므로 조회/검증 명령 외에는 전부 차단한다.
#
# 허용: git diff/log/status/show, ./gradlew test* / compileKotlin / compileTestKotlin
# 차단: 그 외 전부 + 셸 메타문자(; & | > < ` $)가 섞인 복합 명령
#
# 인자 패턴 매칭은 변형에 취약하므로 이 훅은 1차 방어선이다.
# 2차 방어선은 verify-implementation 스킬의 스폰 전후 git diff 해시 비교.

INPUT=$(cat)
COMMAND=$(printf '%s' "$INPUT" | jq -r '.tool_input.command // empty')

deny() {
  echo "evaluator-bash-allowlist: 차단된 명령입니다 — $1" >&2
  echo "평가자는 읽기 전용입니다. 허용 명령: git diff/log/status/show, ./gradlew test*/compileKotlin/compileTestKotlin. 파일 탐색은 Read/Glob/Grep 도구를 사용하세요." >&2
  exit 2
}

[ -z "$COMMAND" ] && deny "빈 명령"

# 복합 명령/리다이렉션/치환 차단 (allowlist 우회 경로)
case "$COMMAND" in
  *';'* | *'&'* | *'|'* | *'>'* | *'<'* | *'`'* | *'$('* | *$'\n'*)
    deny "셸 메타문자 포함: $COMMAND"
    ;;
esac

# git --no-pager 접두 변형은 동일한 읽기성 명령으로 정규화
NORMALIZED="${COMMAND/#git --no-pager /git }"

case "$NORMALIZED" in
  'git diff' | 'git diff '* | \
  'git log' | 'git log '* | \
  'git status' | 'git status '* | \
  'git show' | 'git show '* | \
  './gradlew test' | './gradlew test '* | \
  './gradlew compileKotlin' | './gradlew compileKotlin '* | \
  './gradlew compileTestKotlin' | './gradlew compileTestKotlin '* | \
  './gradlew compileKotlin compileTestKotlin' | './gradlew compileKotlin compileTestKotlin '*)
    exit 0
    ;;
  *)
    deny "$COMMAND"
    ;;
esac
