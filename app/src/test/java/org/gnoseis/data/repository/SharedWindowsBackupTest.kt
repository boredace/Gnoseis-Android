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
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.backup.BackupFormat
import org.gnoseis.data.backup.BackupHeader
import org.gnoseis.data.backup.EncryptedBackup
import org.gnoseis.data.backup.FakeKeyWrapper
import org.gnoseis.data.backup.StoredBackupKey
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.data.entity.note.Note
import org.gnoseis.testutil.waitFor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

/**
 * Gnoseis-Windows' shared encrypted backup, `backup/encrypted-v1-windows.backup`, restored through
 * [DatabaseRepository] on a device without backup encryption set up (#66). The file is a byte-for-byte
 * copy of the one in Gnoseis-Windows' tests; every refusal is built from it here and must leave the
 * database unchanged.
 */
@RunWith(AndroidJUnit4::class)
class SharedWindowsBackupTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val keyFile = File(context.noBackupFilesDir, "backup_key_test")
    private val repository = DatabaseRepository(context, backupKey = StoredBackupKey(keyFile, FakeKeyWrapper()))
    private val backup = File(context.filesDir, "shared_windows.encrypted.backup")
    private val decrypted = File(context.cacheDir, "shared_windows_decrypted.db")
    private val sharedFile: ByteArray by lazy {
        javaClass.classLoader!!.getResourceAsStream("backup/$WINDOWS_FILE")!!.use { it.readBytes() }
    }

    @Before
    fun setUp() {
        GnoseisDatabase.resetInstanceForTesting()
        context.deleteDatabase(DB_NAME)
        keyFile.delete()
        runBlocking {
            val dao = db().noteDao()
            waitFor { runBlocking { dao.getNotes().first().any { it.ownerDbId == SEED_OWNER } } }
            // Something of this device's own, which a refusal must leave in place
            dao.addNote(Note(title = "Made on this device"))
        }
    }

    @After
    fun tearDown() {
        GnoseisDatabase.resetInstanceForTesting()
        context.deleteDatabase(DB_NAME)
        keyFile.delete()
        backup.delete()
        decrypted.delete()
    }

    private fun db() = GnoseisDatabase.getDatabase(context)

    // ---------- The file ----------

    @Test
    fun copy_isByteForByteWindows() = assertEquals(WINDOWS_SHA256, sha256Hex(sharedFile))

    @Test
    fun header_hasTheWrittenSettings_andTheSizeFitsTheChunks() {
        val header = BackupHeader.read(sharedFile.inputStream())
        assertEquals(BackupFormat.WRITTEN_KDF, header.kdf)
        assertEquals(BackupFormat.WRITTEN_CHUNK_SIZE, header.chunkSize)

        EncryptedBackup.decrypt(sharedFile.inputStream(), decrypted, WINDOWS_PASSPHRASE.toByteArray())
        val plainSize = decrypted.length()
        val chunks = (plainSize + header.chunkSize - 1) / header.chunkSize
        assertEquals(92 + plainSize + 16 * chunks, sharedFile.size.toLong())
        SQLiteDatabase.openDatabase(decrypted.path, null, SQLiteDatabase.OPEN_READONLY).use {
            assertEquals(3, it.version)
        }
    }

    // ---------- Restore ----------

    @Test
    fun restore_withThePassphrase_bringsInWindowsRecords() = runTest {
        assertRestores(WINDOWS_PASSPHRASE)
    }

    @Test
    fun restore_withExtraSpacesBetweenWords() = runTest {
        assertRestores(WINDOWS_PASSPHRASE.replace(" ", "   "))
    }

    @Test
    fun restore_withALineBreakBetweenWords() = runTest {
        assertRestores("correct horse\nbattery staple\r\nwindows")
    }

    @Test
    fun restore_withSpacesAtBothEnds() = runTest {
        assertRestores("  \t$WINDOWS_PASSPHRASE \n")
    }

    private suspend fun assertRestores(passphrase: String) {
        write(sharedFile)

        val result = repository.importDatabase(uri(), passphrase.toCharArray())

        assertTrue("$result", result is ImportResult.Restored)
        (result as ImportResult.Restored).keepOffer?.clear()
        assertTrue(result.encrypted)
        val contents = contents()
        assertEquals(WINDOWS_COUNTS, contents.filterKeys { it in WINDOWS_COUNTS }.mapValues { it.value.size })
        val notes = db().noteDao().getNotes().first()
        assertEquals(setOf(SEED_TITLE, WINDOWS_NOTE_TITLE), notes.mapNotNull { it.title }.toSet())
        assertEquals(WINDOWS_NOTE_TEXT, notes.single { it.title == WINDOWS_NOTE_TITLE }.textContents)
        val contact = contents.getValue("Contact").single()
        assertTrue(contact, contact.contains("|nameLast=Lovelace|nameFirst=Ada|"))
    }

    // ---------- Refusals ----------

    @Test
    fun wrongPassphrase_oneWordChanged() = assertRefused(
        ImportResult.WrongPassphraseOrDamaged, sharedFile, "correct horse battery stable windows",
    )

    @Test
    fun flippedByte_inTheKdfSalt() = assertRefused(ImportResult.WrongPassphraseOrDamaged, flipped(28 + 5))

    @Test
    fun flippedByte_inChunk0() = assertRefused(ImportResult.WrongPassphraseOrDamaged, flipped(92 + 1000))

    @Test
    fun flippedByte_inTheLastChunksTag() = assertRefused(ImportResult.WrongPassphraseOrDamaged, flipped(sharedFile.size - 1))

    @Test
    fun versionByte2_isNewer() =
        assertRefused(ImportResult.NewerFormat, sharedFile.copyOf().also { it[8] = 2 })

    @Test(timeout = 5_000)
    fun memory2GiB_isRefusedBeforeAnyKeyIsDerived() {
        val bytes = sharedFile.copyOf()
        // 2 GiB = 2,097,152 KiB = 0x00200000, big-endian at offset 12
        byteArrayOf(0x00, 0x20, 0x00, 0x00).copyInto(bytes, 12)
        assertRefused(ImportResult.WrongPassphraseOrDamaged, bytes)
    }

    @Test
    fun cutAtTheChunkBoundary() =
        assertRefused(ImportResult.WrongPassphraseOrDamaged, sharedFile.copyOf(92 + 65_536 + 16))

    @Test
    fun cutInsideTheLastChunk() =
        assertRefused(ImportResult.WrongPassphraseOrDamaged, sharedFile.copyOf(sharedFile.size - 10))

    @Test
    fun oneExtraByteAtTheEnd() =
        assertRefused(ImportResult.WrongPassphraseOrDamaged, sharedFile.copyOf(sharedFile.size + 1))

    @Test
    fun encryptedFileNamedPlain_asksForThePassphrase() = runTest {
        val plainName = File(context.filesDir, "shared_windows.backup")
        try {
            plainName.writeBytes(sharedFile)
            val before = contents()

            assertEquals(ImportResult.NeedsPassphrase, repository.importDatabase(Uri.fromFile(plainName)))

            assertEquals(before, contents())
        } finally {
            plainName.delete()
        }
    }

    @Test
    fun neitherKind_isNotABackup() =
        assertRefused(ImportResult.NotABackup, "This is a text file, not a Gnoseis backup of any kind. ".repeat(2).toByteArray().copyOf(100))

    private fun flipped(offset: Int) = sharedFile.copyOf().also { it[offset] = (it[offset].toInt() xor 0x01).toByte() }

    private fun assertRefused(expected: ImportResult, file: ByteArray, passphrase: String = WINDOWS_PASSPHRASE) = runTest {
        write(file)
        val before = contents()

        assertEquals(expected, repository.importDatabase(uri(), passphrase.toCharArray()))

        assertEquals(before, contents())
    }

    // ---------- Helpers ----------

    private fun write(bytes: ByteArray) = backup.writeBytes(bytes)

    private fun uri(): Uri = Uri.fromFile(backup)

    /** Every row of every table, as text, for comparing the whole database. */
    private fun contents(): Map<String, List<String>> {
        val live = db().openHelper.writableDatabase
        val tables = live.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' " +
                "AND name NOT IN ('android_metadata', 'room_master_table') ORDER BY name"
        ).use { buildList { while (it.moveToNext()) add(it.getString(0)) } }
        return tables.associateWith { table ->
            live.query("SELECT * FROM `$table` ORDER BY 1").use { rows ->
                buildList {
                    while (rows.moveToNext()) {
                        add((0 until rows.columnCount).joinToString("|") { "${rows.getColumnName(it)}=${rows.getString(it)}" })
                    }
                }
            }
        }
    }

    private fun sha256Hex(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        const val DB_NAME = "gnoseis_data"
        const val SEED_OWNER = "yyy"
        const val WINDOWS_FILE = "encrypted-v1-windows.backup"
        const val WINDOWS_SHA256 = "4238a533cb993a64c4551f120aca9171488961fe48ea109bf4e82828e5cdbe13"
        const val WINDOWS_PASSPHRASE = "correct horse battery staple windows"

        /** Records in Gnoseis-Windows' file: its seed note, one note it added and one contact. */
        val WINDOWS_COUNTS = mapOf(
            "Category" to 0, "Contact" to 1, "Item" to 0, "LinkedRecord" to 0, "Note" to 2, "Organization" to 0,
        )
        const val SEED_TITLE = "This is a default note"
        const val WINDOWS_NOTE_TITLE = "Shared test file"
        const val WINDOWS_NOTE_TEXT = "Made by Gnoseis for Windows"
    }
}
