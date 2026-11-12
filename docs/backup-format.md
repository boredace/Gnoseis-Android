# Gnoseis backup file format

This document describes the backup files Gnoseis writes, so that a backup can be opened without the app. Gnoseis for Android and Gnoseis for Windows write and read the same format, byte for byte.

A backup is one of two kinds:

- **Plain:** an SQLite 3 database file, the same as the app's own database. Any SQLite tool can open it.
- **Encrypted:** that same SQLite file, wrapped in the container described below. Encryption changes nothing inside the database: decrypting an encrypted backup gives exactly the plain backup it was made from.

An encrypted backup is encrypted with AES-256, can only be opened with its backup passphrase, and uses no cryptography that quantum computers are known to break. The passphrase is the real limit: a generated passphrase is as strong as the cipher, while one you choose yourself is only as strong as you made it. There is no recovery and no back door: if the passphrase is lost, the data in that file can't be opened.

## Telling the two kinds apart

Look at the first bytes of the file, never at its name.

| First bytes | Kind |
|---|---|
| ASCII `GNOSEISB` (8 bytes) | Encrypted backup, described below |
| ASCII `SQLite format 3` followed by a zero byte (16 bytes) | Plain backup |

Gnoseis suggests file names ending in `.encrypted.backup` for encrypted backups and `.backup` for plain ones, but the name is only a hint.

## Encrypted container, version 1

All integers are unsigned and big-endian.

### Header (92 bytes)

| Offset | Size | Field | Value in version 1 |
|---|---|---|---|
| 0 | 8 | Magic | ASCII `GNOSEISB` |
| 8 | 1 | Format version | `1` |
| 9 | 1 | Key derivation | `1` = Argon2id, version 1.3 (0x13) |
| 10 | 1 | Cipher | `1` = AES-256-GCM in chunks |
| 11 | 1 | Reserved | `0` |
| 12 | 4 | Argon2 memory, in KiB | written: `65536` (64 MiB) |
| 16 | 4 | Argon2 iterations | written: `3` |
| 20 | 4 | Argon2 parallelism | written: `1` |
| 24 | 4 | Chunk size, in plaintext bytes | written: `65536` (64 KiB) |
| 28 | 32 | KDF salt | random; made when the passphrase is set, so backups made with the same passphrase setup share it |
| 60 | 32 | File salt | random; new for every file |

Gnoseis always writes the values shown. A reader must refuse:

- a format version, key derivation, cipher or reserved byte it doesn't know. Gnoseis reports these as made by a newer version;
- parameters outside these limits, before doing any work, so that a crafted file can't make it use too much memory or time:

| Parameter | Lowest | Highest |
|---|---|---|
| Argon2 memory | 8 MiB (8192 KiB) | 1 GiB (1048576 KiB) |
| Argon2 iterations | 1 | 16 |
| Argon2 parallelism | 1 | 4 |
| Chunk size | 4 KiB (4096 bytes) | 1 MiB (1048576 bytes) |

### Keys

```
passphrase bytes   = UTF-8( normalise(passphrase) )          (see "Passphrase" below)
master key (32 B)  = Argon2id(password = passphrase bytes,
                              salt     = KDF salt,
                              memory, iterations, parallelism from the header,
                              version  = 0x13,
                              no secret, no associated data,
                              output length = 32 bytes)
file key   (32 B)  = HKDF-SHA-512(input key material = master key,
                                  salt = file salt,
                                  info = ASCII "org.gnoseis.backup/v1/file-key",
                                  output length = 32 bytes)
```

Argon2id is defined in RFC 9106 and HKDF in RFC 5869. The file key is used for one file only, which is why the chunk nonces below can be simple counters.

### Chunks

After the header, the plaintext (the SQLite file) is split into chunks of the chunk size from the header. Every chunk is full except the last, which holds the rest: from 1 byte up to a full chunk. Each chunk is stored as its AES-256-GCM ciphertext, which is as long as the plaintext chunk, followed by its 16-byte tag.

- **Key:** the file key.
- **Nonce (12 bytes):** the chunk's index, counting from 0, as an 11-byte integer, followed by one byte: `1` for the last chunk and `0` for every other chunk.
- **Associated data:** the 92 header bytes, the same for every chunk. A changed header makes the first chunk fail.

A reader must refuse:

- a file with no chunks at all (an empty plaintext is never written);
- a chunk whose tag doesn't verify;
- a file that ends before a chunk with the last-chunk flag. This catches a file cut short at a chunk boundary;
- any data after the last chunk.

