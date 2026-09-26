#!/usr/bin/env bash
# release-draft.sh 로컬 테스트: bash .github/scripts/release-draft.test.sh
# gh를 스텁으로 대체하므로 네트워크/권한 없이 실행된다.
set -uo pipefail

SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/release-draft.sh"
# shellcheck source=release-draft.sh
source "$SCRIPT"
set +e # source한 스크립트의 errexit 해제 (실패 케이스도 끝까지 확인)

failures=0
assert_eq() {
  if [[ "$2" == "$3" ]]; then
    echo "ok   $1"
  else
    echo "FAIL $1: got '$2', want '$3'"
    failures=$((failures + 1))
  fi
}

echo "# resolve_bump"
assert_eq "hotfix/* → patch" "$(resolve_bump hotfix/auth '🚑 Hotfix')" patch
assert_eq "dev (라벨 없음) → minor" "$(resolve_bump dev '')" minor
assert_eq "dev + 기타 라벨 → minor" "$(resolve_bump dev $'🚀 Release\n✨ Feature')" minor
assert_eq "dev + 💥 Breaking → major" "$(resolve_bump dev $'🚀 Release\n💥 Breaking')" major
assert_eq "dev + 🩹 Patch → patch" "$(resolve_bump dev $'🚀 Release\n🩹 Patch')" patch
assert_eq "💥 Breaking 이 🩹 Patch 보다 우선" "$(resolve_bump dev $'🩹 Patch\n💥 Breaking')" major
assert_eq "release/* → minor" "$(resolve_bump release/v1.3.0 '')" minor
assert_eq "feat/* 직행 → patch" "$(resolve_bump feat/WTH-1-x '✨ Feature')" patch
assert_eq "devtools 는 릴리즈 PR 아님 → patch" "$(resolve_bump devtools '')" patch

echo "# next_version"
assert_eq "v1.2.3 major" "$(next_version v1.2.3 major)" 2.0.0
assert_eq "v1.2.3 minor" "$(next_version v1.2.3 minor)" 1.3.0
assert_eq "v1.2.3 patch" "$(next_version v1.2.3 patch)" 1.2.4
assert_eq "v 접두사 없음" "$(next_version 1.2.9 patch)" 1.2.10
assert_eq "태그 없음(v0.0.0 기준) minor" "$(next_version v0.0.0 minor)" 0.1.0
next_version v1.0.0 huge >/dev/null 2>&1
assert_eq "알 수 없는 bump 는 실패" "$?" 1

echo "# max_version / highest_tag"
assert_eq "숫자 정렬 (1.10.0 > 1.9.0)" "$(max_version v1.10.0 v1.9.0)" 1.10.0
assert_eq "draft v1.3.0 vs 1.2.1" "$(max_version 1.2.1 v1.3.0)" 1.3.0
assert_eq "같은 버전" "$(max_version v1.2.1 1.2.1)" 1.2.1
assert_eq "최고 태그 (숫자 정렬)" "$(printf 'v1.9.0\nv1.10.0\nv1.2.0\n' | highest_tag)" v1.10.0
assert_eq "semver 외/prerelease 태그 제외" "$(printf 'v2.0.0-rc.1\nlatest\nv1.1.0\n' | highest_tag)" v1.1.0
assert_eq "태그 없음 → 빈 값" "$(printf '' | highest_tag)" ""

echo "# main (gh 스텁)"
STUB_DIR=$(mktemp -d)
trap 'rm -rf "$STUB_DIR"' EXIT
cat >"$STUB_DIR/gh" <<'EOF'
#!/usr/bin/env bash
echo "gh $*" >>"$GH_LOG"
case "$1 $2" in
  "release list") if [[ "$*" == *--exclude-drafts* ]]; then printf '%s' "$PUBLISHED"; else printf '%s' "$DRAFTS"; fi ;;
  "api repos/"*) echo "NOTES" ;;
  "release create" | "release edit") [[ "${STUB_NO_URL:-}" == 1 ]] || echo "https://github.com/o/r/releases/tag/untagged-1" ;;
