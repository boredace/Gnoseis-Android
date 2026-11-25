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

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.backup.EncryptedBackupDatabaseTest
import org.gnoseis.data.backup.FakeKeyWrapper
import org.gnoseis.data.backup.StoredBackupKey
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.settings.SettingKey
import org.gnoseis.testutil.waitFor
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Encrypted export and restore through [DatabaseRepository] (#66). */
@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val keyFile = File(context.noBackupFilesDir, "backup_key_test")
    private val wrapper = FakeKeyWrapper()
    private val backupKey = StoredBackupKey(keyFile, wrapper)
    private val repository = DatabaseRepository(context, now = { EXPORT_TIME }, backupKey = backupKey)
    private val backup = File(context.filesDir, "encrypted_test.backup")
    private val backupUri: Uri get() = Uri.fromFile(backup)

    @Before
    fun setUp() {
        GnoseisDatabase.resetInstanceForTesting()
        context.deleteDatabase(DB_NAME)
        backup.delete()
        keyFile.delete()
        runBlocking {
            val dao = db().noteDao()
            waitFor { runBlocking { dao.getNotes().first().any { it.ownerDbId == SEED_OWNER } } }
        }
    }

    @After
    fun tearDown() {
        GnoseisDatabase.resetInstanceForTesting()
        context.deleteDatabase(DB_NAME)
        backup.delete()
        keyFile.delete()
    }

    private fun db() = GnoseisDatabase.getDatabase(context)

    private suspend fun userTitles() =
        db().noteDao().getNotes().first().filter { it.ownerDbId != SEED_OWNER }.mapNotNull { it.title }.toSet()

    private suspend fun lastBackupAt() = db().settingsDao().getValueInt(SettingKey.LAST_BACKUP_AT.id)

    /** Backs up a database holding note A with [PASSPHRASE], then adds note B. */
    private suspend fun encryptedBackupWithAThenAddB() {
        backupKey.setPassphrase(PASSPHRASE.toByteArray())
        db().noteDao().addNote(Note(title = "A"))
        assertEquals(ExportResult.Encrypted, repository.exportDatabase(backupUri))
        db().noteDao().addNote(Note(title = "B"))
    }

    // ---------- Export ----------

    @Test
    fun export_withEncryptionOn_writesAnEncryptedFileWithNoPlaintext() = runTest {
        backupKey.setPassphrase(PASSPHRASE.toByteArray())
        db().noteDao().addNote(Note(title = "A very recognisable note title"))

        assertEquals(ExportResult.Encrypted, repository.exportDatabase(backupUri))

        val bytes = backup.readBytes()
        assertArrayEquals("GNOSEISB".toByteArray(), bytes.copyOf(8))
        assertFalse(bytes.contains("SQLite format 3".toByteArray()))
        assertFalse(bytes.contains("A very recognisable note title".toByteArray()))
        assertEquals(EXPORT_TIME, lastBackupAt())
    }

    @Test
    fun export_withEncryptionOff_isPlainAsBefore() = runTest {
        assertEquals(ExportResult.Plain, repository.exportDatabase(backupUri))
        assertArrayEquals("SQLite format 3\u0000".toByteArray(), backup.readBytes().copyOf(16))
    }

    @Test
    fun export_plainWhileEncryptionIsOn_writesAPlainFile() = runTest {
        backupKey.setPassphrase(PASSPHRASE.toByteArray())

        assertEquals(ExportResult.Plain, repository.exportDatabase(backupUri, plain = true))

        assertArrayEquals("SQLite format 3\u0000".toByteArray(), backup.readBytes().copyOf(16))
    }

    @Test
    fun export_withAnUnreadableKey_writesNothing_andLeavesLastBackupAtUnchanged() = runTest {
        backupKey.setPassphrase(PASSPHRASE.toByteArray())
        wrapper.failUnwrap = true

        assertEquals(ExportResult.KeyUnreadable, repository.exportDatabase(backupUri))

        assertFalse(backup.exists())
        assertNull(lastBackupAt())
    }

    // ---------- Import ----------

    @Test
    fun import_withTheStoredKey_needsNoPassphrase() = runTest {
        encryptedBackupWithAThenAddB()

        assertEquals(ImportResult.Restored(encrypted = true), repository.importDatabase(backupUri))

        assertEquals(setOf("A"), userTitles())
    }

    @Test
    fun import_afterAPassphraseChange_asksForTheOldPassphrase() = runTest {
        encryptedBackupWithAThenAddB()
        backupKey.setPassphrase("a newer passphrase".toByteArray())

        assertEquals(ImportResult.NeedsPassphrase, repository.importDatabase(backupUri))
        assertEquals(setOf("A", "B"), userTitles())

        val result = repository.importDatabase(backupUri, PASSPHRASE.toCharArray())

        assertEquals(ImportResult.Restored(encrypted = true), result) // this device has its own key: no offer
        assertEquals(setOf("A"), userTitles())
    }

    @Test
    fun import_wrongPassphrase_isRefused_andDataKept() = runTest {
        encryptedBackupWithAThenAddB()
        backupKey.turnOff()

        assertEquals(ImportResult.WrongPassphraseOrDamaged, repository.importDatabase(backupUri, "not it at all".toCharArray()))

        assertEquals(setOf("A", "B"), userTitles())
    }

    @Test
    fun import_passphraseWithAnUnusableCharacter_isAWrongPassphrase() = runTest {
        encryptedBackupWithAThenAddB()
        backupKey.turnOff()

        assertEquals(ImportResult.WrongPassphraseOrDamaged, repository.importDatabase(backupUri, "abc￾defghijkl".toCharArray()))
    }

    @Test
    fun import_onADeviceWithoutAKey_offersToKeepEncrypting() = runTest {
        encryptedBackupWithAThenAddB()
        backupKey.turnOff()

        val result = repository.importDatabase(backupUri, "  ${PASSPHRASE}\n".toCharArray()) as ImportResult.Restored

        assertTrue(result.encrypted)
        assertEquals(setOf("A"), userTitles())
        val offer = result.keepOffer
        assertNotNull(offer)
        repository.keepEncrypting(offer!!)

        // The next backup is encrypted and restores with the kept key, no passphrase
        db().noteDao().addNote(Note(title = "C"))
        assertEquals(ExportResult.Encrypted, repository.exportDatabase(backupUri))
        db().noteDao().addNote(Note(title = "D"))
        assertEquals(ImportResult.Restored(encrypted = true), repository.importDatabase(backupUri))
        assertEquals(setOf("A", "C"), userTitles())
    }

    @Test
    fun import_damagedFile_isRefused_andDataKept() = runTest {
        encryptedBackupWithAThenAddB()
        val bytes = backup.readBytes()
        bytes[bytes.size / 2] = (bytes[bytes.size / 2].toInt() xor 1).toByte()
        backup.writeBytes(bytes)

        assertEquals(ImportResult.WrongPassphraseOrDamaged, repository.importDatabase(backupUri))

        assertEquals(setOf("A", "B"), userTitles())
    }

    @Test
    fun import_cutShort_isRefused_andDataKept() = runTest {
        encryptedBackupWithAThenAddB()
        backup.writeBytes(backup.readBytes().copyOf(92 + 65_536 + 16))

        assertEquals(ImportResult.WrongPassphraseOrDamaged, repository.importDatabase(backupUri))
        assertEquals(setOf("A", "B"), userTitles())
    }

    @Test
    fun import_newerFormat_isRefused_andDataKept() = runTest {
        encryptedBackupWithAThenAddB()
        backup.writeBytes(backup.readBytes().also { it[8] = 2 })

        assertEquals(ImportResult.NewerFormat, repository.importDatabase(backupUri))
        assertEquals(setOf("A", "B"), userTitles())
    }

    @Test
    fun import_sharedFileMadeByAndroid_restores() = runTest {
        javaClass.classLoader!!.getResourceAsStream("backup/encrypted-v1-android.backup").use { input ->
            backup.outputStream().use { input.copyTo(it) }
        }

        val result = repository.importDatabase(backupUri, EncryptedBackupDatabaseTest.SHARED_PASSPHRASE.toCharArray())

        assertTrue(result is ImportResult.Restored)
        (result as ImportResult.Restored).keepOffer?.clear()
        assertEquals(EncryptedBackupDatabaseTest.SHARED_TITLES, userTitles())
    }

    // ---------- Temporary files ----------

    @Test
    fun noPlainCopyIsLeftInTheCache_afterEncryptedExportImportOrRefusal() = runTest {
        encryptedBackupWithAThenAddB()
        repository.importDatabase(backupUri)
        backupKey.turnOff()
        repository.importDatabase(backupUri, "not it at all".toCharArray())
        repository.importDatabase(backupUri)

        assertEquals(emptyList<String>(), tempFiles())
    }

    @Test
    fun leftoverDecryptedFile_isRemovedAtStartup() = runTest {
        File(context.cacheDir, "gnoseis_import123.db").writeText("decrypted leftover")
        File(context.cacheDir, "gnoseis_export456.db").writeText("plain leftover")

        repository.deleteLeftoverTempFiles()

        assertEquals(emptyList<String>(), tempFiles())
    }

    /** Backup copies in the cache folder (not Room's gnoseis_data.lck lock file). */
    private fun tempFiles() =
        context.cacheDir.list()!!.filter { it.startsWith("gnoseis_export") || it.startsWith("gnoseis_import") }

    private fun ByteArray.contains(part: ByteArray): Boolean =
        (0..size - part.size).any { start -> part.indices.all { this[start + it] == part[it] } }

    private companion object {
        const val DB_NAME = "gnoseis_data"
        const val SEED_OWNER = "yyy"
        const val EXPORT_TIME = 1_000L
        const val PASSPHRASE = "the backup passphrase for these tests"
    }
}
