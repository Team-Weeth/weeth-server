#!/bin/bash
# PreToolUse hook: 보호 브랜치(main, dev)로의 git push 차단
#
# settings.json의 deny 규칙은 명령 문자열 패턴 매칭이라
# `HEAD:refs/heads/main`, `git -C . push origin main`, `main --force`,
# main 브랜치에서 인자 없는 `git push` 같은 변형을 놓친다.
# 이 훅은 git 서브커맨드와 refspec을 파싱해 실제 push 대상을 판별한다.
# bypass permissions 모드에서도 동작한다.
#
# 문자열 기반 판별이므로 eval/변수 치환 같은 의도적 우회까지 막지는 못한다.
# 최종 방어선은 GitHub branch protection이다.

PROTECTED_BRANCHES="main dev"

INPUT=$(cat)
COMMAND=$(printf '%s' "$INPUT" | jq -r '.tool_input.command // empty')
BASE_DIR=$(printf '%s' "$INPUT" | jq -r '.cwd // empty')
[ -z "$BASE_DIR" ] && BASE_DIR="$PWD"

[ -z "$COMMAND" ] && exit 0
case "$COMMAND" in
  *git*push*) ;;
  *) exit 0 ;;
esac

deny() {
  echo "block-protected-push: 보호 브랜치(${PROTECTED_BRANCHES// /, })로의 push는 차단됩니다 — $1" >&2
  echo "feature 브랜치로 push한 뒤 PR을 사용하세요." >&2
  exit 2
}

is_protected() {
  local name="${1#refs/heads/}"
  local b
  for b in $PROTECTED_BRANCHES; do
    [ "$name" = "$b" ] && return 0
  done
  return 1
}

# 인자 없는 push / HEAD push의 실제 대상 브랜치: @{push} → 현재 브랜치 순으로 판별
push_target_of() {
  local dir="$1" target
  target=$(git -C "$dir" rev-parse --abbrev-ref --symbolic-full-name '@{push}' 2>/dev/null)
  if [ -n "$target" ]; then
    echo "${target#*/}"
    return
  fi
  git -C "$dir" branch --show-current 2>/dev/null
}

check_push() {
  local dir="$1"
  shift
  local positionals=() tok
  for tok in "$@"; do
    case "$tok" in
      --all | --mirror) deny "$tok 옵션" ;;
      -*) ;;
      *) positionals+=("$tok") ;;
    esac
  done

  # positionals[0]은 remote, 나머지는 refspec
  if [ "${#positionals[@]}" -le 1 ]; then
    local target
    target=$(push_target_of "$dir")
    is_protected "$target" && deny "현재 브랜치의 push 대상이 $target"
    return
  fi

  local i refspec dst
  for ((i = 1; i < ${#positionals[@]}; i++)); do
    refspec="${positionals[$i]#+}"
    case "$refspec" in
      *:*) dst="${refspec##*:}" ;;
      *) dst="$refspec" ;;
    esac
    if [ "$dst" = "HEAD" ]; then
      dst=$(push_target_of "$dir")
    fi
    is_protected "$dst" && deny "refspec ${positionals[$i]}"
  done
}

# 복합 명령을 세그먼트 단위로 분리하고, 괄호와 따옴표는 제거해 토큰화한다
NL=$'\n'
SEGMENTS="$COMMAND"
SEGMENTS="${SEGMENTS//&&/$NL}"
SEGMENTS="${SEGMENTS//||/$NL}"
SEGMENTS="${SEGMENTS//;/$NL}"
SEGMENTS="${SEGMENTS//|/$NL}"
SEGMENTS="${SEGMENTS//&/$NL}"
SEGMENTS=$(printf '%s\n' "$SEGMENTS" | tr -d "()\"'\`")

dir="$BASE_DIR"
while IFS= read -r segment; do
  read -ra tokens <<<"$segment"
  [ "${#tokens[@]}" -eq 0 ] && continue

  # `cd <dir> && git push` 형태의 작업 디렉터리 변경 추적
  if [ "${tokens[0]}" = "cd" ] && [ -n "${tokens[1]}" ]; then
    case "${tokens[1]}" in
      /*) dir="${tokens[1]}" ;;
      *) dir="$dir/${tokens[1]}" ;;
    esac
    continue
  fi

  # 명령 위치의 git만 검사한다. 커밋 메시지·PR 본문 속 "git push main" 문구는 무시
  n=${#tokens[@]}
  g=0
  while [ "$g" -lt "$n" ]; do
    case "${tokens[$g]}" in
      sudo | command | exec | env | time | nohup | bash | sh | zsh | -c | -lc | *=*) g=$((g + 1)) ;;
      *) break ;;
    esac
  done
  [ "${tokens[$g]##*/}" = "git" ] || continue

  # git 전역 옵션(-C <dir>, -c <k=v> 등)을 건너뛰어 서브커맨드를 찾는다
  git_dir="$dir"
  j=$((g + 1))
  while [ "$j" -lt "$n" ]; do
    case "${tokens[$j]}" in
      -C)
        j=$((j + 1))
        case "${tokens[$j]}" in
          /*) git_dir="${tokens[$j]}" ;;
          *) git_dir="$git_dir/${tokens[$j]}" ;;
        esac
        ;;
      -c) j=$((j + 1)) ;;
      -*) ;;
      *) break ;;
    esac
    j=$((j + 1))
  done

  if [ "${tokens[$j]}" = "push" ]; then
    check_push "$git_dir" "${tokens[@]:$((j + 1))}"
  fi
done <<<"$SEGMENTS"

exit 0
