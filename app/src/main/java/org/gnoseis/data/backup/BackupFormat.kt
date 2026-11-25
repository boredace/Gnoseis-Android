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

import java.io.InputStream
import java.nio.ByteBuffer

/** Argon2id settings of an encrypted backup (#66). Memory is in KiB. */
data class KdfParameters(val memoryKiB: Int, val iterations: Int, val parallelism: Int) {
    /** True when a reader accepts these settings; checked before any key is derived. */
    val isWithinLimits: Boolean
        get() = memoryKiB in BackupFormat.MEMORY_KIB_LIMITS &&
            iterations in BackupFormat.ITERATIONS_LIMITS &&
            parallelism in BackupFormat.PARALLELISM_LIMITS
}

/**
 * The encrypted backup container, version 1 (#66). The plaintext is the usual backup file,
 * unchanged. All integers are unsigned and big-endian. See docs/backup-format.md.
 */
object BackupFormat {
    /** ASCII "GNOSEISB". A plain backup starts with "SQLite format 3\u0000" instead. */
    val MAGIC: ByteArray get() = "GNOSEISB".toByteArray(Charsets.US_ASCII)

    const val VERSION = 1
    const val KDF_ARGON2ID_13 = 1
    const val CIPHER_AES_256_GCM_CHUNKED = 1
    const val RESERVED = 0

    const val HEADER_SIZE = 92
    const val SALT_SIZE = 32
    const val KEY_SIZE = 32
    const val TAG_SIZE = 16
    const val NONCE_SIZE = 12

    /** What a writer always writes. */
    val WRITTEN_KDF = KdfParameters(memoryKiB = 65_536, iterations = 3, parallelism = 1)
    const val WRITTEN_CHUNK_SIZE = 65_536

    /** What a reader accepts, so a crafted header can't make it exhaust memory or time. */
    val MEMORY_KIB_LIMITS = 8_192..1_048_576
    val ITERATIONS_LIMITS = 1..16
    val PARALLELISM_LIMITS = 1..4
    val CHUNK_SIZE_LIMITS = 4_096..1_048_576

    /** True when [start] (the first bytes of a file) begins with [MAGIC]. */
    fun hasMagic(start: ByteArray): Boolean =
        start.size >= MAGIC.size && MAGIC.indices.all { start[it] == MAGIC[it] }
}

/**
 * The 92-byte header of an encrypted backup. [bytes] are exactly the bytes in the file; they are
 * the associated data of every chunk.
 */
class BackupHeader private constructor(
    val kdf: KdfParameters,
    val chunkSize: Int,
    private val kdfSaltBytes: ByteArray,
    private val fileSaltBytes: ByteArray,
    private val headerBytes: ByteArray,
) {
    val kdfSalt: ByteArray get() = kdfSaltBytes.copyOf()
    val fileSalt: ByteArray get() = fileSaltBytes.copyOf()
    val bytes: ByteArray get() = headerBytes.copyOf()

    companion object {
        /** Builds the header a writer writes. */
        fun create(kdf: KdfParameters, chunkSize: Int, kdfSalt: ByteArray, fileSalt: ByteArray): BackupHeader {
            require(kdf.isWithinLimits) { "Argon2 settings out of range" }
            require(chunkSize in BackupFormat.CHUNK_SIZE_LIMITS) { "Chunk size out of range" }
            require(kdfSalt.size == BackupFormat.SALT_SIZE && fileSalt.size == BackupFormat.SALT_SIZE)
            val bytes = ByteBuffer.allocate(BackupFormat.HEADER_SIZE)
                .put(BackupFormat.MAGIC)
                .put(BackupFormat.VERSION.toByte())
                .put(BackupFormat.KDF_ARGON2ID_13.toByte())
                .put(BackupFormat.CIPHER_AES_256_GCM_CHUNKED.toByte())
                .put(BackupFormat.RESERVED.toByte())
                .putInt(kdf.memoryKiB)
                .putInt(kdf.iterations)
                .putInt(kdf.parallelism)
                .putInt(chunkSize)
                .put(kdfSalt)
                .put(fileSalt)
                .array()
            return BackupHeader(kdf, chunkSize, kdfSalt.copyOf(), fileSalt.copyOf(), bytes)
        }

        /**
         * Reads and checks the header at the start of [input], leaving [input] at the first chunk.
         * Refuses an unknown version or settings out of range before any key work.
         */
        fun read(input: InputStream): BackupHeader {
            val bytes = ByteArray(BackupFormat.HEADER_SIZE)
            val count = input.readFully(bytes)
            if (!BackupFormat.hasMagic(bytes.copyOf(count))) throw BackupDecryptException.NotEncrypted()
            if (count < BackupFormat.HEADER_SIZE) throw BackupDecryptException.Truncated()

            if (bytes[8].toInt() != BackupFormat.VERSION ||
                bytes[9].toInt() != BackupFormat.KDF_ARGON2ID_13 ||
                bytes[10].toInt() != BackupFormat.CIPHER_AES_256_GCM_CHUNKED ||
                bytes[11].toInt() != BackupFormat.RESERVED
            ) throw BackupDecryptException.NewerFormat()

            val memory = bytes.uint32At(12)
            val iterations = bytes.uint32At(16)
            val parallelism = bytes.uint32At(20)
            val chunkSize = bytes.uint32At(24)
            if (memory !in BackupFormat.MEMORY_KIB_LIMITS.toLongRange() ||
                iterations !in BackupFormat.ITERATIONS_LIMITS.toLongRange() ||
                parallelism !in BackupFormat.PARALLELISM_LIMITS.toLongRange() ||
                chunkSize !in BackupFormat.CHUNK_SIZE_LIMITS.toLongRange()
            ) throw BackupDecryptException.ParametersOutOfRange()

            return BackupHeader(
                kdf = KdfParameters(memory.toInt(), iterations.toInt(), parallelism.toInt()),
                chunkSize = chunkSize.toInt(),
                kdfSaltBytes = bytes.copyOfRange(28, 60),
                fileSaltBytes = bytes.copyOfRange(60, 92),
                headerBytes = bytes,
            )
        }

        private fun ByteArray.uint32At(offset: Int): Long =
            (0 until 4).fold(0L) { value, i -> (value shl 8) or (this[offset + i].toLong() and 0xFF) }

        private fun IntRange.toLongRange() = first.toLong()..last.toLong()
    }
}

/**
 * Reads until [buffer] is full or the stream ends, and returns the number of bytes read.
 * (InputStream.readNBytes needs API 33.)
 */
internal fun InputStream.readFully(buffer: ByteArray): Int {
    var total = 0
    while (total < buffer.size) {
        val count = read(buffer, total, buffer.size - total)
        if (count < 0) break
        total += count
    }
    return total
}
