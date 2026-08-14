# shades

A Spring Boot (WebFlux, Java 11 target) API that unifies control of Soma Smart Shades and Sunsa shades behind one HTTP interface, so Home Assistant and other callers don't need to know which vendor a given shade is.

## Deployment

Runs in production as `shadesapi.service` on a Raspberry Pi (`pi@192.168.7.48:8081`), alongside several other small home-lab apps on that box. The Pi has no git and can't build the project itself (old armv6l hardware, ~50s just to boot the JVM) - all deploys are pushed from a dev machine.

**`./deploy.sh` is the only supported way to change what's running on the Pi.** Never hand-edit `/usr/local/shades/api/application.yml` on the Pi directly - that creates drift between the repo and prod (this happened once; see git history around 2026-08-14 for the cleanup).

- `./deploy.sh` - full deploy: builds the jar (`./gradlew clean bootJar`), copies both the jar and `src/main/resources/application.yml` to the Pi, repoints the `shades-LATEST.jar` symlink, restarts the service, and polls `/listNames` until it responds.
- `./deploy.sh config` - config-only deploy: skips the build, just pushes `application.yml` and restarts. Use this for things like adding/removing a device or changing a `seasonalDefault`.

Both modes treat `src/main/resources/application.yml` in the repo as the single source of truth - whatever's in the repo is what ends up live after running the script. Commit and push config changes to `main` before or after deploying; the script deploys whatever is on disk locally, it does not itself commit or push.

**Before touching the live `application.yml`, the script pulls the Pi's current copy and diffs it against the local file.** If they match, it skips the config push and restart entirely (no pointless ~2min reboot on that slow box). If they differ, it prints the diff and asks for confirmation before overwriting - pushing a value you didn't mean to (like the wrong `server.port`, which took prod down once on 2026-08-14 when a leftover local-dev override got deployed as-is) is exactly what this catches. Without a TTY (e.g. Claude running it), the prompt can't be answered - read the diff, then re-run with `--yes` only once you've confirmed it's intentional. Never pass `--yes` reflexively.

Prod's `application.yml` has diverged from the repo's before (the `server.port` incident) and could again for reasons that are actually correct for that environment - if the diff ever shows something that looks deliberate rather than stale, stop and ask before overwriting it.

SSH to the Pi is mediated by a Bitwarden SSH agent - each command triggers a native approval popup on the user's screen. If a `deploy.sh` step hangs, that's usually why.
