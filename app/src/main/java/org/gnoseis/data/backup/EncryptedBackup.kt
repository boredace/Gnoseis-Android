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

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts a plain backup file into the encrypted container and back (#66). The plaintext is
 * split into chunks, each sealed with AES-256-GCM under a key used for this one file. The nonce
 * is the chunk's index (11 bytes) and a last-chunk flag (1 byte), and the header is the
 * associated data of every chunk, so chunks can't be changed, reordered, dropped or cut off.
 *
 * Streams one chunk at a time, never holding the file in memory. Writes no plaintext to the
 * output, and writes a decrypted chunk only after its tag has been checked.
 */
object EncryptedBackup {
    private const val AES_GCM = "AES/GCM/NoPadding"
    private const val TAG_BITS = BackupFormat.TAG_SIZE * 8

    /**
     * Writes [plainFile] to [output] as an encrypted backup, with a new random file salt.
     * [masterKey], [kdfSalt] and [kdf] come from the passphrase setup. [output] is left open.
     */
    fun encrypt(
        plainFile: File,
        output: OutputStream,
        masterKey: ByteArray,
        kdfSalt: ByteArray,
        kdf: KdfParameters = BackupFormat.WRITTEN_KDF,
        random: SecureRandom = SecureRandom(),
    ) {
        require(plainFile.length() > 0) { "An empty backup is never written" }
        val chunkSize = BackupFormat.WRITTEN_CHUNK_SIZE
        val fileSalt = ByteArray(BackupFormat.SALT_SIZE).also(random::nextBytes)
        val header = BackupHeader.create(kdf, chunkSize, kdfSalt, fileSalt)
        val associatedData = header.bytes
        val fileKey = BackupKeys.deriveFileKey(masterKey, fileSalt)
        var current = ByteArray(chunkSize)
        var next = ByteArray(chunkSize)
        try {
            val key = SecretKeySpec(fileKey, "AES")
            val cipher = Cipher.getInstance(AES_GCM)
            output.write(associatedData)
            plainFile.inputStream().use { input ->
                var length = input.readFully(current)
                var index = 0L
                while (true) {
                    // A full chunk is the last one only if nothing follows it
                    val nextLength = if (length == chunkSize) input.readFully(next) else 0
                    val last = nextLength == 0
                    cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce(index, last)))
                    cipher.updateAAD(associatedData)
                    output.write(cipher.doFinal(current, 0, length))
                    if (last) break
                    current = next.also { next = current }
                    length = nextLength
                    index++
                }
            }
        } finally {
            fileKey.fill(0)
            current.fill(0)
            next.fill(0)
        }
    }

    /**
     * Decrypts the encrypted backup in [input] into [plainFile] with [passphrase], the UTF-8
     * bytes of the normalised passphrase. On any failure [plainFile] is deleted.
     */
    fun decrypt(input: InputStream, plainFile: File, passphrase: ByteArray) {
        val header = BackupHeader.read(input)
        val masterKey = try {
            BackupKeys.deriveMasterKey(passphrase, header.kdfSalt, header.kdf)
        } catch (e: OutOfMemoryError) {
            // Settings within the format's limits can still be more than this device's heap
            throw BackupDecryptException.WrongPassphraseOrDamaged(e)
        }
        try {
            decrypt(input, header, masterKey, plainFile)
        } finally {
            masterKey.fill(0)
        }
    }

    /**
     * Decrypts the chunks that follow [header] in [input] into [plainFile] with [masterKey]
     * (a stored key, or one derived from the passphrase with the header's salt and settings).
     * Read the header first with [BackupHeader.read]. On any failure [plainFile] is deleted.
     */
    fun decrypt(input: InputStream, header: BackupHeader, masterKey: ByteArray, plainFile: File) {
        val associatedData = header.bytes
        val sealedSize = header.chunkSize + BackupFormat.TAG_SIZE
        val fileKey = BackupKeys.deriveFileKey(masterKey, header.fileSalt)
        var current = ByteArray(sealedSize)
        var next = ByteArray(sealedSize)
        try {
            val key = SecretKeySpec(fileKey, "AES")
            val cipher = Cipher.getInstance(AES_GCM)
            fun open(sealed: ByteArray, length: Int, index: Long, last: Boolean): ByteArray? = try {
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce(index, last)))
                cipher.updateAAD(associatedData)
                cipher.doFinal(sealed, 0, length)
            } catch (e: AEADBadTagException) {
                null
            }

            plainFile.outputStream().use { output ->
                fun write(plain: ByteArray) {
                    output.write(plain)
                    plain.fill(0)
                }

                var length = input.readFully(current)
                var index = 0L
                while (true) {
                    if (length <= BackupFormat.TAG_SIZE) {
                        // No chunks at all, or a last chunk too short to hold any plaintext
                        throw BackupDecryptException.Truncated()
                    }
                    if (length < sealedSize) {
                        // A short chunk can only be the last one
                        write(open(current, length, index, last = true)
                            ?: throw BackupDecryptException.WrongPassphraseOrDamaged())
                        return
                    }
                    val nextLength = input.readFully(next)
                    if (nextLength == 0) {
                        // A full chunk at the end must carry the last-chunk flag
                        val plain = open(current, length, index, last = true)
                        if (plain == null) {
                            if (open(current, length, index, last = false) != null) {
                                throw BackupDecryptException.Truncated() // cut at a chunk boundary
                            }
                            throw BackupDecryptException.WrongPassphraseOrDamaged()
                        }
                        write(plain)
                        return
                    }
                    val plain = open(current, length, index, last = false)
                    if (plain == null) {
                        if (open(current, length, index, last = true) != null) {
                            throw BackupDecryptException.TrailingData()
                        }
                        throw BackupDecryptException.WrongPassphraseOrDamaged()
                    }
                    write(plain)
                    current = next.also { next = current }
                    length = nextLength
                    index++
                }
            }
        } catch (e: Throwable) {
            plainFile.delete()
            throw e
        } finally {
            fileKey.fill(0)
            current.fill(0)
            next.fill(0)
        }
    }

    /** The chunk's index from 0 as an 11-byte integer, then 1 for the last chunk, 0 otherwise. */
    internal fun nonce(index: Long, last: Boolean): ByteArray =
        ByteBuffer.allocate(BackupFormat.NONCE_SIZE)
            .put(ByteArray(3))
            .putLong(index)
            .put(if (last) 1 else 0)
            .array()
}
