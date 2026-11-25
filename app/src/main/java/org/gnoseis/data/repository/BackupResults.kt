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

package org.gnoseis.data.repository

import org.gnoseis.data.backup.KdfParameters

/** How a backup export ended (#66). */
sealed interface ExportResult {
    /** Written as a plain SQLite file. */
    data object Plain : ExportResult

    /** Written encrypted with the stored backup key. */
    data object Encrypted : ExportResult

    /**
     * Encryption is on but the stored key can't be opened. Nothing was written; the user sets
     * the passphrase again (or backs up without encryption on purpose).
     */
    data object KeyUnreadable : ExportResult

    /** The copy or the write failed. lastBackupAt is unchanged. */
    data object Failed : ExportResult
}

/** How a restore ended (#66). On anything but [Restored] the database is unchanged. */
sealed interface ImportResult {
    /**
     * The backup replaced all data. [keepOffer] is set after a passphrase restore on a device
     * without a usable stored key: the offer to keep encrypting with that passphrase.
     */
    data class Restored(val encrypted: Boolean, val keepOffer: KeepOffer? = null) : ImportResult

    /** An encrypted backup the stored key doesn't open: ask for its passphrase and try again. */
    data object NeedsPassphrase : ImportResult

    /** "Wrong passphrase, or the file is damaged." */
    data object WrongPassphraseOrDamaged : ImportResult

    /** "Made by a newer version of Gnoseis": a newer container or a newer database version. */
    data object NewerFormat : ImportResult

    /** Not a Gnoseis backup, or one that can't be brought up to date. */
    data object NotABackup : ImportResult

    /** The file couldn't be opened or read, or the restore itself failed. */
    data object Failed : ImportResult
}

/**
 * The key derived from the passphrase during a restore, offered to be kept on this device
 * ("Keep encrypting backups on this device with this passphrase"). Pass it to
 * [DatabaseRepository.keepEncrypting], or call [clear] if the user declines.
 */
class KeepOffer internal constructor(
    internal val masterKey: ByteArray,
    internal val kdfSalt: ByteArray,
    internal val kdf: KdfParameters,
) {
    fun clear() {
        masterKey.fill(0)
        kdfSalt.fill(0)
    }
}
