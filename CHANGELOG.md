# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/).

## [1.1.1] - 2026-10-28

### Fixed

- Updating from inside the app crashed on TV firmware whose installer has no confirmation screen for session
  installs (Xiaomi, Android 11). The file now goes to the installer's ordinary "open an APK" screen there.
- After leaving the installer without installing, the update button stayed on "Handing over to the installer…";
  it now offers the update again.

## [1.1.0] - 2026-10-28

### Changed

- A new layout in two columns: the apps on the left (icon, name, how many permissions are on), the app under the
  focus on the right with its actions and all its permissions and special accesses as sliders (a slider in the
  middle: some of a kind are on).
- The filters are two sliders over the list: *Show system apps*, and *Only apps asking for permissions* (on by
  default, hides the apps with nothing to switch).

### Added

- Updates: on opening, the app asks GitHub for the latest release; a newer one is offered with a button that fills
  as a progress bar while the file comes. The file is checked against the SHA-256 GitHub announced for it, then
  handed to Android's installer, which asks for the confirmation. 1.0.0 has no updater, so install this one by hand.
- The icon before the title, and the author, licence and GitHub link at the bottom.

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
