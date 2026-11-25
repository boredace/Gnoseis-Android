/*
*   Gnoseis is an Android native CRM application and general knowledge manager.
*
*   Copyright (C) 2024-2026 Gnoseis.org
*
*   This program is free software: you can redistribute it and/or modify
*   it under the terms of the GNU General Public License as published by
*   the Free Software Foundation version 3 of the License.
*
*   This program is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*   GNU General Public License for more details.
*
*   You should have received a copy of the GNU General Public License
*   along with this program. If not, see
*   https://github.com/boredace/Gnoseis-Android/blob/main/LICENSE.
*
*   Dual Licensing Notice:
*
*   Gnoseis can also be licensed under commercial terms. See
*   https://github.com/boredace/Gnoseis-Android/blob/main/COMMERCIAL_LICENSE
*
*   By contributing to Gnoseis, you agree that your contributions will be
*   licensed under both the open-source license in the LICENSE file and the
*   commercial license described in the COMMERCIAL_LICENSE file.
*/

package org.gnoseis.data.backup

import java.io.IOException

/**
 * Why an encrypted backup was refused (#66). Whatever the reason, nothing is restored and no
 * decrypted file is left behind.
 *
 * Only [NewerFormat] has its own message for the user; every other kind is shown as
 * "Wrong passphrase, or the file is damaged". [Truncated] and [TrailingData] are reported where
 * the format can tell them apart; a file cut inside a chunk, or with data after a short last
 * chunk, fails a chunk's tag and is reported as [WrongPassphraseOrDamaged].
 */
sealed class BackupDecryptException(message: String, cause: Throwable? = null) : IOException(message, cause) {
    /** The file doesn't start with the encrypted backup marker. */
    class NotEncrypted : BackupDecryptException("Not an encrypted backup")

    /** Unknown version, key derivation, cipher or reserved byte. */
    class NewerFormat : BackupDecryptException("Made by a newer version of Gnoseis")

    /** Argon2 settings or chunk size outside what a reader accepts. */
    class ParametersOutOfRange : BackupDecryptException("Backup settings out of range")

    /** A chunk's tag didn't match: a wrong passphrase, or a changed file. */
    class WrongPassphraseOrDamaged(cause: Throwable? = null) :
        BackupDecryptException("Wrong passphrase, or the file is damaged", cause)

    /** The file ends before its last chunk. */
    class Truncated : BackupDecryptException("The backup file is cut short")

    /** There is data after the last chunk. */
    class TrailingData : BackupDecryptException("The backup file has data after its end")
}
