# chikins

chikins is an Android app. It draws a small chicken farm on top of your home screen and other apps. Chickens wander, eat feed, lay eggs, sit on eggs, and hatch chicks. Chicks grow into chickens. Chickens die at a random age. The app keeps the flock size steady, between 1 and a limit you set.

This project is a port of the browser game mombotro. The live site is at https://boccbo.cc. The source is at https://github.com/mombotro/mombotro.github.io.

## Requirements

- Android 4.1 (Android API level 16) or later. Android assigns an API level number to each release. This app declares API level 16 as its minimum, but see "Tested versions" below.
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

Automated tests cover the simulation logic only: population limits, chicken lifespan, egg timing, and similar rules. Manual, on-device tests cover four points:

- Android 4.1.2 (API level 16), on a real LG VS950. This is the app's declared minimum, and confirms the pre-API-23 path: the "draw over other apps" permission is granted at install time, with no runtime prompt at all.
- Android 7.1.1 (API level 25), on a real Barnes & Noble Nook HD (CyanogenMod). This confirms the API-23-to-25 path: a runtime permission prompt, but still the older `TYPE_PHONE` overlay window type, and no foreground service or notification.
- Android 10 (API level 29), on a real Samsung Galaxy Note9. This confirms the API-26-and-later path: `TYPE_APPLICATION_OVERLAY`, plus the foreground service and its notification.
- Android 14 (API level 34), on an emulator.

**This app is untested between API level 17 and API level 24, and between API level 26 and API level 28.** It is also untested above API level 34. If you run this app on a device in one of those gaps, watch for permission or window-type errors, and report them.

## License

No license file yet. Treat this as "all rights reserved" until one is added.
