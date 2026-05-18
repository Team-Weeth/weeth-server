#!/usr/bin/env bash
set -euo pipefail

# Github Action에서 주입한 환경변수 사용
: "${APP_IMAGE:?APP_IMAGE is required}"
: "${DOMAIN:?DOMAIN is required}"
: "${DEPLOY_DIR:=/opt/weeth/dev}"
: "${HEALTH_CHECK_TIMEOUT_SECONDS:=120}"
: "${HEALTH_CHECK_INTERVAL_SECONDS:=3}"
: "${HEALTH_CHECK_CURL_CONNECT_TIMEOUT_SECONDS:=2}"
: "${HEALTH_CHECK_CURL_MAX_TIME_SECONDS:=5}"

cd "$DEPLOY_DIR"

export APP_IMAGE DOMAIN

# EC2 홈 디렉토리의 .env를 심링크
ln -sf "$HOME/.env" "$DEPLOY_DIR/.env"

# upstream.conf가 없으면 현재 실행 중인 컨테이너를 감지하여 생성
if [ ! -f ./caddy/upstream.conf ]; then
  if docker ps --format '{{.Names}}' | grep -q "weeth-dev-app-green"; then
    echo "reverse_proxy weeth-dev-app-green:8080" > ./caddy/upstream.conf
  else
    echo "reverse_proxy weeth-dev-app-blue:8080" > ./caddy/upstream.conf
  fi
fi

if grep -q "app-blue" ./caddy/upstream.conf; then
  NEW_COLOR="green"
  NEW_HEALTH_PORT="18082"
  OLD_COLOR="blue"
else
  NEW_COLOR="blue"
  NEW_HEALTH_PORT="18081"
  OLD_COLOR="green"
fi

echo "[deploy] image=$APP_IMAGE new_color=$NEW_COLOR old_color=$OLD_COLOR"

docker compose --profile "$NEW_COLOR" -f docker-compose.yml pull "app-$NEW_COLOR"
docker compose --profile "$NEW_COLOR" -f docker-compose.yml up -d "app-$NEW_COLOR"

health_check_deadline=$((SECONDS + HEALTH_CHECK_TIMEOUT_SECONDS))

while [ "$SECONDS" -le "$health_check_deadline" ]; do
  if curl -fsS \
    --connect-timeout "$HEALTH_CHECK_CURL_CONNECT_TIMEOUT_SECONDS" \
    --max-time "$HEALTH_CHECK_CURL_MAX_TIME_SECONDS" \
    "http://127.0.0.1:${NEW_HEALTH_PORT}/actuator/health" >/dev/null; then
    echo "[deploy] new app is healthy"
    break
  fi

  if [ "$SECONDS" -ge "$health_check_deadline" ]; then
    echo "[deploy] health check failed after ${HEALTH_CHECK_TIMEOUT_SECONDS}s, stopping new container"
    docker compose --profile "$NEW_COLOR" -f docker-compose.yml stop "app-$NEW_COLOR" || true
    docker compose --profile "$NEW_COLOR" -f docker-compose.yml rm -f "app-$NEW_COLOR" || true
    exit 1
  fi

  sleep "$HEALTH_CHECK_INTERVAL_SECONDS"
done

echo "reverse_proxy weeth-dev-app-${NEW_COLOR}:8080" > ./caddy/upstream.conf

# 현재 Caddy 컨테이너의 DOMAIN과 비교하여 변경 시에만 재생성
CURRENT_DOMAIN=$(docker inspect weeth-dev-caddy --format '{{range .Config.Env}}{{println .}}{{end}}' 2>/dev/null | grep '^DOMAIN=' | cut -d= -f2- || true)

if [ "$CURRENT_DOMAIN" != "$DOMAIN" ]; then
  echo "[deploy] domain changed, recreating caddy"
  docker compose up -d --force-recreate caddy
elif docker compose ps caddy --format '{{.State}}' 2>/dev/null | grep -q running; then
  reload_ok=false
  if reload_output=$(docker compose exec -T caddy caddy reload --config /etc/caddy/Caddyfile 2>&1); then
    # reload가 0을 리턴해도 실제 활성 config에 새 upstream이 반영됐는지 admin API로 검증한다
    if docker compose exec -T caddy wget -qO- http://localhost:2019/config/ 2>/dev/null \
        | grep -q "weeth-dev-app-${NEW_COLOR}:8080"; then
      reload_ok=true
    else
      echo "[deploy] caddy reload returned ok but new upstream not active"
    fi
  else
    echo "[deploy] caddy reload failed:"
    echo "$reload_output"
  fi

  if [ "$reload_ok" = true ]; then
    echo "[deploy] caddy reloaded"
  else
    echo "[deploy] falling back to restart"
    docker compose restart caddy
  fi
else
  docker compose up -d caddy
fi

docker compose --profile "$OLD_COLOR" -f docker-compose.yml stop "app-$OLD_COLOR" || true
docker compose --profile "$OLD_COLOR" -f docker-compose.yml rm -f "app-$OLD_COLOR" || true

docker image prune -f
echo "[deploy] completed"
