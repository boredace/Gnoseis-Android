/*
*   Gnoseis is an Android native CRM application and general knowledge manager.
*
*   Copyright (C) 2024 Gnoseis.org
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
*   https://github.com/gnoseis.org/Gnoseis-Android/blob/main/LICENSE.
*
*   Dual Licensing Notice:
*
*   Gnoseis can also be licensed under commercial terms. See
*   https://github.com/gnoseis.org/Gnoseis-Android/blob/main/COMMERCIAL_LICENSE
*
*   By contributing to Gnoseis, you agree that your contributions will be
*   licensed under both the open-source license in the LICENSE file and the
*   commercial license described in the COMMERCIAL_LICENSE file.
*/

package org.gnoseis.data.backup

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * The backup master key kept on this device, so Back up now needs no passphrase (#66).
 * The master key, KDF salt and Argon2 settings are wrapped by [wrapper] (the Android Keystore)
 * in a file in the no-backup folder, which Android never copies in a backup or device transfer.
 * The passphrase itself is never stored. This is a device setting: nothing goes into the
 * database or the Settings table.
 *
 * Use one instance per file; its methods are synchronized.
 */
class StoredBackupKey(
    private val file: File,
    private val wrapper: KeyWrapper,
    private val random: SecureRandom = SecureRandom(),
) {
    /** Where the wrapped key is kept. */
    internal val location: File get() = file

    sealed interface State

    /** Backup encryption is off on this device. */
    data object Off : State

    /**
     * The file is there but can't be opened (the Keystore key is gone, or the file changed).
     * Encryption is still on: backups are refused until the passphrase is set again, so a
     * backup is never written in plain by surprise.
     */
    data object Unreadable : State

    /** The stored key. Call [clear] when done with it. */
    class On(val masterKey: ByteArray, val kdfSalt: ByteArray, val kdf: KdfParameters) : State {
        fun clear() {
            masterKey.fill(0)
            kdfSalt.fill(0)
        }
    }

    @Synchronized
    fun state(): State {
        if (!file.exists()) return Off
        var payload: ByteArray? = null
        return try {
            val stored = AtomicFile(file).readFully()
            if (stored.isEmpty() || stored[0].toInt() != FORMAT_VERSION) return Unreadable
            payload = wrapper.unwrap(stored.copyOfRange(1, stored.size), ASSOCIATED_DATA)
            if (payload.size != PAYLOAD_SIZE) return Unreadable
            val buffer = ByteBuffer.wrap(payload)
            val masterKey = ByteArray(BackupFormat.KEY_SIZE).also { buffer.get(it) }
            val kdfSalt = ByteArray(BackupFormat.SALT_SIZE).also { buffer.get(it) }
            val kdf = KdfParameters(buffer.int, buffer.int, buffer.int)
            if (!kdf.isWithinLimits) Unreadable else On(masterKey, kdfSalt, kdf)
        } catch (e: Exception) {
            Unreadable
        } finally {
            payload?.fill(0)
        }
    }

    /**
     * Sets or changes the passphrase: a new KDF salt, a master key derived from [passphrase]
     * (the normalised UTF-8 bytes) with the written settings, stored for backups from now on.
     * Takes about a second; call it off the main thread.
     */
    @Synchronized
    fun setPassphrase(passphrase: ByteArray) {
        val kdfSalt = ByteArray(BackupFormat.SALT_SIZE).also(random::nextBytes)
        val masterKey = BackupKeys.deriveMasterKey(passphrase, kdfSalt, BackupFormat.WRITTEN_KDF)
        try {
            keep(masterKey, kdfSalt, BackupFormat.WRITTEN_KDF)
        } finally {
            masterKey.fill(0)
        }
    }

    /** Stores a master key already derived, as after a restore on a device without one. */
    @Synchronized
    fun keep(masterKey: ByteArray, kdfSalt: ByteArray, kdf: KdfParameters) {
        require(masterKey.size == BackupFormat.KEY_SIZE && kdfSalt.size == BackupFormat.SALT_SIZE)
        require(kdf.isWithinLimits)
        val payload = ByteBuffer.allocate(PAYLOAD_SIZE)
            .put(masterKey).put(kdfSalt)
            .putInt(kdf.memoryKiB).putInt(kdf.iterations).putInt(kdf.parallelism)
            .array()
        try {
            val wrapped = wrapper.wrap(payload, ASSOCIATED_DATA)
            file.parentFile?.mkdirs()
            val atomic = AtomicFile(file)
            val output = atomic.startWrite()
            try {
                output.write(FORMAT_VERSION)
                output.write(wrapped)
                atomic.finishWrite(output)
            } catch (e: Exception) {
                atomic.failWrite(output)
                throw e
            }
        } finally {
            payload.fill(0)
        }
    }

    /**
     * The stored master key if [header]'s KDF salt and settings are the ones it was derived
     * with, otherwise null (ask for the passphrase). The caller clears the returned key.
     */
    @Synchronized
    fun matching(header: BackupHeader): ByteArray? {
        val stored = state() as? On ?: return null
        val headerSalt = header.kdfSalt
        return try {
            if (MessageDigest.isEqual(stored.kdfSalt, headerSalt) && stored.kdf == header.kdf) {
                stored.masterKey.copyOf()
            } else {
                null
            }
        } finally {
            stored.clear()
            headerSalt.fill(0)
        }
    }

    /** Turns backup encryption off on this device. Encrypted files already made are untouched. */
    @Synchronized
    fun turnOff() {
        AtomicFile(file).delete()
        wrapper.delete()
    }

    companion object {
        const val FILE_NAME = "backup_key"
        private const val FORMAT_VERSION = 1
        private const val PAYLOAD_SIZE = BackupFormat.KEY_SIZE + BackupFormat.SALT_SIZE + 3 * 4
        private val ASSOCIATED_DATA = "org.gnoseis.backup/stored-key/v1".toByteArray(Charsets.US_ASCII)

        /** This device's stored key, in the no-backup folder, wrapped by the Android Keystore. */
        fun forDevice(context: Context) =
            StoredBackupKey(File(context.noBackupFilesDir, FILE_NAME), AndroidKeystoreWrapper())
    }
}
