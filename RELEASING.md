# Releasing Gnoseis

## Version numbers

`versionName` follows `MAJOR.MINOR.PATCH`, with `-dev.N` for dev builds and `-beta.N` for betas. `versionCode` is derived from it:

```
versionCode = MAJOR × 1,000,000 + MINOR × 10,000 + PATCH × 100 + NN
```

NN counts the pre-releases of a version (01–98), dev builds and betas together: each new dev build or beta takes the next free NN. **99** means the final release, so every final release is above its pre-releases.

| Version | versionCode |
|---|---|
| 0.2.4-beta.3 | 20_403 |
| 0.2.4 | 20_499 |
| 0.3.0-dev.1 | 30_001 |
| 0.3.0-beta.1 (after dev.1 and dev.2) | 30_003 |
| 1.2.3 | 1_020_399 |

Android only accepts updates with a higher versionCode, so each release must use a larger number than the one before.

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

## Dev builds

Dev builds are debug builds for testing work toward the next milestone. They are signed with the debug key and are never published as GitHub releases.

1. On a `release/<version>` branch from `develop`, set `versionName` to `<next version>-dev.N` and `versionCode` to the next free NN, then update the drawer label in `NavigationDrawerContent.kt`.
2. Run the unit tests, then open a pull request named `Version <version>` into `develop` and merge it. Don't tag it.
3. Build with `./gradlew assembleDebug` and keep the APK as `Gnoseis-v<version>.apk`.

## Making a release

1. On a `release/<version>` branch from `develop`, set `versionName` and `versionCode` in `app/build.gradle.kts`, then update the drawer label in `NavigationDrawerContent.kt`.
2. Run the unit tests:
   ```bash
   ./gradlew :app:testDebugUnitTest
   ```
3. Open a pull request named `Release <version>` into `develop` and merge it.
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
8. Close the milestone.

Gradle 8.9 needs Java 17 to 22, and Java 21 is the version it's tested with. Recent Android Studio versions come with Java 25, which Gradle 8.9 can't run on. Point `JAVA_HOME` at a Java 21 installation, for example a JetBrains Runtime 21 downloaded through Android Studio.

## Distribution channels

Each channel signs with its own key, so a copy from one channel can't be updated with an APK from another. To switch channels: Export a backup, uninstall, install the new copy, then Import.

Separate builds per channel (`play`, `fdroid`, `github`) are tracked in #36. F-Droid builds should only follow final tags (`^v[0-9]+\.[0-9]+\.[0-9]+$`), so betas don't reach F-Droid users.