esac
EOF
chmod +x "$STUB_DIR/gh"
# curl 스텁: -d 로 넘어온 payload를 기록하고, CURL_FAIL=1 이면 실패
cat >"$STUB_DIR/curl" <<'EOF'
#!/usr/bin/env bash
while [[ $# -gt 0 ]]; do
  [[ "$1" == "-d" ]] && echo "curl $2" >>"$GH_LOG"
  shift
done
[[ "${CURL_FAIL:-}" != 1 ]]
EOF
chmod +x "$STUB_DIR/curl"

# run_main KEY=VALUE... → 실행된 gh release create/edit 명령 (실패 시 EXIT)
run_main() {
  (
    export PATH="$STUB_DIR:$PATH" GH_LOG="$STUB_DIR/log" GH_REPO=o/r TARGET_SHA=abc
    export PUBLISHED='' DRAFTS='' BUMP_INPUT='' HEAD_REF='' PR_LABELS='[]'
    export PR_NUMBER='' SLACK_WEBHOOK_URL='' CURL_FAIL='' STUB_NO_URL=''
    # shellcheck disable=SC2163 # KEY=VALUE 인자를 그대로 export
    export "$@"
    : >"$GH_LOG"
    bash "$SCRIPT" >/dev/null 2>&1 || echo EXIT
    grep -E '^gh release (create|edit)' "$GH_LOG"
  )
}

assert_eq "hotfix, draft 없음 → v1.2.1 생성" \
  "$(run_main PUBLISHED=$'v1.2.0\nv1.1.1' HEAD_REF=hotfix/a PR_LABELS='["🚑 Hotfix"]')" \
  "gh release create v1.2.1 --draft --target abc --title v1.2.1 --notes NOTES"
assert_eq "hotfix draft 위에 릴리즈 PR → v1.3.0 으로 갱신" \
  "$(run_main PUBLISHED=v1.2.0 DRAFTS=v1.2.1 HEAD_REF=dev PR_LABELS='["🚀 Release"]')" \
  "gh release edit v1.2.1 --draft --tag v1.3.0 --target abc --title v1.3.0 --notes NOTES"
assert_eq "minor draft 위에 hotfix → v1.3.0 유지" \
  "$(run_main PUBLISHED=v1.2.0 DRAFTS=v1.3.0 HEAD_REF=hotfix/b)" \
  "gh release edit v1.3.0 --draft --tag v1.3.0 --target abc --title v1.3.0 --notes NOTES"
assert_eq "workflow_dispatch major (라벨 null)" \
  "$(run_main PUBLISHED=v1.2.0 BUMP_INPUT=major PR_LABELS=null)" \
  "gh release create v2.0.0 --draft --target abc --title v2.0.0 --notes NOTES"
assert_eq "낮은 버전의 오래된 draft 흡수" \
  "$(run_main PUBLISHED=v1.2.0 DRAFTS=v1.1.2 HEAD_REF=dev)" \
  "gh release edit v1.1.2 --draft --tag v1.3.0 --target abc --title v1.3.0 --notes NOTES"
assert_eq "첫 릴리즈 → v0.1.0" \
  "$(run_main HEAD_REF=dev)" \
  "gh release create v0.1.0 --draft --target abc --title v0.1.0 --notes NOTES"
assert_eq "잘못된 bump → 릴리즈 호출 없이 실패" \
  "$(run_main PUBLISHED=v1.0.0 BUMP_INPUT=huge)" \
  "EXIT"

run_main PUBLISHED=v1.2.0 HEAD_REF=dev >/dev/null
assert_eq "generate-notes 에 직전 태그 전달" \
  "$(grep -c 'previous_tag_name=v1.2.0' "$STUB_DIR/log")" 1
run_main HEAD_REF=dev >/dev/null
assert_eq "태그 없으면 previous_tag_name 생략" \
  "$(grep -c 'previous_tag_name' "$STUB_DIR/log")" 0

echo "# slack 알림"
# slack_text KEY=VALUE... → 전송된 Slack 메시지 text (전송 안 했으면 빈 값, 스크립트 실패 시 EXIT)
slack_text() {
  run_main "$@" >/dev/null
  grep '^curl ' "$STUB_DIR/log" | sed 's/^curl //' | jq -r .text
}
WEBHOOK=SLACK_WEBHOOK_URL=https://hooks.example/x

assert_eq "시크릿 없으면 전송 안 함" \
  "$(run_main PUBLISHED=v1.2.0 HEAD_REF=dev >/dev/null; grep -c '^curl ' "$STUB_DIR/log")" 0
assert_eq "릴리즈 PR draft 생성 메시지" \
  "$(slack_text $WEBHOOK PUBLISHED=v1.2.0 HEAD_REF=dev PR_NUMBER=130)" \
  $'📦 *v1.3.0* draft 생성 (minor · <https://github.com/o/r/pull/130|#130> dev)\n확인 후 Publish: <https://github.com/o/r/releases/tag/untagged-1|v1.3.0 draft 열기>'
assert_eq "draft URL 을 못 얻으면 Releases 목록으로 폴백" \
  "$(slack_text $WEBHOOK STUB_NO_URL=1 PUBLISHED=v1.2.0 HEAD_REF=dev PR_NUMBER=130 | tail -1)" \
  '확인 후 Publish: <https://github.com/o/r/releases|v1.3.0 draft 열기>'
assert_eq "draft 버전이 바뀌면 이전 → 새 태그 표시" \
  "$(slack_text $WEBHOOK PUBLISHED=v1.2.0 DRAFTS=v1.2.1 HEAD_REF=dev PR_NUMBER=131 | head -1)" \
  '📦 *v1.3.0* draft 갱신 (v1.2.1 → v1.3.0) (minor · <https://github.com/o/r/pull/131|#131> dev)'
assert_eq "같은 버전 draft 갱신" \
  "$(slack_text $WEBHOOK PUBLISHED=v1.2.0 DRAFTS=v1.3.0 HEAD_REF=hotfix/b PR_NUMBER=132 | head -1)" \
  '📦 *v1.3.0* draft 갱신 (patch · <https://github.com/o/r/pull/132|#132> hotfix/b)'
assert_eq "workflow_dispatch 는 수동 실행 표시" \
  "$(slack_text $WEBHOOK PUBLISHED=v1.2.0 BUMP_INPUT=major PR_LABELS=null | head -1)" \
  '📦 *v2.0.0* draft 생성 (major · 수동 실행)'
assert_eq "브랜치명의 <>& 이스케이프" \
  "$(slack_text $WEBHOOK PUBLISHED=v1.2.0 HEAD_REF='hotfix/a<b>&c' PR_NUMBER=1 | head -1)" \
  '📦 *v1.2.1* draft 생성 (patch · <https://github.com/o/r/pull/1|#1> hotfix/a&lt;b&gt;&amp;c)'
assert_eq "Slack 전송 실패해도 draft 생성은 성공" \
  "$(run_main $WEBHOOK CURL_FAIL=1 PUBLISHED=v1.2.0 HEAD_REF=dev)" \
  "gh release create v1.3.0 --draft --target abc --title v1.3.0 --notes NOTES"

echo
if [[ $failures -gt 0 ]]; then
  echo "$failures test(s) failed"
  exit 1
fi
echo "all tests passed"
