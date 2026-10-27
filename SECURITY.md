# Security

## What the app can do, and what it needs for it

HyperMissions changes another app's permissions through the TV's own `adbd`. For that it keeps an RSA key
of its own in the app's private storage (`files/adb_priv`, `files/adb_pub`). The TV asks once to
authorize that key ("Allow USB debugging?"), and from then on a process that holds the key can run
shell commands on that TV, over the network if ADB is reachable from outside the TV.

So the key is worth protecting, and the app's code is kept narrow on purpose:

- The app sends only `pm grant|revoke`, `appops set`, `am force-stop` and `pm clear`
  ([`AdbShell.kt`](app/src/main/java/io/github/goodwind/hypermissions/AdbShell.kt)). Package and
  permission names must match a dotted-identifier pattern, the `appops` operation comes from a fixed
  list, and no API accepts a command string.
- It connects to `127.0.0.1` only.
- The key never leaves the TV and is not logged.
- The panel that answers other apps' settings requests (`RequestActivity`) is not exported; only the
  `RequestEntry` alias is, and it takes a package name from the intent and nothing else. What that
  package's permissions then are can be changed only by the person using the remote.

## Updates

The app asks `api.github.com` for the latest release of `goodwind/HyperMissions`, takes only a file under that
repository's own `releases/download`, refuses it unless its SHA-256 equals the digest GitHub announced for it, and
hands it to Android's package installer, which asks you to confirm and refuses a file not signed with the key of the
copy it replaces. That needs the permission to install packages, which the app declares and the TV asks about.

## Limits you should know about

- Anything that can read the app's private files (root, a backup of the app data) can use the key
  against that TV while the authorization stands.
- The TV's `adbd` listening on the network is the TV's own setting. Anyone on the network who has an
  authorized key can use it, with or without this app. Turn network ADB off when you are not using it
  if that worries you.
- `Clear data` and `Uninstall` are destructive. `Clear data` asks for a second press.

## Taking the authorization back

- Uninstall HyperMissions (this deletes the key), or
- in the TV's developer options, **Revoke USB debugging authorizations**.

## Reporting a vulnerability

Please do not open a public issue for a security problem. Use GitHub's
[private vulnerability reporting](https://github.com/goodwind/HyperMissions/security/advisories/new)
for this repository. You will get a first answer within about a week. If the report holds, a fix is
released and credited to you, if you wish.

Supported: the latest release.
