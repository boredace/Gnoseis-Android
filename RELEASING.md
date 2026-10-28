# Releasing Gnoseis

## Version numbers

`versionName` follows `MAJOR.MINOR.PATCH`, with `-beta.N` for betas. `versionCode` is derived from it:

```
versionCode = MAJOR × 1,000,000 + MINOR × 10,000 + PATCH × 100 + NN
```

NN is the beta number (01–98). **99** means the final release, so every final release is above its betas. **00** is the version `main` carries while the next version is being built, before its first beta.

| Version | versionCode |
|---|---|
| 0.2.4-beta.3 | 20_403 |
| 0.2.4 | 20_499 |
| 0.3.0 (on `main`, not released yet) | 30_000 |
| 0.3.0-beta.1 | 30_001 |
| 1.2.3 | 1_020_399 |

Android only accepts updates with a higher versionCode, so each release must use a larger number than the one before.

`main` always carries the version it is heading for. Right after a final release, `main` moves to the next version (see [Making a release](#making-a-release)), so nothing built from `main` shows an older version than the work it contains.

Debug builds add where they were built from to the version, using `git describe`: the number of commits since the last release tag, the commit, and `.dirty` if there were uncommitted changes. For example, `0.3.0-debug.12+a1b2c3d` is 12 commits after the last release tag, at commit `a1b2c3d`. The drawer and Android's App info show the full version. Release builds show the plain version.

## Branches

Gnoseis uses [GitHub Flow](https://docs.github.com/en/get-started/using-github/github-flow). `main` is the only long-lived branch.

- Every change is a short-lived branch from `main`, named after its issue (`feature/45-home-dashboard`, `fix/42-filter-button`), merged into `main` with a pull request.
- A release is a tag on `main`. APKs for users are only ever built from a tag, never from the tip of `main`.

## Signing

GitHub releases are signed with the maintainer's release key. The key and its password are never committed: `.gitignore` excludes `*.jks`, `*.keystore` and `*.p12`.

The build reads these values from `~/.gradle/gradle.properties` first, then from environment variables:

| Name | Value |
|---|---|
| `GNOSEIS_GITHUB_STORE_FILE` | Path to the keystore |
| `GNOSEIS_GITHUB_KEY_ALIAS` | Key alias |
| `GNOSEIS_GITHUB_STORE_PASSWORD` | Keystore password |
| `GNOSEIS_GITHUB_KEY_PASSWORD` | Optional. Defaults to the store password, which is what PKCS12 keystores use |

Keep the passwords out of `gradle.properties`. Values in that file override environment variables. A typical PowerShell session:

```powershell
$env:GNOSEIS_GITHUB_STORE_FILE = "<path to keystore>"
$env:GNOSEIS_GITHUB_KEY_ALIAS = "<alias>"
$pw = Read-Host -AsSecureString "Keystore password"
$env:GNOSEIS_GITHUB_STORE_PASSWORD = [System.Net.NetworkCredential]::new('', $pw).Password
Remove-Variable pw
```

Debug builds and unit tests don't need any of this. A release build that is missing a value stops and names the missing value.

Debug builds use the application ID `org.gnoseis.debug` and are named "Gnoseis Debug", so they install next to a release build.

## Making a release

1. On a `release/<version>` branch from `main`, set `versionName` and `versionCode` in `app/build.gradle.kts` to the release, for example `0.3.0-beta.1` and `30_001`.
2. Run the unit tests:
   ```bash
   ./gradlew :app:testDebugUnitTest
   ```
3. Open a pull request named `Release <version>` into `main` and merge it.
4. Tag the merge commit `v<version>` and push the tag.
5. Build the signed APK from the tag:
   ```bash
   ./gradlew assembleRelease
   ```
   The APK is written to `app/build/outputs/apk/release/app-release.apk`.
6. Verify the signature. The SHA-256 digest must match the fingerprint in the README:
   ```bash
   apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
   ```
7. Create the GitHub release for the tag and attach the APK as `Gnoseis-v<version>.apk`. Mark betas as pre-releases.
8. After a final release, open a pull request named `Start <next version>` that sets `main` to the next version with NN = 00, for example `0.4.0` and `40_000`. After a beta, `main` keeps the beta's version until the next release.
9. Close the milestone.

### Fixing an older release

If a released version needs a fix while `main` already holds unreleased work, branch `hotfix/<version>` from that release's tag, set the patch version (for example `0.3.1` and `30_199`), fix it, and release from that branch the same way: tag, build, GitHub release. Then bring the fix into `main` with a pull request. Releases with nothing unreleased on `main` don't need this: they go through `main` as usual.

Gradle 8.9 needs Java 17 to 22, and Java 21 is the version it's tested with. Recent Android Studio versions come with Java 25, which Gradle 8.9 can't run on. Point `JAVA_HOME` at a Java 21 installation, for example a JetBrains Runtime 21 downloaded through Android Studio.

## Distribution channels

Each channel signs with its own key, so a copy from one channel can't be updated with an APK from another. To switch channels: Export a backup, uninstall, install the new copy, then Import.

Separate builds per channel (`play`, `fdroid`, `github`) are tracked in #36. F-Droid builds should only follow final tags (`^v[0-9]+\.[0-9]+\.[0-9]+$`), so betas don't reach F-Droid users.
