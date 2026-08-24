#!/usr/bin/env bash
# Deploys shades to the production Raspberry Pi (pi@192.168.7.48).
#
# Usage:
#   ./deploy.sh [--yes]
#     --yes   Skip the interactive confirmation prompt before pushing changed files.
#             Without a TTY (e.g. run by an agent) the prompt can't be answered, so
#             review the printed diff first, then re-run with --yes once you're sure
#             it's correct - don't add --yes blindly.
#
# No build step on the Pi - server/index.js runs as-is, no npm/build tooling
# there. server/{index.js,devices.json,package.json,.env} in the repo are the
# single source of truth. The Pi never gets hand-edited directly - this script
# is the only path onto it.
#
# server/node_modules (currently just the mqtt client, for Zigbee2MQTT support -
# see devices.json's "zigbee" type) is vendored: regenerated locally via `npm
# install` below (the Pi has no npm at all) and synced over as a plain file tree,
# same as index.js/devices.json - not a build step on the target, just another
# artifact this script copies across.
set -euo pipefail

PI_HOST="pi@192.168.7.48"
REMOTE_DIR="/usr/local/shades/api"
SERVICE="shadesapi.service"
LOCAL_DIR="server"
FILES=(index.js devices.json package.json .env)

AUTO_YES=0
for arg in "$@"; do
  case "$arg" in
    --yes) AUTO_YES=1 ;;
    *)
      echo "Usage: $0 [--yes]" >&2
      exit 1
      ;;
  esac
done

cd "$(dirname "$0")"

TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

CHANGED=()
for f in "${FILES[@]}"; do
  scp -q "$PI_HOST:$REMOTE_DIR/$f" "$TMP_DIR/$f" 2>/dev/null || true
  if ! diff -q "$TMP_DIR/$f" "$LOCAL_DIR/$f" >/dev/null 2>&1; then
    CHANGED+=("$f")
  fi
done

# node_modules is a vendored tree, not a single diffable text file - regenerate
# it locally from package.json (no-ops quickly if already up to date), then
# compare a tarball checksum against what's actually live on the Pi, the same
# "trust the live target, not a marker file" approach the FILES loop above uses.
NODE_MODULES_CHANGED=0
if [[ -f "$LOCAL_DIR/package.json" ]]; then
  echo "==> Ensuring local node_modules matches package.json"
  (cd "$LOCAL_DIR" && npm install --silent)
  LOCAL_NM_HASH=$(tar -cf - -C "$LOCAL_DIR" node_modules 2>/dev/null | shasum -a 256 | cut -d' ' -f1)
  REMOTE_NM_HASH=$(ssh "$PI_HOST" "tar -cf - -C $REMOTE_DIR node_modules 2>/dev/null | shasum -a 256 | cut -d' ' -f1" 2>/dev/null || echo "")
  if [[ "$LOCAL_NM_HASH" != "$REMOTE_NM_HASH" ]]; then
    NODE_MODULES_CHANGED=1
  fi
fi

if [[ ${#CHANGED[@]} -eq 0 && "$NODE_MODULES_CHANGED" -eq 0 ]]; then
  echo "==> No changes to deploy, service left untouched."
  exit 0
fi

if [[ ${#CHANGED[@]} -gt 0 ]]; then
  echo "--- changed files: ${CHANGED[*]} ---"
  for f in "${CHANGED[@]}"; do
    echo "--- live on Pi vs. $LOCAL_DIR/$f ---"
    diff -u "$TMP_DIR/$f" "$LOCAL_DIR/$f" || true
  done
fi
if [[ "$NODE_MODULES_CHANGED" -eq 1 ]]; then
  echo "--- node_modules changed (will sync full tree, $(du -sh "$LOCAL_DIR/node_modules" | cut -f1)) ---"
fi
echo "---------------------------------"

if [[ "$AUTO_YES" == "1" ]]; then
  echo "--yes passed, proceeding without prompt."
else
  read -r -p "Push these changes to prod? [y/N] " CONFIRM
  if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
    echo "Aborted."
    exit 1
  fi
fi

ssh "$PI_HOST" "sudo mkdir -p $REMOTE_DIR"

if [[ ${#CHANGED[@]} -gt 0 ]]; then
  echo "==> Copying changed files to Pi"
  for f in "${CHANGED[@]}"; do
    scp -q "$LOCAL_DIR/$f" "$PI_HOST:/tmp/deploy_$f"
  done

  echo "==> Installing files"
  for f in "${CHANGED[@]}"; do
    ssh "$PI_HOST" "sudo mv /tmp/deploy_$f $REMOTE_DIR/$f && sudo chown pi:pi $REMOTE_DIR/$f"
  done
fi

if [[ "$NODE_MODULES_CHANGED" -eq 1 ]]; then
  echo "==> Syncing node_modules to Pi"
  ssh "$PI_HOST" "sudo rm -rf $REMOTE_DIR/node_modules"
  tar -cf - -C "$LOCAL_DIR" node_modules | ssh "$PI_HOST" "sudo tar -xf - -C $REMOTE_DIR && sudo chown -R pi:pi $REMOTE_DIR/node_modules"
fi

echo "==> Restarting $SERVICE"
ssh "$PI_HOST" "sudo systemctl restart $SERVICE"

echo "==> Waiting for service to come back up"
ssh "$PI_HOST" '
  for i in $(seq 1 20); do
    R=$(curl -s -m 2 http://localhost:8081/listNames || true)
    if [ -n "$R" ]; then
      echo "$R"
      exit 0
    fi
    sleep 2
  done
  echo "TIMEOUT waiting for shadesapi to respond - check journalctl -u shadesapi.service on the Pi" >&2
  exit 1
'

echo "==> Deploy complete"
