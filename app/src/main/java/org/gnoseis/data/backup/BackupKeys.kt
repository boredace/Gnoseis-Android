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

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Key derivation for encrypted backups (#66):
 * master key = Argon2id(passphrase bytes, KDF salt, settings), and
 * file key = HKDF-SHA-512(master key, file salt, "org.gnoseis.backup/v1/file-key").
 * Bouncy Castle is used directly for Argon2id; no security provider is registered.
 */
object BackupKeys {
    private const val FILE_KEY_INFO = "org.gnoseis.backup/v1/file-key"
    private const val HMAC_SHA512 = "HmacSHA512"

    /**
     * Derives the 32-byte master key from [passphrase], the UTF-8 bytes of the normalised
     * passphrase. Takes about a second and [KdfParameters.memoryKiB] of heap.
     */
    fun deriveMasterKey(passphrase: ByteArray, kdfSalt: ByteArray, kdf: KdfParameters): ByteArray {
        require(kdf.isWithinLimits) { "Argon2 settings out of range" }
        require(kdfSalt.size == BackupFormat.SALT_SIZE)
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withMemoryAsKB(kdf.memoryKiB)
            .withIterations(kdf.iterations)
            .withParallelism(kdf.parallelism)
            .withSalt(kdfSalt)
            .build()
        val master = ByteArray(BackupFormat.KEY_SIZE)
        Argon2BytesGenerator().apply { init(parameters) }.generateBytes(passphrase, master)
        return master
    }

    /** Derives the 32-byte key for the one file with [fileSalt]. */
    fun deriveFileKey(masterKey: ByteArray, fileSalt: ByteArray): ByteArray {
        require(masterKey.size == BackupFormat.KEY_SIZE && fileSalt.size == BackupFormat.SALT_SIZE)
        return hkdf(
            HMAC_SHA512,
            ikm = masterKey,
            salt = fileSalt,
            info = FILE_KEY_INFO.toByteArray(Charsets.US_ASCII),
            length = BackupFormat.KEY_SIZE,
        )
    }

    /** HKDF (RFC 5869) extract and expand, on the platform's HMAC [algorithm]. */
    internal fun hkdf(algorithm: String, ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        val mac = Mac.getInstance(algorithm)
        require(length in 1..255 * mac.macLength)
        // RFC 5869: no salt means a string of hash-length zeros
        mac.init(SecretKeySpec(salt.takeIf { it.isNotEmpty() } ?: ByteArray(mac.macLength), algorithm))
        val prk = mac.doFinal(ikm)
        try {
            mac.init(SecretKeySpec(prk, algorithm))
        } finally {
            prk.fill(0)
        }
        val okm = ByteArray(length)
        var block = ByteArray(0)
        var filled = 0
        var counter = 1
        while (filled < length) {
            mac.update(block)
            mac.update(info)
            mac.update(counter.toByte())
            block.fill(0)
            block = mac.doFinal()
            val count = minOf(block.size, length - filled)
            block.copyInto(okm, filled, 0, count)
            filled += count
            counter++
        }
        block.fill(0)
        return okm
    }
}
