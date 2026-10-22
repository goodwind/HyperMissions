# Contributing

Thanks for looking. Issues and pull requests are welcome.

## Reporting a problem

Open an issue and tell us:

- the TV model and its Android / firmware version (`adb shell getprop ro.product.model`,
  `ro.build.version.release`);
- what you did, what you expected and what happened;
- for a permission that did not change: the output of
  `adb shell dumpsys package <package> | grep -A3 "runtime permissions"` and, for a special access,
  `adb shell appops get <package>`.

Security problems: see [SECURITY](SECURITY.md), please not in a public issue.

## Translations

Strings are in `app/src/main/res/values*/strings.xml`: `values` is English, one folder per language. The
translations were prepared without native review, so corrections are especially welcome. A new
language is a new `values-<code>` folder with the same keys; keep the `%1$s` / `%2$d` placeholders and
use `’` rather than a bare `'` in text. Group names (`grp_*`) are the names of Android's permission
groups as that language's own Android calls them where you can check.

## Code

Kotlin and Jetpack Compose, no other dependencies.

```bash
./gradlew assembleRelease     # JDK 17, Android SDK platform 36
```

- Comments and identifiers in English. Every user-visible string in `values` (English) and, as you can,
  the other `values-*` folders.
- Controls the remote can reach are focusable rows or cards (see `Widgets.kt`); the interface is drawn on a
  960×540 dp canvas that is scaled to the screen (`canvasDensity`).
- The only way to change another app is `AdbShell`, and it should stay narrow: no method that takes a
  command string, names validated, a fixed list of operations. A change that widens it needs a reason
  in the pull request.
- Say what you tested and on which TV. Things you could not test on a real TV should be listed as untested.

## Releases

Maintainers sign a release with a key kept outside the repository (`local/signing.properties`, see the
README). Tag `vX.Y.Z`, attach `HyperMissions-X.Y.Z.apk` and its SHA-256, and add the release to
[CHANGELOG](CHANGELOG.md).

## License

By contributing you agree that your contribution is licensed under the [Apache License 2.0](LICENSE).
