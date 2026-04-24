#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/home/ubuntu/infra/prod/monitoring}"
APP_NETWORK="${APP_NETWORK:-weeth-prod_web}"
MONITORING_ENV_FILE="${MONITORING_ENV_FILE:-$DEPLOY_DIR/.env.monitoring}"

cd "$DEPLOY_DIR"

if [ ! -f "$MONITORING_ENV_FILE" ]; then
  echo "[monitoring] env file not found: $MONITORING_ENV_FILE"
  exit 1
fi

export MONITORING_ENV_FILE

if ! docker network inspect "$APP_NETWORK" >/dev/null 2>&1; then
  echo "[monitoring] required docker network not found: $APP_NETWORK"
  echo "[monitoring] deploy the app stack first or create the network before deploying monitoring"
  exit 1
fi

echo "[monitoring] pulling images..."
docker compose --env-file "$MONITORING_ENV_FILE" pull

echo "[monitoring] starting monitoring stack..."
docker compose --env-file "$MONITORING_ENV_FILE" up -d

echo "[monitoring] waiting for services to be healthy..."
for i in {1..30}; do
  if curl -fsS "http://127.0.0.1:12345/-/ready" >/dev/null 2>&1 &&
     curl -fsS "http://127.0.0.1:9090/-/ready" >/dev/null 2>&1 &&
     curl -fsS "http://127.0.0.1:3100/ready" >/dev/null 2>&1 &&
     curl -fsS "http://127.0.0.1:3200/ready" >/dev/null 2>&1 &&
     curl -fsS "http://127.0.0.1:3000/api/health" >/dev/null 2>&1; then
    echo "[monitoring] all services healthy"
    break
  fi

  if [ "$i" -eq 30 ]; then
    echo "[monitoring] health check failed — check docker compose logs"
    exit 1
  fi

  sleep 2
done

echo "[monitoring] deploy completed"
