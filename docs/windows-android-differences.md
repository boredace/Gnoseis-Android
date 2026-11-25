# Differences between Gnoseis for Windows and Gnoseis for Android

Both apps use the same database and backup format, so a database or backup can move between them
(see [Supported schema versions](https://github.com/boredace/Gnoseis-Windows#supported-schema-versions)).
This list is the same in both repositories.

## Versions that work together

Windows and Android are numbered separately. Each app opens files with its own schema version or an
older one.

| Windows | Android | Schema version |
|---|---|---|
| 0.1.0 | 0.3.0 | 3 |
| 0.2.0 | 0.4.0 | 4 |

## Only on Windows

- Several databases: chosen at start-up or in Settings › Databases, or with `--data-folder`; and a
  portable mode that keeps the data next to the app.
- Record times shown: when a note was added and last modified, and when a contact, organization or
  item with no notes yet was added.
- Record pages for a wide screen: a contact's, organization's or item's notes as a history with a
  note box above it, People cards on an organization, one search across a category's linked
  records, and a reading pane on wide windows.
- Linked records open beside the section, in a trail of opened records.
- Notes are edited in place on their own page.
- Keyboard shortcuts following Outlook, Office and Windows.

## Only on Android

- **See all** for Most linked on Home: a page that lists the most linked records.

## The same feature, done differently

- **Backup and restore:** Android: Settings › Import / Export. Windows: Settings › Back up database
  and Restore database.
- **Backup key:** kept on the device so backing up doesn't ask for the passphrase. Android: the
  Android Keystore. Windows: DPAPI, for the current Windows user.
- **Unlinking:** Android: long-press a linked record. Windows: its context menu (right-click,
  Shift+F10 or the menu key). Both offer Undo.
- **Where the data is:** Android: the app's private storage. Windows: `%LOCALAPPDATA%\Gnoseis`, or a
  folder you choose.
- **Download:** Android: a signed APK. Windows: a portable zip, not code-signed before 1.0, with a
  SHA-256 and a build attestation.
