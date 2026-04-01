#!/usr/bin/env bash
set -euo pipefail

# Github Action에서 주입한 환경변수 사용
: "${APP_IMAGE:?APP_IMAGE is required}"
: "${DOMAIN:?DOMAIN is required}"
: "${DEPLOY_DIR:=/opt/weeth/dev}"

cd "$DEPLOY_DIR"

export APP_IMAGE DOMAIN

# EC2 홈 디렉토리의 .env를 심링크
ln -sf "$HOME/.env" "$DEPLOY_DIR/.env"

if [ ! -f ./caddy/upstream.conf ]; then
  echo "reverse_proxy weeth-dev-app-blue:8080" > ./caddy/upstream.conf
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

for i in {1..20}; do
  if curl -fsS "http://127.0.0.1:${NEW_HEALTH_PORT}/actuator/health" >/dev/null; then
    echo "[deploy] new app is healthy"
    break
  fi

  if [ "$i" -eq 20 ]; then
    echo "[deploy] health check failed"
    exit 1
  fi

  sleep 3
done

echo "reverse_proxy weeth-dev-app-${NEW_COLOR}:8080" > ./caddy/upstream.conf

# 현재 Caddy 컨테이너의 DOMAIN과 비교하여 변경 시에만 재생성
CURRENT_DOMAIN=$(docker inspect weeth-dev-caddy --format '{{range .Config.Env}}{{println .}}{{end}}' 2>/dev/null | grep '^DOMAIN=' | cut -d= -f2-)

if [ "$CURRENT_DOMAIN" != "$DOMAIN" ]; then
  echo "[deploy] domain changed, recreating caddy"
  docker compose up -d --force-recreate caddy
elif docker compose ps caddy --format '{{.State}}' 2>/dev/null | grep -q running; then
  docker compose exec caddy caddy reload --config /etc/caddy/Caddyfile
else
  docker compose up -d caddy
fi

docker compose --profile "$OLD_COLOR" -f docker-compose.yml stop "app-$OLD_COLOR" || true
docker compose --profile "$OLD_COLOR" -f docker-compose.yml rm -f "app-$OLD_COLOR" || true

echo "[deploy] completed"