The counter in the nonce stops chunks being reordered or dropped. The last-chunk flag stops a file being cut short without notice. A wrong passphrase makes the tag of chunk 0 fail, which can't be told apart from a damaged first chunk; Gnoseis reports it as "Wrong passphrase, or the file is damaged".

File size = 92 + plaintext size + 16 × number of chunks.

### Decrypting a file, step by step

1. Read 92 bytes. Check the magic, the version, key derivation, cipher and reserved bytes, and the limits above.
2. Normalise the passphrase and encode it as UTF-8.
3. Derive the master key with Argon2id, then the file key with HKDF-SHA-512.
4. Read the chunks in order. Chunk *i* is (chunk size + 16) bytes, except the last, which is shorter or the same length; the last chunk is the one that ends at the end of the file. Decrypt each with nonce (*i*, last-chunk flag) and the header as associated data, and write out the plaintext.
5. If any tag fails, or the file ends unexpectedly, discard everything written. The result is not a usable backup.

The result is an SQLite 3 file. Gnoseis then checks it and, if it was made by an older version, upgrades it, as it does for a plain backup.

## Passphrase

A backup passphrase is either generated by Gnoseis or chosen by the user. Both go through the same normalising before key derivation, so a passphrase can be typed back with line breaks or extra spaces and still work.

### Normalising

In this order. Upper and lower case are kept as they are.

1. **Refuse** a passphrase that contains an unpaired surrogate or a noncharacter (U+FDD0 to U+FDEF, and every code point ending in FFFE or FFFF). Gnoseis treats such a passphrase as wrong when restoring, and doesn't let it be set. Generated passphrases never contain these.
2. **Unicode NFKC** normalisation.
3. **White space:** remove white space at both ends, and turn every run of white space inside into one U+0020 SPACE. White space means exactly these 25 code points (Unicode's White_Space property), and nothing else:

   U+0009 to U+000D, U+0020, U+0085, U+00A0, U+1680, U+2000 to U+200A, U+2028, U+2029, U+202F, U+205F, U+3000.

   Every other character is kept as it is, including U+001C to U+001F and the zero-width characters (U+200B, U+200C, U+200D, U+FEFF, U+180E).
4. **UTF-8** encoding.

Check against this fixed list rather than a platform's own white-space test or trim function: they disagree with each other and with this list. For example, Kotlin's `Char.isWhitespace` leaves out U+0085 and includes U+001C to U+001F; Java's `Character.isWhitespace` also leaves out U+00A0, U+2007 and U+202F; Java's `String.trim()` removes every character up to U+0020; .NET's `char.IsWhiteSpace` matches the list.

NFKC can itself produce spaces: the no-break and typographic spaces become U+0020, and so do spacing accents (U+00B4 ´ becomes U+0020 U+0301). Step 3 treats those like any other space, so "´abc" normalises to U+0301 followed by "abc".

### Rules when setting a passphrase

These apply only when a passphrase is set; nothing in the file format depends on them.

- **Generated:** always 20 words, drawn with the operating system's secure random generator from the [EFF large word list](https://www.eff.org/dice) (7,776 words, about 12.9 bits each, about 258 bits in all), lower case, separated by single spaces. The order of the words is part of the passphrase.
- **Chosen by the user:** at least 12 characters after normalising, counted in Unicode code points (so a character outside the Basic Multilingual Plane counts once).

The passphrase itself is never stored. Gnoseis keeps the derived master key on the device, protected by the platform's key store, so that later backups don't ask for it; that key never goes into a backup file.

## Test file

`app/src/test/resources/backup/encrypted-v1-android.backup` in this repository is an encrypted backup made by Gnoseis for Android, with the passphrase `gnoseis shared test backup made on android`. It can be used to check another reader. Decrypted, it is an SQLite database (`user_version` 3) with three notes titled "Shared backup note one", "Shared backup note two" and "Shared backup note three".

## Why these choices

- **AES-256-GCM:** built into both platforms and hardware-accelerated, with a 256-bit key, which keeps its strength against known quantum attacks.
- **HKDF-SHA-512:** built from HMAC, which both platforms have; SHA-512 keeps the whole chain at the 256-bit level.
- **Argon2id:** the current standard for deriving keys from passphrases. It needs a lot of memory for each guess, so guessing doesn't scale cheaply on graphics cards.
- **Chunks:** the file is never held in memory whole, and a damaged or cut file is caught where the damage is.
- **No compression** in version 1. The format version and the reserved byte leave room for changes.

## What an encrypted file still shows

Without the passphrase, anyone with the file can see that it is a Gnoseis backup, roughly how large the database is, and whether two backups were made with the same passphrase setup (they share the KDF salt). The file name Gnoseis suggests includes the date it was made.
