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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.random.Random

/** The backup key stored on the device (#66), with a stand-in for the Android Keystore. */
@RunWith(AndroidJUnit4::class)
class StoredBackupKeyTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val file = File(context.noBackupFilesDir, StoredBackupKey.FILE_NAME)
    private val wrapper = FakeKeyWrapper()
    private val stored = StoredBackupKey(file, wrapper)

    private val random = Random(66)
    private val masterKey = random.nextBytes(32)
    private val kdfSalt = random.nextBytes(32)

    @After
    fun tearDown() {
        file.delete()
    }

    @Test
    fun nothingStored_isOff() = assertEquals(StoredBackupKey.Off, stored.state())

    @Test
    fun keep_thenState_givesTheSameKeySaltAndSettings() {
        stored.keep(masterKey, kdfSalt, BackupFormat.WRITTEN_KDF)

        val state = stored.state() as StoredBackupKey.On
        assertArrayEquals(masterKey, state.masterKey)
        assertArrayEquals(kdfSalt, state.kdfSalt)
        assertEquals(BackupFormat.WRITTEN_KDF, state.kdf)
    }

    @Test
    fun storedFile_holdsNeitherTheKeyNorTheSaltInTheClear() {
        stored.keep(masterKey, kdfSalt, BackupFormat.WRITTEN_KDF)
        val bytes = file.readBytes()

        assertFalse(bytes.contains(masterKey.copyOf(8)))
        assertFalse(bytes.contains(kdfSalt.copyOf(8)))
    }

    @Test
    fun setPassphrase_storesTheKeyDerivedWithTheStoredSalt() {
        val passphrase = "a passphrase for the stored key".toByteArray()

        stored.setPassphrase(passphrase)

        val state = stored.state() as StoredBackupKey.On
        assertEquals(BackupFormat.WRITTEN_KDF, state.kdf)
        assertArrayEquals(BackupKeys.deriveMasterKey(passphrase, state.kdfSalt, state.kdf), state.masterKey)
    }

    @Test
    fun matching_sameSaltAndSettings_givesTheKey() {
        stored.keep(masterKey, kdfSalt, BackupFormat.WRITTEN_KDF)

        assertArrayEquals(masterKey, stored.matching(header(kdfSalt, BackupFormat.WRITTEN_KDF)))
    }

    @Test
    fun matching_otherSaltOrSettings_givesNone() {
        stored.keep(masterKey, kdfSalt, BackupFormat.WRITTEN_KDF)
        val written = BackupFormat.WRITTEN_KDF

        assertNull(stored.matching(header(random.nextBytes(32), written)))
        assertNull(stored.matching(header(kdfSalt, written.copy(memoryKiB = 32_768))))
        assertNull(stored.matching(header(kdfSalt, written.copy(iterations = 4))))
        assertNull(stored.matching(header(kdfSalt, written.copy(parallelism = 2))))
    }

    @Test
    fun matching_whenOff_givesNone() = assertNull(stored.matching(header(kdfSalt, BackupFormat.WRITTEN_KDF)))

    @Test
    fun change_replacesTheSaltAndTheKey() {
        stored.setPassphrase("the first passphrase".toByteArray())
        val first = stored.state() as StoredBackupKey.On

        stored.setPassphrase("the second passphrase".toByteArray())
        val second = stored.state() as StoredBackupKey.On

        assertFalse(first.kdfSalt.contentEquals(second.kdfSalt))
        assertFalse(first.masterKey.contentEquals(second.masterKey))
        assertNull(stored.matching(header(first.kdfSalt, first.kdf)))
        assertNotNull(stored.matching(header(second.kdfSalt, second.kdf)))
    }

    @Test
    fun turnOff_deletesTheFileAndTheWrappingKey() {
        stored.keep(masterKey, kdfSalt, BackupFormat.WRITTEN_KDF)

        stored.turnOff()

        assertFalse(file.exists())
        assertEquals(1, wrapper.deleted)
        assertEquals(StoredBackupKey.Off, stored.state())
    }

    @Test
    fun lostWrappingKey_isUnreadable_notOff() {
        stored.keep(masterKey, kdfSalt, BackupFormat.WRITTEN_KDF)
        wrapper.failUnwrap = true

        assertEquals(StoredBackupKey.Unreadable, stored.state())
        assertNull(stored.matching(header(kdfSalt, BackupFormat.WRITTEN_KDF)))
        assertTrue(file.exists())
    }

    @Test
    fun damagedFile_isUnreadable_notOff() {
        stored.keep(masterKey, kdfSalt, BackupFormat.WRITTEN_KDF)
        val bytes = file.readBytes()
        bytes[bytes.size - 3] = (bytes[bytes.size - 3].toInt() xor 1).toByte()
        file.writeBytes(bytes)

        assertEquals(StoredBackupKey.Unreadable, stored.state())
    }

    @Test
    fun emptyOrUnknownVersionFile_isUnreadable() {
        file.parentFile!!.mkdirs()
        file.writeBytes(ByteArray(0))
        assertEquals(StoredBackupKey.Unreadable, stored.state())

        stored.keep(masterKey, kdfSalt, BackupFormat.WRITTEN_KDF)
        file.writeBytes(file.readBytes().also { it[0] = 2 })
        assertEquals(StoredBackupKey.Unreadable, stored.state())
    }

    @Test
    fun setPassphraseAfterUnreadable_turnsItBackOn() {
        stored.keep(masterKey, kdfSalt, BackupFormat.WRITTEN_KDF)
        wrapper.failUnwrap = true
        assertEquals(StoredBackupKey.Unreadable, stored.state())

        wrapper.failUnwrap = false
        stored.setPassphrase("set it again".toByteArray())

        assertTrue(stored.state() is StoredBackupKey.On)
    }

    private fun header(salt: ByteArray, kdf: KdfParameters) =
        BackupHeader.create(kdf, BackupFormat.WRITTEN_CHUNK_SIZE, salt, ByteArray(32))

    private fun ByteArray.contains(part: ByteArray): Boolean =
        (0..size - part.size).any { start -> part.indices.all { this[start + it] == part[it] } }
}
