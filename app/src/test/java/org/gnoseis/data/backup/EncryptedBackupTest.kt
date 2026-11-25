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

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import kotlin.random.Random

/**
 * The encrypted backup container (#66): round trips, and every way a file is refused.
 * Most tests use a master key directly, so only a few run the 64 MiB Argon2id.
 */
class EncryptedBackupTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val random = Random(66)
    private val masterKey = random.nextBytes(32)
    private val kdfSalt = random.nextBytes(32)
    private val output: File get() = File(folder.root, "decrypted.db")

    // ---------- Round trips ----------

    @Test
    fun roundTrip_oneByte() = assertRoundTrip(1)

    @Test
    fun roundTrip_exactlyOneChunk() = assertRoundTrip(CHUNK)

    @Test
    fun roundTrip_oneChunkPlusOneByte() = assertRoundTrip(CHUNK + 1)

    @Test
    fun roundTrip_exactlyTwoChunks() = assertRoundTrip(2 * CHUNK)

    @Test
    fun roundTrip_withPassphrase() {
        val passphrase = "correct horse battery staple".toByteArray()
        val master = BackupKeys.deriveMasterKey(passphrase, kdfSalt, BackupFormat.WRITTEN_KDF)
        val plain = plaintext(CHUNK + 100)
        val sealed = encrypt(plain, master)

        EncryptedBackup.decrypt(ByteArrayInputStream(sealed), output, passphrase)

        assertArrayEquals(plain, output.readBytes())
    }

    @Test
    fun encrypt_writesTheHeaderOfTheFormat() {
        val sealed = encrypt(plaintext(10))
        val header = ByteBuffer.wrap(sealed, 0, BackupFormat.HEADER_SIZE)

        assertArrayEquals("GNOSEISB".toByteArray(), sealed.copyOfRange(0, 8))
        assertArrayEquals(byteArrayOf(1, 1, 1, 0), sealed.copyOfRange(8, 12))
        assertEquals(65_536, header.getInt(12))
        assertEquals(3, header.getInt(16))
        assertEquals(1, header.getInt(20))
        assertEquals(65_536, header.getInt(24))
        assertArrayEquals(kdfSalt, sealed.copyOfRange(28, 60))
    }

    @Test
    fun encrypt_usesANewFileSaltForEveryFile() {
        val plain = plaintext(10)
        val first = encrypt(plain)
        val second = encrypt(plain)

        assertFalse(first.copyOfRange(60, 92).contentEquals(second.copyOfRange(60, 92)))
        assertFalse(first.copyOfRange(92, first.size).contentEquals(second.copyOfRange(92, second.size)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun encrypt_emptyFile_isRefused() {
        encrypt(ByteArray(0))
    }

    @Test
    fun nonce_isTheIndexThenTheLastChunkFlag() {
        assertArrayEquals(ByteArray(12), EncryptedBackup.nonce(0, last = false))
        assertArrayEquals(ByteArray(11) + byteArrayOf(1), EncryptedBackup.nonce(0, last = true))
        assertArrayEquals(ByteArray(9) + byteArrayOf(1, 2, 0), EncryptedBackup.nonce(258, last = false))
    }

    // ---------- Refused: wrong key or changed file ----------

    @Test
    fun wrongPassphrase_isRefused() {
        val master = BackupKeys.deriveMasterKey("the right one".toByteArray(), kdfSalt, BackupFormat.WRITTEN_KDF)
        val sealed = encrypt(plaintext(100), master)

        assertRefused<BackupDecryptException.WrongPassphraseOrDamaged> {
            EncryptedBackup.decrypt(ByteArrayInputStream(sealed), output, "the wrong one".toByteArray())
        }
    }

    @Test
    fun wrongMasterKey_isRefused() =
        assertRefused<BackupDecryptException.WrongPassphraseOrDamaged>(encrypt(plaintext(100)), random.nextBytes(32))

    @Test
    fun flippedHeaderByte_isRefused() {
        // Memory, chunk size, KDF salt and file salt, each changed but kept within the limits
        for (offset in listOf(15, 27, 40, 70, 91)) {
            assertRefused<BackupDecryptException.WrongPassphraseOrDamaged>(encrypt(plaintext(100)).flip(offset))
        }
    }

    @Test
    fun flippedChunkByte_isRefused() =
        assertRefused<BackupDecryptException.WrongPassphraseOrDamaged>(encrypt(plaintext(3 * CHUNK)).flip(92 + SEALED + 10))

    @Test
    fun flippedTagByte_isRefused() =
        assertRefused<BackupDecryptException.WrongPassphraseOrDamaged>(encrypt(plaintext(3 * CHUNK)).flip(92 + SEALED - 1))

    @Test
    fun cutAtAChunkBoundary_isRefused() {
        val sealed = encrypt(plaintext(2 * CHUNK + 100))

        assertRefused<BackupDecryptException.Truncated>(sealed.copyOf(92 + 2 * SEALED))
        assertRefused<BackupDecryptException.Truncated>(sealed.copyOf(92 + SEALED))
    }

    @Test
    fun cutInsideAChunk_isRefused() {
        val sealed = encrypt(plaintext(2 * CHUNK + 100))

        assertRefused<BackupDecryptException.WrongPassphraseOrDamaged>(sealed.copyOf(sealed.size - 10))
        assertRefused<BackupDecryptException.WrongPassphraseOrDamaged>(sealed.copyOf(92 + SEALED + 1000))
        assertRefused<BackupDecryptException.Truncated>(sealed.copyOf(92 + 5))
    }

    @Test
    fun headerOnly_isRefused() =
        assertRefused<BackupDecryptException.Truncated>(encrypt(plaintext(100)).copyOf(92))

    @Test
    fun cutInsideTheHeader_isRefused() =
        assertRefused<BackupDecryptException.Truncated>(encrypt(plaintext(100)).copyOf(50))

    @Test
    fun extraDataAfterAFullLastChunk_isRefused() =
        assertRefused<BackupDecryptException.TrailingData>(encrypt(plaintext(2 * CHUNK)) + byteArrayOf(1, 2, 3))

    @Test
    fun extraDataAfterAShortLastChunk_isRefused() =
        assertRefused<BackupDecryptException.WrongPassphraseOrDamaged>(encrypt(plaintext(CHUNK + 100)) + byteArrayOf(1, 2, 3))

    @Test
    fun swappedChunks_areRefused() {
        val sealed = encrypt(plaintext(3 * CHUNK))
        val first = sealed.copyOfRange(92, 92 + SEALED)
        val second = sealed.copyOfRange(92 + SEALED, 92 + 2 * SEALED)
        second.copyInto(sealed, 92)
        first.copyInto(sealed, 92 + SEALED)

        assertRefused<BackupDecryptException.WrongPassphraseOrDamaged>(sealed)
    }

    @Test
    fun droppedChunk_isRefused() {
        val sealed = encrypt(plaintext(3 * CHUNK))
        val withoutSecond = sealed.copyOf(92 + SEALED) + sealed.copyOfRange(92 + 2 * SEALED, sealed.size)

        assertRefused<BackupDecryptException.WrongPassphraseOrDamaged>(withoutSecond)
    }

    // ---------- Refused: header the reader doesn't accept ----------

    @Test
    fun eachParameterOutOfRange_isRefusedBeforeAnyKeyWork() {
        val outOfRange = mapOf(
            12 to listOf(0L, 8_191L, 1_048_577L, 0xFFFF_FFFFL), // memory, KiB
            16 to listOf(0L, 17L, 0xFFFF_FFFFL), // iterations
            20 to listOf(0L, 5L, 0xFFFF_FFFFL), // parallelism
            24 to listOf(0L, 4_095L, 1_048_577L, 0xFFFF_FFFFL), // chunk size
        )
        val sealed = encrypt(plaintext(100))
        for ((offset, values) in outOfRange) {
            for (value in values) {
                val changed = sealed.withUInt32(offset, value)
                // The passphrase path would run Argon2id; the header check must stop it first
                assertRefused<BackupDecryptException.ParametersOutOfRange> {
                    EncryptedBackup.decrypt(ByteArrayInputStream(changed), output, "x".toByteArray())
                }
            }
        }
    }

    @Test
    fun parametersAtTheLimits_areAccepted() {
        val sealed = encrypt(plaintext(100))
        val limits = mapOf(12 to listOf(8_192L, 1_048_576L), 16 to listOf(1L, 16L), 20 to listOf(1L, 4L), 24 to listOf(4_096L, 1_048_576L))
        for ((offset, values) in limits) {
            for (value in values) {
                BackupHeader.read(ByteArrayInputStream(sealed.withUInt32(offset, value)))
            }
        }
    }

    @Test
    fun unknownVersionKdfCipherOrReservedByte_isRefused() {
        for (offset in 8..11) {
            assertRefused<BackupDecryptException.NewerFormat>(encrypt(plaintext(100)).also { it[offset] = 2 })
        }
    }

    @Test
    fun plainSqliteFile_isNotAnEncryptedBackup() =
        assertRefused<BackupDecryptException.NotEncrypted>("SQLite format 3\u0000".toByteArray() + ByteArray(200))

    @Test
    fun emptyFile_isNotAnEncryptedBackup() =
        assertRefused<BackupDecryptException.NotEncrypted>(ByteArray(0))

    // ---------- Helpers ----------

    private fun assertRoundTrip(size: Int) {
        val plain = plaintext(size)
        val sealed = encrypt(plain)
        val chunks = (size + CHUNK - 1) / CHUNK

        assertEquals(92 + size + 16 * chunks, sealed.size)
        assertFalse("plaintext found in the encrypted file", sealed.contains(MARKER))
        decrypt(sealed)
        assertArrayEquals(plain, output.readBytes())
    }

    /** Recognisable plaintext, so a test can check it doesn't appear in the encrypted file. */
    private fun plaintext(size: Int) = ByteArray(size) { MARKER[it % MARKER.size] }

    private fun encrypt(plain: ByteArray, master: ByteArray = masterKey): ByteArray {
        val plainFile = folder.newFile().apply { writeBytes(plain) }
        return ByteArrayOutputStream().also { EncryptedBackup.encrypt(plainFile, it, master, kdfSalt) }.toByteArray()
    }

    private fun decrypt(sealed: ByteArray, master: ByteArray = masterKey) {
        val input = ByteArrayInputStream(sealed)
        EncryptedBackup.decrypt(input, BackupHeader.read(input), master, output)
    }

    private inline fun <reified T : BackupDecryptException> assertRefused(sealed: ByteArray, master: ByteArray = masterKey) =
        assertRefused<T> { decrypt(sealed, master) }

    private inline fun <reified T : BackupDecryptException> assertRefused(block: () -> Unit) {
        try {
            block()
            fail("Expected ${T::class.simpleName}")
        } catch (e: BackupDecryptException) {
            assertTrue("Expected ${T::class.simpleName}, got ${e::class.simpleName}", e is T)
        }
        assertFalse("decrypted file left behind", output.exists())
    }

    private fun ByteArray.flip(offset: Int) = copyOf().also { it[offset] = (it[offset].toInt() xor 0x01).toByte() }

    private fun ByteArray.withUInt32(offset: Int, value: Long) = copyOf().also {
        for (i in 0 until 4) it[offset + i] = (value shr (8 * (3 - i))).toByte()
    }

    private fun ByteArray.contains(part: ByteArray): Boolean =
        (0..size - part.size).any { start -> part.indices.all { this[start + it] == part[it] } }

    private companion object {
        const val CHUNK = 65_536
        const val SEALED = CHUNK + 16
        val MARKER = "GNOSEIS-PLAINTEXT-".toByteArray()
    }
}
