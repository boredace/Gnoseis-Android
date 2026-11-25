# Gnoseis
Gnoseis is a high performance Android native CRM application that can be also used as an all-purpose knowledge manager. It stores all data securely on device and in future will allow secure cloud-free synchronization.

Gnoseis is also available [for Windows](https://github.com/boredace/Gnoseis-Windows). Both apps use the same database and backup format, so a database or backup can move between them: Android 0.4.0 and Windows 0.2.0 open each other's files, encrypted backups included. See [Database and compatibility](#database-and-compatibility) for which versions work together, and [docs/windows-android-differences.md](docs/windows-android-differences.md) for what differs between the two apps.

<p align="center">
  <img src="docs/readme-screens.png" alt="Gnoseis in dark mode: the Home page with the most linked records, a contact with linked-record tabs, and search results with a best match" width="880">
</p>

More screenshots of the current release are in [screenshots](screenshots/).

## Features

- **Five kinds of records**: contacts, organizations, notes, categories and items. Each type has its own colour, used everywhere in the app.
- **Contacts**: first and last name, job title, company, main, mobile, home and work phone numbers and email addresses, and free-form comments. The list is sorted A–Z with letter headings.
- **Notes**: a title, text, and the day and time they were written, shown in your device's time zone. When editing, set the day and time with a date and time picker that follows your device's 12- or 24-hour setting. The list shows the newest first, grouped by the day they were written.
- **Organizations, categories and items**: a name and comments, for companies, tags such as "Clients" or "Follow-up", and things you want to keep track of.
- **Home page**: search, add a new record of any type, see your most linked records as a bubble chart (bigger bubble, more links), with **See all** for the full list of linked records, ranked by their number of links and filtered by type as on Home, and your latest notes, and get an overview with a count of each record type and a chart of the notes you wrote over the last two weeks. Home also points out notes that aren't linked to anything yet, and reminds you when your data has changed since your last backup.
- **Link anything to anything**: connect a note to the people, organizations, categories and items it is about. Each record's page has a tab for every type of linked record, with a count, so you can go from a contact to their notes, or from a category to everything in it. Long-press a linked record to unlink it.
- **Create linked records in one step**: from any record, add a new linked note, contact, category or item, or search for existing records to link, picking several across searches and linking them all at once. The page always shows which record the new or selected records will be linked to.
- **Search** across all record types by name or title, from Home or from any list page. Results show each record's type colour and a line of context, the best match comes first, and filter chips with match counts narrow them to one or more types. Before you type, search shows the records you changed most recently. If what you're looking for isn't there, Gnoseis offers to create a new record with the name you typed.
- **Backup and restore**: export the whole database to a file in a location you choose, and import it again later or on another device. Import checks that the file is a valid Gnoseis backup, upgrades backups from older versions, and leaves your data untouched if anything is wrong. Backups can be encrypted with a backup passphrase, either 20 generated words or one you choose. A passphrase you choose isn't checked for strength yet, so the 20 generated words are recommended. An encrypted backup is encrypted with AES-256, can only be opened with its passphrase, and uses no cryptography that quantum computers are known to break. A lost passphrase can't be recovered. Plain backups still work as before. Backups, encrypted or plain, can also be restored in Gnoseis for Windows 0.2.0 or later. The file format is described in [docs/backup-format.md](docs/backup-format.md).
- **Private by design**: all data stays on your device. The app has no internet permission, and Android cloud backup and device-to-device transfer are turned off for its data.

What changed in each version is in the [release notes](https://github.com/boredace/Gnoseis-Android/releases).

## Database and compatibility

Gnoseis keeps all data in one SQLite database. Its schema version is stored in the file as `PRAGMA user_version`, and a backup has the version of the database it was made from.

| Schema version | Written by |
|---|---|
| 1 | Android before 0.2.4 |
| 2 | Android 0.2.4 and 0.2.5 |
| 3 | Android 0.3.0, and Windows 0.1.0 |
| 4 | Android 0.4.0, and Windows 0.2.0 |

Android 0.4.0 opens versions 1 to 4, and upgrades older databases and backups to version 4. A backup from a newer version is refused with "Made by a newer version of Gnoseis", and nothing is changed. Android before 0.4.0 can't open a version 4 file or restore an encrypted backup, so update to 0.4.0 before copying a file from Windows 0.2.0 to Android.

**3 to 4:** the tables don't change; only note times do. Until version 4, a note's time (`createDateTime`) held the local date and time it was written. It becomes that moment in UTC, converted once in the time zone of the device that upgrades the database or restores the older backup. The note's day (`createDate`) becomes the UTC moment of 00:00 of its local day, and a note with no time of day gets 00:00 of its day. Every other column is unchanged.

Before upgrading its database to version 4, the app keeps a copy of it as it was, `gnoseis_data.before-v4-<date>_<time>`, next to it in the app's private storage. The app never deletes it, but other apps and file managers can't reach it. To keep a copy you can open, export a backup before updating.

## Roadmap
The next couple releases are planned using Milestones. To track progress see [here](https://github.com/boredace/Gnoseis-Android/milestones?direction=asc&sort=title&state=open).

## Download

Gnoseis needs Android 10 or later.

Signed APKs are attached to each [GitHub release](https://github.com/boredace/Gnoseis-Android/releases). To check that an APK is genuine, compare its signing certificate with this SHA-256 fingerprint:

```
SHA-256: FA:F6:85:2E:31:BF:F2:DA:5C:1E:92:85:55:E7:71:F5:D4:C1:34:0E:C5:6D:E1:E9:6B:C3:44:53:11:E4:05:4C
```

You can read it with `apksigner verify --print-certs <apk>`.

Copies from different sources (GitHub, and later Google Play and F-Droid) are signed with different keys, so one can't update another. To switch, export a backup in the app, uninstall, install the new copy, then import the backup.

## Building

You need the Android SDK and a Java 21 installation. The build runs Gradle on Java 21 (set in `gradle/gradle-daemon-jvm.properties`); any installed Java can start `./gradlew`, and Gradle finds the Java 21 by itself. A JetBrains Runtime 21 downloaded through Android Studio works, as does any other JDK 21. To build a debug APK:

```
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`. Debug builds use the package name `org.gnoseis.debug`, so they install next to a release build. Release builds need a signing key; see [RELEASING.md](RELEASING.md). Contribution rules are in [CONTRIBUTING.md](CONTRIBUTING.md).

## Licensing

Gnoseis is dual-licensed:

1. **Open-Source License**: Available under the [GNU General Public License v3.0](LICENSE). This allows you to use, modify, and distribute the software freely in open-source projects.
2. **Commercial License**: For proprietary, closed-source, or commercial use, a commercial license is required. See [COMMERCIAL_LICENSE](COMMERCIAL_LICENSE). This file includes contact information.

By contributing to this project, you agree to license your contributions under both licenses.

### Third-party components

- [Bouncy Castle](https://www.bouncycastle.org), for Argon2id: MIT License, see [third_party/bouncycastle/LICENSE.md](third_party/bouncycastle/LICENSE.md).
- The [EFF large word list](https://www.eff.org/dice), for generated backup passphrases: © Electronic Frontier Foundation, CC BY 4.0, see [third_party/eff-wordlist/NOTICE.txt](third_party/eff-wordlist/NOTICE.txt).
- The [Fraunces](https://github.com/undercasetype/Fraunces) font, on the splash screen: SIL Open Font License 1.1, see [third_party/fonts/Fraunces-OFL.txt](third_party/fonts/Fraunces-OFL.txt).

The screenshots show fictional sample data.
