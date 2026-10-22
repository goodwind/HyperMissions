# Architecture

Kotlin and Jetpack Compose, one module, no other dependencies. The interface is drawn on a 960×540 dp
canvas that `canvasDensity` scales to the screen, so it looks the same on a 1080p TV at 240 or 320 dpi.

```
app/src/main/java/io/github/goodwind/hypermissions/
├── MainActivity.kt     the launcher entry: the grid of apps; also tells RequestEntry to look again
├── ListScreen.kt       the grid of cards (icon, name, "On: n/m"), the Installed / All filter, focus restore
├── DetailPanel.kt      one app: actions, permissions, special accesses; the compact "what is off" form
├── Widgets.kt          FocusRow / ActionButton / AccessRow (remote focus = a light plate), glyphs, colors
├── AppDetails.kt       reads an app: version, dates, runtime permission groups, special accesses
├── StandardGroups.kt   Android's permission groups with names in our languages
├── AdbShell.kt         the narrow ADB client to the TV's own adbd (the only thing that changes other apps)
├── RequestActivity.kt  the panel over the app that asked for a settings screen; its own task
├── RequestEntry.kt     the activity-alias that answers those requests, on only when nothing else does
├── Actions.kt          Open and Uninstall
└── GlyphPaths.kt       Material Symbols paths (Apache 2.0)
```

## Reading and changing

- **Reading** needs no privilege: `PackageManager` for permissions and their groups, `AppOpsManager` for
  the special accesses (Android 10+).
- **Changing** goes through `AdbShell`, which speaks ADB protocol version 1 to `127.0.0.1:5555`
  (CNXN, AUTH with the app's RSA key, OPEN `shell:`). It has no method that takes a command; each
  method builds one of a few fixed commands from names that match a pattern.

## Answering the settings screens apps ask for

Some firmware has no handler for `ACTION_APPLICATION_DETAILS_SETTINGS` or the special-access screens, so
the asking app fails. `RequestEntry` is an `activity-alias` (disabled in the manifest) with those
intent filters. `RequestEntry.update` turns it on only when no other app answers the App Info intent
(neither the firmware nor another app standing in for it), at launch and at boot; two answers would make
Android ask which to use every time.

It points to `RequestActivity`: `singleInstance`, its own task affinity, a translucent theme, excluded
from Recents. Alone in its task over the asking app, it closes with `finishAndRemoveTask`, so Back lands
in the app that asked. Hosted in the launcher activity instead, it ended up in the asking app's task, and
that app's tile then opened the wrong thing after the permission change killed the app.

The panel shows what is off when asked (`requestOnly`), or only the access the intent names, with a way
on to the whole panel.

## Things found out on the way

- `pm clear` on Android 11 has no `--cache-only`; it ignores the option and wipes all the data.
- All files access (`MANAGE_EXTERNAL_STORAGE`) is a uid-level app op: `appops set --uid`.
- Some firmware reports storage and contacts permissions in no group, and has the permission labels in
  English only; hence `StandardGroups`.
- Changing a permission makes Android restart the app.
