# shades

A dependency-free Node.js API that unifies control of Soma Smart Shades and Sunsa shades behind one HTTP interface, so Home Assistant and other callers don't need to know which vendor a given shade is.

Originally a Spring Boot app; rewritten in Node on 2026-08-14 to match the sibling apps on its Pi (`roasts`, `brews`, `baskets`, `sonos-http-api`) and to stop fighting that box's limited RAM - the JVM took 60-145s+ to boot under memory pressure and used 150-250MB RSS, versus the Node version's sub-second boot and ~30MB RSS. The old Java source lives in git history (see the commit that removed `src/`, `build.gradle.kts`, etc.) if ever needed again.

## Structure

- `server/index.js` - the entire app. Node built-ins only (`http`, `https`, `url`, `fs`) - no `package.json`, no npm dependencies, no build step. Written to run as-is on the Pi's Node 12.
- `server/devices.json` - the device list (id, type, name, seasonal default position, groups) and the Soma/Sunsa API path templates. Structural config, committed to git.
- `server/.env` - secrets/scalars (`PORT`, `SOMA_CONNECT_IP`, `SUNSA_BASE_URL`, `SUNSA_API_KEY`, `SUNSA_ID_USER`, `RETRIES`). **Gitignored** - only `server/.env.example` (placeholder values) is committed. Not auto-loaded by `index.js` (no `dotenv`); for local runs, export it into the environment first: `set -a; source server/.env; set +a; node server/index.js`.

## Deployment

Runs in production as `shadesapi.service` on a Raspberry Pi (`pi@192.168.7.48:8081`), alongside several other small home-lab apps on that box (all Node, all deployed the same way). The Pi has no git - deploys are pushed from a dev machine.

**`./deploy.sh` is the only supported way to change what's running on the Pi.** Never hand-edit files under `/usr/local/shades/api` on the Pi directly - that creates drift between the repo and prod (this happened once with the old `application.yml`; see git history around 2026-08-14).

`./deploy.sh [--yes]` diffs `server/{index.js,devices.json,.env}` against what's currently live on the Pi, prints the diff for whatever changed, and asks for confirmation before pushing - skips the push and restart entirely if nothing changed (no pointless restart). Without a TTY (e.g. Claude running it), the prompt can't be answered - read the diff, then re-run with `--yes` only once you've confirmed it's intentional. Never pass `--yes` reflexively. There's no build step anymore; the script just copies files and restarts `shadesapi.service`.

The repo's `server/` files are the single source of truth - commit and push config/code changes to `main` before or after deploying; the script deploys whatever is on disk locally, it does not itself commit or push.

SSH to the Pi is mediated by a Bitwarden SSH agent - each command triggers a native approval popup on the user's screen. If a `deploy.sh` step hangs, that's usually why.

## Behavior notes worth knowing before touching `server/index.js`

- `/reopen` and `/reclose` (what Home Assistant's `lift_shades`/`lower_shades` rest_commands call) run a retry loop (`durablePosition`) that treats all devices as failed on the first pass, retries only the ones still failing, up to `RETRIES` times with linear backoff. Soma failure is `result === "error"`; Sunsa failure is judged by comparing the requested position against the reported position rounded to the nearest multiple of ten (`isNextHighestMultipleOfTen`) - Sunsa's real API only reports position in steps of 10.
- Sunsa's device list is cached in memory (invalidated on any position change, plus an hourly sweep) so `/getStates`/`/getBattery` don't hit the Sunsa API once per device per call.
- Response shapes intentionally use the *real* upstream field types/names (e.g. Soma's `position` as a number, Sunsa's `isConnected`), not whatever the old Java models happened to coerce them into - see git history around 2026-08-14 for the two cases where this was a deliberate decision, not an oversight.
