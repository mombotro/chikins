# Chicken Overlay App — Design Spec

Date: 2026-08-27

## Purpose

Android app that draws a small chicken farm sim as a system overlay on top
of the home screen and other apps (KitKat-and-up "desktop pet" style),
based on the browser sim at `/Users/juleah/Documents/websites/mombotro`
(chicken.js / egg.js / feed.js). Adds two behaviors mombotro does not have:
chicks growing into chickens, and chickens dying at a random age — so the
flock keeps cycling instead of accumulating forever or vanishing.

## Target platform

- `minSdk 19` (KitKat) — user has old KitKat devices in active use alongside
  modern ones.
- `targetSdk` latest stable.
- Overlay permission and window type branch on `Build.VERSION.SDK_INT`:
  - `< 23`: `SYSTEM_ALERT_WINDOW` is granted at install time, no runtime
    prompt.
  - `>= 23`: must request via `ACTION_MANAGE_OVERLAY_PERMISSION`
    (`Settings.canDrawOverlays`).
  - `< 26`: overlay window type `TYPE_PHONE`.
  - `>= 26`: overlay window type `TYPE_APPLICATION_OVERLAY`; the service
    must run as a foreground service with a persistent notification
    (channel required).

## Architecture

Native Kotlin, no WebView. One `OverlayService` (foreground on API 26+,
plain background service below that) owns:

- a `WindowManager` reference,
- the live entity list (chickens, chicks, eggs, feed piles, the feed bag),
- a ~30 fps tick loop via `Handler.postDelayed` (30 fps chosen over 60 —
  this runs continuously in the background, so battery cost matters more
  than animation smoothness).

Each entity is its own small `WindowManager`-added `ImageView`, sized to
its sprite frame and positioned via `updateViewLayout` with absolute x/y —
mirroring mombotro's absolutely-positioned sprite divs. Because no overlay
view covers the rest of the screen, the home screen and every other app
stay touchable everywhere except directly on a chicken/chick/egg/feed
element. This is why WebView (one big rectangle, either fully touchable or
fully passthrough) and live wallpaper (sits behind other apps, not on top)
were rejected in favor of this approach.

`MainActivity` is minimal: permission prompt (API 23+) plus a Start/Stop
toggle that starts/stops `OverlayService`.

## Assets

`chicken.PNG`, `chick.PNG`, `egg.PNG`, `feed.PNG`, `feed-bag.PNG` (already
staged in `assets/` in this project) move to `res/drawable`. At service
startup, each spritesheet is decoded once and sliced into individual
`Bitmap` frames using the same frame width/height as mombotro's
`CHICKEN_CONFIG` / `CHICK_CONFIG` / `EGG_CONFIG` / `FEED_CONFIG`
(`backgroundPosition` shift amount → `Bitmap.createBitmap(sheet, frameX, 0,
w, h)`). Each tick sets the current frame's `Bitmap` on the entity's
`ImageView`.

## Entity behavior

Ported from mombotro's `chicken.js` / `egg.js` / `feed.js`, same states and
probabilities (wander, idle, peck, jump, egg-lay roll, egg-sit roll,
chick follow/ride-parent/run-from-touch), with these additions:

- **Chicken death (new).** At birth, `deathAt = now +
  random(5min, 15min)`. Each tick, if `now >= deathAt`, the chicken is
  removed (view removed from `WindowManager`, entity dropped from the
  list). No death animation — removal is instant, matching mombotro's lack
  of any "dying" state to build on.
- **Chick growth (new).** At hatch, `growUpAt = now +
  random(1min, 3min)` — short enough that most chicks reach adulthood
  before a typical parent's 5-15min lifespan ends. On reaching `growUpAt`,
  the chick's view is removed and replaced with a full `Chicken` entity at
  the same position, with its own fresh `deathAt`.
- **Egg sitting time (adjusted).** mombotro's 10s sit requirement is too
  fast against a multi-minute chicken lifespan; scaled up to
  45-90s (randomized per egg) so hatching feels paced rather than instant.
- **Feed placement (adjusted for overlay touch model).** mombotro places
  feed via "click bag, then click ground" — not available here since
  empty screen space isn't a touchable overlay surface. Instead: the feed
  bag is a persistent draggable overlay icon docked in a screen corner.
  Press-drag-release it anywhere on screen to drop a feed pile at the
  release point; the bag animates back to its dock. Feed pile behavior
  (chickens rush in within `notifyRadius`, peck, pile depletes, sprite
  frame shifts by remaining amount, removed at 0) matches `feed.js`
  unchanged.

## Population regulation (new)

Keeps the flock between 5 and 15 individuals (chickens + chicks counted
together) so the screen never looks crowded or empty:

- **Below 5:** spawn a fresh adult `Chicken` directly at a random on-screen
  position, bypassing the egg cycle. Guarantees the flock can't go
  extinct even on a long run of bad luck or a burst of deaths.
- **5-15:** normal egg-lay → sit → hatch → grow cycle runs unmodified.
- **At or above 15:** chickens skip their egg-laying probability roll
  entirely until the count drops back under 15, so hatches taper off
  instead of overshooting the cap.

Population check runs once per tick alongside the rest of the game loop —
no separate timer.

## Testing

- Unit-testable in isolation (no Android framework dependency): death-time
  and growth-time randomization stay within configured bounds; population
  regulation transitions (spawn-below-min, cap-at-max, normal-band
  passthrough) given a fake entity-count input.
- Manual verification on-device (both a KitKat device and a modern
  device): overlay permission prompt flow, start/stop service, touch
  passthrough to home screen and another app while overlay is running,
  drag-drop feed bag placement, observe a full lay→sit→hatch→grow→death
  cycle without restarting the app.

## Out of scope

- No live wallpaper mode, no widget.
- No persistence of flock state across service restarts (fresh initial
  population each time the service starts, same as mombotro's page-load
  behavior).
- No sound, no telegram feed / room-navigation features from mombotro —
  those are the browser site's chrome, not part of the sim itself.
