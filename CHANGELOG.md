# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/).

## [1.0.0] - 2026-10-22

First release.

### Added

- The apps as a grid of cards with icon, name and how many permissions are on (`On: 3/4`); *Installed*
  and *All*.
- A panel per app: version and install date; **Open**, **Force stop**, **Clear data** (two presses) and
  **Uninstall**; runtime permissions by kind, in the TV's language; special accesses (All files access,
  Display over other apps, Change system settings, Install unknown apps).
- Answers the settings screens that apps send people to (App Info and the four special accesses) with a
  translucent panel over the asking app, showing what is off; Back returns to that app. The entry is off
  until nothing else on the TV answers the App Info request.
- Permissions are changed through the TV's own `adbd` on `127.0.0.1:5555` with a key of the app's own,
  with a narrow command set (`pm grant|revoke`, `appops set`, `am force-stop`, `pm clear`).
- 20 languages: English, Russian, Ukrainian, Kazakh, Simplified and Traditional Chinese, Spanish,
  Portuguese, French, German, Italian, Japanese, Korean, Arabic (right to left), Hindi, Turkish, Polish,
  Indonesian, Vietnamese, Dutch.

### Known limits

- No *Clear cache*: `pm clear` on Android 11 has no `--cache-only` and would wipe all data.
- Tested on one TV only: Xiaomi MiTV-MFTR0 (HyperOS, Android 11).

[1.0.0]: https://github.com/goodwind/HyperMissions/releases/tag/v1.0.0
