# chikins

chikins is an Android app. It draws a small chicken farm on top of your home screen and other apps. Chickens wander, eat feed, lay eggs, sit on eggs, and hatch chicks. Chicks grow into chickens. Chickens die at a random age. The app keeps the flock size steady, between 1 and a limit you set.

This project is a port of the browser game mombotro. The live site is at https://boccbo.cc. The source is at https://github.com/mombotro/mombotro.github.io.

## Requirements

- Android 4.2 (Android API level 17) or later. Android assigns an API level number to each release. This app declares API level 17 as its minimum, but see "Tested versions" below.
- Permission to draw over other apps. The app asks for this permission on first use.

## Build

1. Clone this repository.
2. Open a terminal in the project folder.
3. Run `./gradlew assembleDebug` for a debug build, or `./gradlew assembleRelease` for a release build.
4. Find the APK under `app/build/outputs/apk/`.

## Install and run

1. Install the APK on your device: `adb install app/build/outputs/apk/debug/app-debug.apk`.
2. Open the chikins app.
3. Tap "start chikins".
4. On Android 6.0 and later, the system asks for the "draw over other apps" permission. Grant it. The overlay starts automatically.
5. Tap "stop chikins" to stop the overlay, or use the notification action.

## Feed

Drag the feed bag to move it. Tap the feed bag, then tap anywhere on the screen to drop feed there. Chickens near the feed pile walk to it and eat. Chicks that eat near a feed pile mature faster.

## Settings

The main screen has two controls below the start button.

- "max chikins": a slider that sets the largest allowed flock size.
- "cull all but one chikin": a button that removes all chickens, chicks, and eggs except one chicken. The persistent notification also has this action.

## Known simplifications

- The jump ignores nearby chickens and other objects. It never blocks anything, because the overlay uses one small window per chicken.
- Egg hatching shows five frames: egg, cracked, peeking out, broken free, and empty shell. The empty shell stays for two seconds, then the egg view disappears.
- Feed placement uses two gestures: drag and drop, or tap the bag and then tap the ground. The browser version of mombotro uses a single click-then-click gesture instead. The overlay app cannot capture a tap anywhere on the screen without a temporary full-screen window, so both gestures exist for convenience.

## Tested versions

Automated tests cover the simulation logic only: population limits, chicken lifespan, egg timing, and similar rules. Manual, on-device tests cover two points only:

- Android 10 (API level 29), on a real Samsung Galaxy Note9.
- Android 14 (API level 34), on an emulator.

**This app is untested on all other Android versions, including every version earlier than Android 10.** The app declares a minimum API level of 17 (Android 4.2, Jelly Bean) for broad compatibility. Nobody tested it on a device between API level 17 and API level 28.

The overlay permission flow changed more than once in that range. It needs no runtime prompt before API level 23. It needs a runtime prompt from API level 23. It needs a different window type and a foreground-service notification from API level 26. If you run this app on an older device, watch for permission or window-type errors, and report them.

## License

No license file yet. Treat this as "all rights reserved" until one is added.
