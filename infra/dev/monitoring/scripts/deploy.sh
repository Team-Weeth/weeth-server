#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/home/ubuntu/infra/dev/monitoring}"
APP_NETWORK="${APP_NETWORK:-weeth-dev_web}"
MONITORING_ENV_FILE="${MONITORING_ENV_FILE:-$DEPLOY_DIR/../.env.monitoring}"

cd "$DEPLOY_DIR"

if [ -f "$MONITORING_ENV_FILE" ]; then
  ln -sf "$MONITORING_ENV_FILE" "$DEPLOY_DIR/.env"
elif [ -f "$DEPLOY_DIR/../.env" ]; then
  echo "[monitoring] warning: $MONITORING_ENV_FILE not found, falling back to $DEPLOY_DIR/../.env"
  ln -sf "$DEPLOY_DIR/../.env" "$DEPLOY_DIR/.env"
elif [ -f "$HOME/.env" ]; then
  echo "[monitoring] warning: $MONITORING_ENV_FILE not found, falling back to $HOME/.env"
  ln -sf "$HOME/.env" "$DEPLOY_DIR/.env"
else
  echo "[monitoring] env file not found: $MONITORING_ENV_FILE"
  exit 1
fi

if ! docker network inspect "$APP_NETWORK" >/dev/null 2>&1; then
  echo "[monitoring] required docker network not found: $APP_NETWORK"
  echo "[monitoring] deploy the app stack first or create the network before deploying monitoring"
  exit 1
fi

echo "[monitoring] pulling images..."
docker compose pull

echo "[monitoring] starting monitoring stack..."
docker compose up -d

echo "[monitoring] waiting for services to be healthy..."
for i in {1..15}; do
  if curl -fsS "http://127.0.0.1:12345/-/ready" >/dev/null 2>&1 &&
     curl -fsS "http://127.0.0.1:9090/-/ready" >/dev/null 2>&1 &&
     curl -fsS "http://127.0.0.1:3100/ready" >/dev/null 2>&1 &&
     curl -fsS "http://127.0.0.1:3200/ready" >/dev/null 2>&1 &&
     curl -fsS "http://127.0.0.1:3000/api/health" >/dev/null 2>&1; then
    echo "[monitoring] all services healthy"
    break
  fi

  if [ "$i" -eq 15 ]; then
    echo "[monitoring] health check failed — check docker compose logs"
    exit 1
  fi

  sleep 2
done

echo "[monitoring] deploy completed"
