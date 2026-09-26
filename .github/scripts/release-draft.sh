#!/usr/bin/env bash
# main 머지 커밋을 타깃으로 draft 릴리즈를 생성하거나, 이미 있는 draft를 갱신한다.
#
# 입력(env)
#   TARGET_SHA  (필수) draft가 가리킬 main 커밋 SHA
#   GH_REPO     (필수) owner/repo
#   GH_TOKEN    (필수) contents: write 권한 토큰
#   BUMP_INPUT  (선택) major|minor|patch — 지정 시 브랜치/라벨 판정을 건너뛴다 (workflow_dispatch)
#   HEAD_REF    (선택) 머지된 PR의 head 브랜치
#   PR_LABELS   (선택) 머지된 PR의 라벨 JSON 배열
#   PR_NUMBER   (선택) 머지된 PR 번호 (Slack 알림 표시용)
#   SLACK_WEBHOOK_URL (선택) 설정 시 draft 생성/갱신을 Slack으로 알린다
set -euo pipefail

SEMVER_RE='^v?[0-9]+\.[0-9]+\.[0-9]+$'

# hotfix는 patch, 릴리즈 PR은 라벨로 판정(기본 minor), 그 외 main 직행 PR은 patch
resolve_bump() {
  local head_ref="$1" labels="$2"
  case "$head_ref" in
    hotfix/*) echo patch ;;
    dev | release/*)
      if grep -qxF '💥 Breaking' <<<"$labels"; then
        echo major
      elif grep -qxF '🩹 Patch' <<<"$labels"; then
        echo patch
      else
        echo minor
      fi
      ;;
    *) echo patch ;;
  esac
}

# next_version v1.2.3 minor → 1.3.0
next_version() {
  local major minor patch
  IFS=. read -r major minor patch <<<"${1#v}"
  case "$2" in
    major) echo "$((major + 1)).0.0" ;;
    minor) echo "$major.$((minor + 1)).0" ;;
    patch) echo "$major.$minor.$((patch + 1))" ;;
    *)
      echo "unknown bump: $2" >&2
      return 1
      ;;
  esac
}

sort_versions() { sort -t. -k1,1n -k2,2n -k3,3n; }

# max_version v1.3.0 1.2.1 → 1.3.0
max_version() { printf '%s\n' "${1#v}" "${2#v}" | sort_versions | tail -1; }

# stdin의 태그 목록 중 가장 높은 semver 태그 (없으면 빈 문자열)
highest_tag() { { grep -E "$SEMVER_RE" || true; } | sed 's/^v//' | sort_versions | tail -1 | sed '/./s/^/v/'; }

latest_published_tag() {
  gh release list --exclude-drafts --exclude-pre-releases --limit 100 \
    --json tagName -q '.[].tagName' | highest_tag
}

existing_draft_tag() {
  gh release list --limit 100 --json tagName,isDraft \
    -q '.[] | select(.isDraft) | .tagName' | highest_tag
}

generate_notes() {
  local tag="$1" prev="$2"
  local args=(-f tag_name="$tag" -f target_commitish="$TARGET_SHA")
  [[ -n "$prev" ]] && args+=(-f previous_tag_name="$prev")
  gh api "repos/$GH_REPO/releases/generate-notes" "${args[@]}" -q .body
}

# Slack mrkdwn 제어 문자 이스케이프
# bash 5.2+ 는 ${s//x/y} 의 y에서 &를 매칭 문자열로 치환하므로(patsub_replacement) sed를 쓴다
slack_escape() { sed -e 's/&/\&amp;/g' -e 's/</\&lt;/g' -e 's/>/\&gt;/g' <<<"$1"; }

# 알림 실패가 draft 생성을 실패로 만들지 않도록 경고만 남긴다
notify_slack() {
  [[ -z "${SLACK_WEBHOOK_URL:-}" ]] && return 0
  local payload
  payload=$(jq -nc --arg text "$1" '{text: $text}')
  if ! curl -fsS --max-time 10 -H 'Content-Type: application/json' -d "$payload" "$SLACK_WEBHOOK_URL" >/dev/null; then
    echo "::warning::Slack 알림 전송 실패"
  fi
}

draft_message() {
  local tag="$1" bump="$2" draft_tag="$3" url="$4" trigger action
  if [[ -n "${PR_NUMBER:-}" ]]; then
    trigger="<https://github.com/$GH_REPO/pull/$PR_NUMBER|#$PR_NUMBER> $(slack_escape "${HEAD_REF:-}")"
  else
    trigger="수동 실행"
  fi
  if [[ -z "$draft_tag" ]]; then
    action="생성"
  elif [[ "$draft_tag" == "$tag" ]]; then
    action="갱신"
  else
    action="갱신 ($draft_tag → $tag)"
  fi
  printf '📦 *%s* draft %s (%s · %s)\n확인 후 Publish: <%s|%s draft 열기>' \
    "$tag" "$action" "$bump" "$trigger" "$url" "$tag"
}

main() {
  : "${TARGET_SHA:?TARGET_SHA is required}"
  : "${GH_REPO:?GH_REPO is required}"

  local bump
  if [[ -n "${BUMP_INPUT:-}" ]]; then
    bump="$BUMP_INPUT"
  else
    bump=$(resolve_bump "${HEAD_REF:-}" "$(jq -r '.[]?' <<<"${PR_LABELS:-[]}")")
  fi

  local prev_tag draft_tag version tag notes out url
  prev_tag=$(latest_published_tag)
  version=$(next_version "${prev_tag:-v0.0.0}" "$bump")

  # publish 전에 main 머지가 여러 번 쌓이면 draft 하나에 모으고, 가장 큰 bump 버전을 유지한다
  draft_tag=$(existing_draft_tag)
  [[ -n "$draft_tag" ]] && version=$(max_version "$version" "$draft_tag")
  tag="v$version"

  notes=$(generate_notes "$tag" "$prev_tag")

  if [[ -n "$draft_tag" ]]; then
    out=$(gh release edit "$draft_tag" --draft --tag "$tag" --target "$TARGET_SHA" --title "$tag" --notes "$notes")
    echo "Updated draft $draft_tag → $tag (bump=$bump, prev=${prev_tag:-none}, target=$TARGET_SHA)"
  else
    out=$(gh release create "$tag" --draft --target "$TARGET_SHA" --title "$tag" --notes "$notes")
    echo "Created draft $tag (bump=$bump, prev=${prev_tag:-none}, target=$TARGET_SHA)"
  fi

  # gh release create/edit 는 릴리즈 URL을 출력한다. draft는 untagged-* 주소라 못 얻으면 목록 페이지로 대신한다
  url=$({ grep -E '^https://' <<<"$out" || true; } | tail -1)
  notify_slack "$(draft_message "$tag" "$bump" "$draft_tag" "${url:-https://github.com/$GH_REPO/releases}")"

  if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
    {
      echo "### Draft release \`$tag\`"
      echo "- bump: \`$bump\` / previous: \`${prev_tag:-none}\` / target: \`$TARGET_SHA\`"
    } >>"$GITHUB_STEP_SUMMARY"
  fi
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  main "$@"
fi
