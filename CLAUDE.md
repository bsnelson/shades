# shades

A small Node.js API that unifies control of the house's window shades behind one HTTP interface, so Home Assistant and other callers don't need to know which vendor a given shade is. Two vendors are live today:

- **Zigbee** - Current Luxury E-Wands (OfficeLeft, OfficeMiddle, KitchenSide), driven over MQTT via the Zigbee2MQTT/Mosquitto stack on the Synology NAS.
- **Sunsa** - the remaining wands, driven via Sunsa's cloud API.

Soma, the original vendor, was fully replaced by the Zigbee E-Wands in 2026-09 and its client code was removed on 2026-10-22 (see git history if ever needed).

Originally a Spring Boot app; rewritten in Node on 2026-08-14 to match the sibling apps on its Pi (`roasts`, `brews`, `baskets`, `sonos-http-api`) and to stop fighting that box's limited RAM - the JVM took 60-145s+ to boot under memory pressure. The old Java source lives in git history (see the commit that removed `src/`, `build.gradle.kts`, etc.) if ever needed again.

## Structure

- `server/index.js` - the entire app. Written to run as-is on the Pi's Node 12, no build step.
- `server/package.json` - one dependency, `mqtt@4.3.8` (pinned: v5 needs a newer Node than the Pi has). `server/node_modules` is gitignored and vendored by `deploy.sh` (see below).
- `server/devices.json` - the device list and vendor API path templates. Structural config, committed to git. Per device:
  - `name` - the app's name for the shade (`KitchenSide`), used by `?name=` and in responses.
  - `id` - the vendor's id: the Zigbee2MQTT friendly name for zigbee (`kitchen_side`), the numeric device id for Sunsa.
  - `type` - `zigbee` / `sunsa`.
  - `seasonalDefault` - where `/reopen` sends this shade (app position scale, see below).
  - `groups` - names usable with `?group=`. Every device is in `All`. The `Seasonal` group is just a label: `/reopen` applies `seasonalDefault` to *every* device regardless of group.
- `server/.env` - secrets/scalars (`PORT`, `SUNSA_BASE_URL`, `SUNSA_API_KEY`, `SUNSA_ID_USER`, `RETRIES`, `MQTT_BROKER_URL`, `MQTT_USERNAME`, `MQTT_PASSWORD`). **Gitignored** - only `server/.env.example` (placeholder values) is committed. Not auto-loaded by `index.js` (no `dotenv`); for local runs, export it into the environment first: `set -a; source server/.env; set +a; node server/index.js`.

## Position conventions (read before touching any position number)

- **This app: 0 = open, 100 = closed** (higher = more closed). `seasonalDefault`, `/setPosition/N`, and `/getStates` all use this scale - Soma's native convention, which the app inherited.
- **Zigbee2MQTT: 0 = closed, 100 = open.** `index.js` flips on the way out (`zigbeeSetShadePosition`: `100 - position`) and on the way back (`zigbeeToAppPosition`), so callers only ever see the app scale. Both flips are needed - this was got wrong twice before settling (see git history around 2026-08-25/26).
- **Home Assistant / Alexa: 0 = closed, 100 = open** (percent open). HA's template covers convert with `100 - raw_position`. Deliberately kept HA-standard, so "Alexa, set kitchen side to 70%" means 70% *open*.
- Sunsa reports position only in steps of 10.

## Endpoints

| Endpoint | Does |
|---|---|
| `/reopen` | Every device to its `seasonalDefault`, via the `durablePosition` retry loop |
| `/reclose` | Every device to `100` (closed), same retry loop |
| `/setPosition/{N}?name=X` or `?group=X` | One device or one group to N. **Use `?name=` for testing** - `/reopen`/`/reclose` move every shade in the house |
| `/getStates`, `/getBattery` | Current position / battery per device |
| `/listNames`, `/listDevices` | Configured devices (`deploy.sh` polls `/listNames` as its health check) |

Home Assistant (`configuration.yaml` on the HAOS VM) calls: `rest_command.lift_shades` → `/reopen` and `lower_shades` → `/reclose` (the whole-house "Shades" cover), `set_{kitchen_side,office_left,office_middle}_position` → `/setPosition/...?name=...` (individual template covers), plus a `rest` sensor polling `/getStates`. Sunsa shades have no individual HA entity.

## Deployment

Runs in production as `shadesapi.service` on a Raspberry Pi (`pi@192.168.7.48:8081`), alongside several other small home-lab apps on that box. The Pi has no git and no npm - deploys are pushed from a dev machine.

**`./deploy.sh` is the only supported way to change what's running on the Pi.** Never hand-edit files under `/usr/local/shades/api` on the Pi directly - that creates drift between the repo and prod (this happened once with the old `application.yml`; see git history around 2026-08-14).

`./deploy.sh [--yes]` runs `npm install` locally in `server/`, diffs `server/{index.js,devices.json,package.json,.env}` and a content checksum of the `node_modules` tree against what's live on the Pi, prints the diff for whatever changed, and asks for confirmation before pushing - skips the push and restart entirely if nothing changed. Without a TTY (e.g. Claude running it), the prompt can't be answered - read the diff, then re-run with `--yes` only once you've confirmed it's intentional. Never pass `--yes` reflexively.

The repo's `server/` files are the single source of truth - commit and push config/code changes to `main` before or after deploying; the script deploys whatever is on disk locally, it does not itself commit or push.

SSH to the Pi is mediated by a Bitwarden SSH agent - each new connection triggers a native approval popup on the user's screen. `deploy.sh` multiplexes all its ssh/scp calls over one connection (closed when the script exits), so expect one popup per run. If a `deploy.sh` step hangs, an unanswered popup is usually why.

## Behavior notes worth knowing before touching `server/index.js`

- `/reopen` and `/reclose` run a retry loop (`durablePosition`) that treats all devices as failed on the first pass, retries only the ones still failing, up to `RETRIES` times with linear backoff. Failure is judged per vendor: Zigbee by `result === "error"`; Sunsa by comparing the requested position against the reported one rounded up to the next multiple of ten (`isNextHighestMultipleOfTen`).
- Zigbee: subscribes to `zigbee2mqtt/+` and caches each device's last state message. A set publishes to `zigbee2mqtt/<id>/set` and **waits up to 15s for Z2M to report the target position** before resolving, so the retry loop gets a real success/failure signal. Z2M's device state topics aren't retained on this broker, so right after a restart `/getStates`/`/getBattery` are empty for Zigbee devices until each one next reports - harmless.
- Zigbee devices driven by this app are excluded from Home Assistant's own Zigbee2MQTT discovery (`homeassistant: null` per device in Z2M's `configuration.yaml` on the NAS), so this app stays the only controller. A newly migrated Zigbee shade needs that exclusion added too.
- Sunsa's device list is cached in memory (invalidated on any position change, plus an hourly sweep) so `/getStates`/`/getBattery` don't hit the Sunsa API once per device per call.
- Response shapes intentionally use the *real* upstream field types/names (e.g. Sunsa's `isConnected`), not whatever the old Java models happened to coerce them into - see git history around 2026-08-14.
