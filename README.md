# BT Remote Watchdog

Root-based auto-fix for the remote-drops-for-a-second issue on Android 14
receivers. A foreground service listens for Bluetooth disconnect events and
immediately runs the same shell-level fix that already worked manually
(`svc bluetooth disable/enable`, disabling deep/light idle, enabling BLE
scan), using root access.

**Root is required.** Without root, the app cannot execute these commands —
a normal app process doesn't have shell-level permission. If the device
isn't rooted, the "Check Status" screen will show "Root access: not
available" and the auto-fix won't run.

## Getting the APK without Android Studio

You don't need Android Studio installed. GitHub can build it for you for
free:

1. Create a free account at github.com (skip if you already have one).
2. Create a new repository (any name, e.g. `bt-watchdog`).
3. On the repo page, click **Add file → Upload files**, then drag in
   everything from this folder (keeping the folder structure — GitHub's
   uploader preserves it if you drag the whole extracted project folder).
4. Commit the upload.
5. Go to the **Actions** tab of your repo — a workflow called "Build APK"
   will run automatically (takes a few minutes).
6. When it finishes (green checkmark), click into that run, scroll to
   **Artifacts**, and download **BTWatchdog-debug-apk** — that's your
   installable APK, zipped.

## Installing on the receiver

```
adb install app-debug.apk
```
or copy it to a USB drive / send via a file manager that supports
"install unknown apps".

## Using the app

1. Open it once, grant Bluetooth permissions when prompted.
2. Tap **Start Watchdog** — it runs as a foreground service (small
   persistent notification) and auto-restarts after every reboot.
3. Tap **Check Status** anytime to see: whether the watchdog is running,
   whether root access is granted, and whether Bluetooth is currently on.

## Distributing to others

Share the downloaded `app-debug.apk` — anyone with the same rooted
receiver/remote can sideload it the same way.
