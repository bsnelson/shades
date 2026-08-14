#!/usr/bin/env bash
# Deploys shades to the production Raspberry Pi (pi@192.168.7.48).
#
# Usage:
#   ./deploy.sh [config] [--yes]
#     (no args)     Full deploy: build the jar, push jar + application.yml, restart, verify.
#     config        Config-only deploy: push application.yml only, restart, verify.
#     --yes         Skip the interactive confirmation prompt before pushing a changed
#                    application.yml. Without a TTY (e.g. run by an agent) the prompt
#                    can't be answered, so review the printed diff first, then re-run
#                    with --yes once you're sure it's correct - don't add --yes blindly.
#
# The repo's src/main/resources/application.yml is the single source of truth.
# The Pi never gets hand-edited directly - this script is the only path onto it.
set -euo pipefail

PI_HOST="pi@192.168.7.48"
REMOTE_DIR="/usr/local/shades/api"
SERVICE="shadesapi.service"
LOCAL_YML="src/main/resources/application.yml"

MODE="full"
AUTO_YES=0
for arg in "$@"; do
  case "$arg" in
    full|config) MODE="$arg" ;;
    --yes) AUTO_YES=1 ;;
    *)
      echo "Usage: $0 [full|config] [--yes]" >&2
      exit 1
      ;;
  esac
done

cd "$(dirname "$0")"

if [[ "$MODE" == "full" ]]; then
  echo "==> Building jar"
  ./gradlew clean bootJar -q

  JAR_PATH=$(ls build/libs/shades-*.jar | head -1)
  JAR_NAME=$(basename "$JAR_PATH")

  echo "==> Copying $JAR_NAME to Pi"
  scp -q "$JAR_PATH" "$PI_HOST:/tmp/$JAR_NAME"
fi

echo "==> Diffing application.yml against what's currently live on the Pi"
REMOTE_YML_TMP=$(mktemp)
trap 'rm -f "$REMOTE_YML_TMP"' EXIT
scp -q "$PI_HOST:$REMOTE_DIR/application.yml" "$REMOTE_YML_TMP" 2>/dev/null || true

if diff -q "$REMOTE_YML_TMP" "$LOCAL_YML" >/dev/null 2>&1; then
  echo "    No config changes - skipping application.yml deploy and restart-for-config."
  SKIP_YML=1
else
  echo "--- live on Pi vs. $LOCAL_YML ---"
  diff -u "$REMOTE_YML_TMP" "$LOCAL_YML" || true
  echo "---------------------------------"
  if [[ "$AUTO_YES" == "1" ]]; then
    echo "--yes passed, proceeding without prompt."
  else
    read -r -p "Push this application.yml to prod? [y/N] " CONFIRM
    if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
      echo "Aborted."
      exit 1
    fi
  fi
  SKIP_YML=0
fi

if [[ "$SKIP_YML" == "0" ]]; then
  echo "==> Copying application.yml to Pi"
  scp -q "$LOCAL_YML" "$PI_HOST:/tmp/application.yml"
fi

if [[ "$MODE" == "full" ]]; then
  echo "==> Installing jar and repointing shades-LATEST.jar"
  ssh "$PI_HOST" "
    sudo mv /tmp/$JAR_NAME $REMOTE_DIR/$JAR_NAME &&
    sudo chown pi:pi $REMOTE_DIR/$JAR_NAME &&
    sudo ln -sfn $JAR_NAME $REMOTE_DIR/shades-LATEST.jar
  "
fi

if [[ "$SKIP_YML" == "0" ]]; then
  echo "==> Installing application.yml"
  ssh "$PI_HOST" "
    sudo mv /tmp/application.yml $REMOTE_DIR/application.yml &&
    sudo chown root:root $REMOTE_DIR/application.yml
  "
fi

if [[ "$MODE" == "config" && "$SKIP_YML" == "1" ]]; then
  echo "==> Nothing to deploy, service left untouched."
  exit 0
fi

echo "==> Restarting $SERVICE"
ssh "$PI_HOST" "sudo systemctl restart $SERVICE"

echo "==> Waiting for service to come back up (this Pi is slow and RAM-constrained - JVM boot has taken over 2 minutes under load, be patient)"
ssh "$PI_HOST" '
  for i in $(seq 1 60); do
    R=$(curl -s -m 2 http://localhost:8081/listNames || true)
    if [ -n "$R" ]; then
      echo "$R"
      exit 0
    fi
    sleep 5
  done
  echo "TIMEOUT waiting for shadesapi to respond after 5 minutes - check journalctl -u shadesapi.service on the Pi" >&2
  exit 1
'

echo "==> Deploy complete"
