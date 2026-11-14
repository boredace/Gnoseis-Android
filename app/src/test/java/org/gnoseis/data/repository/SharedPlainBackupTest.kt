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

package org.gnoseis.data.repository

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.backup.FakeKeyWrapper
import org.gnoseis.data.backup.StoredBackupKey
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.data.entity.note.Note
import org.gnoseis.testutil.waitFor
import org.gnoseis.testutil.withDefaultZone
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

/**
 * Plain backups shared byte for byte with Gnoseis-Windows (#66): `backup/plain-v3-android.backup`,
 * made by this client's plain export, and `backup/plain-v3-windows.backup`, made by Gnoseis-Windows'.
 * Each holds every record type, two links, a title with non-ASCII text and a note with a line break.
 * Both restore here through [DatabaseRepository], with no passphrase.
 */
@RunWith(AndroidJUnit4::class)
class SharedPlainBackupTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val keyFile = File(context.noBackupFilesDir, "backup_key_test")
    private val repository = DatabaseRepository(context, backupKey = StoredBackupKey(keyFile, FakeKeyWrapper()))
    private val backup = File(context.filesDir, "shared_plain.backup")

    @Before
    fun setUp() {
        GnoseisDatabase.resetInstanceForTesting()
        context.deleteDatabase(DB_NAME)
        keyFile.delete()
        runBlocking {
            val dao = db().noteDao()
            waitFor { runBlocking { dao.getNotes().first().any { it.ownerDbId == SEED_OWNER } } }
            dao.addNote(Note(title = "Made on this device")) // replaced by the restore
        }
    }

    @After
    fun tearDown() {
        GnoseisDatabase.resetInstanceForTesting()
        context.deleteDatabase(DB_NAME)
        keyFile.delete()
        backup.delete()
    }

    private fun db() = GnoseisDatabase.getDatabase(context)

    private fun resource(name: String) =
        javaClass.classLoader!!.getResourceAsStream("backup/$name")!!.use { it.readBytes() }

    // ---------- The files ----------

    @Test
    fun androidFile_isUnchanged_andPlain() = assertUnchangedAndPlain(ANDROID_FILE, ANDROID_SHA256)

    @Test
    fun windowsCopy_isByteForByteWindows_andPlain() = assertUnchangedAndPlain(WINDOWS_FILE, WINDOWS_SHA256)

    private fun assertUnchangedAndPlain(name: String, sha256: String) {
        val bytes = resource(name)
        assertEquals(sha256, MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) })
        assertArrayEquals("SQLite format 3\u0000".toByteArray(), bytes.copyOf(16))
    }

    // ---------- Restore ----------

    @Test
    fun windowsFile_restores() = runTest {
        assertRestores(resource(WINDOWS_FILE), "Lovelace|Ada", "Windows Test Organization", "Windows")
    }

    @Test
    fun androidFile_restores() = runTest {
        assertRestores(resource(ANDROID_FILE), "Hopper|Grace", "Android Test Organization", "Android")
    }

    /** Restore decides by the first 8 bytes, never by the name: a plain file named .encrypted.backup is plain. */
    @Test
    fun windowsFile_namedEncrypted_restoresAsPlain() = runTest {
        val named = File(context.filesDir, "shared_plain.encrypted.backup")
        try {
            named.writeBytes(resource(WINDOWS_FILE))
            assertEquals(ImportResult.Restored(encrypted = false), repository.importDatabase(Uri.fromFile(named)))
            assertEquals(EXPECTED_COUNTS, counts())
        } finally {
            named.delete()
        }
    }

    /**
     * #68: both files are at version 3, so restoring converts their note times in the restoring
     * device's zone, here America/Toronto (EST, -05:00, on those days). updatedAt is unchanged.
     */
    @Test
    fun androidFile_restores_withNoteTimesConverted() = runTest {
        assertNoteTimesConverted(
            ANDROID_FILE,
            mapOf(
                // title prefix to v4 createDateTime, createDate, createdAt, updatedAt
                "This" to listOf(1794557789071, 1794546000000, 1794539789070, 1794539789070),
                "Zür" to listOf(1794557789192, 1794546000000, null, null),
                "Two" to listOf(1794557789192, 1794546000000, null, null),
            ),
        )
    }

    @Test
    fun windowsFile_restores_withNoteTimesConverted() = runTest {
        assertNoteTimesConverted(
            WINDOWS_FILE,
            mapOf(
                "This" to listOf(1794532614928, 1794459600000, 1794539814929, 1794539814929),
                "Zür" to listOf(1794557814936, 1794546000000, 1794539814937, 1794539814937),
                "Two" to listOf(1794557814942, 1794546000000, 1794539814942, 1794539814942),
            ),
        )
    }

    private suspend fun assertNoteTimesConverted(name: String, expected: Map<String, List<Long?>>) {
        backup.writeBytes(resource(name))

        val result = withDefaultZone("America/Toronto") { repository.importDatabase(Uri.fromFile(backup)) }

        assertEquals(ImportResult.Restored(encrypted = false), result)
        val notes = db().noteDao().getNotes().first()
        assertEquals(expected.keys, notes.map { note -> expected.keys.single { note.title!!.startsWith(it) } }.toSet())
        expected.forEach { (prefix, times) ->
            val note = notes.single { it.title!!.startsWith(prefix) }
            assertEquals(prefix, times, listOf(note.createDateTime, note.createDate, note.createdAt, note.updatedAt))
        }
    }

    private suspend fun assertRestores(file: ByteArray, contact: String, organization: String, client: String) {
        backup.writeBytes(file)

        assertEquals(ImportResult.Restored(encrypted = false), repository.importDatabase(Uri.fromFile(backup)))

        assertEquals(EXPECTED_COUNTS, counts())
        // The two texts, byte for byte
        val notes = db().noteDao().getNotes().first()
        val accented = notes.single { it.title?.startsWith("Z") == true }
        assertEquals(utf8Hex(ACCENTED_TITLE), utf8Hex(accented.title!!))
        assertEquals("Made by Gnoseis for $client", accented.textContents)
        assertEquals(utf8Hex(TWO_LINES), utf8Hex(notes.single { it.title == "Two lines" }.textContents!!))
        assertEquals(setOf(SEED_TITLE, ACCENTED_TITLE, "Two lines"), notes.mapNotNull { it.title }.toSet())
        // The other records and both links, by the records they join
        assertEquals(listOf(contact), query("SELECT nameLast || '|' || nameFirst FROM Contact"))
        assertEquals(listOf(organization), query("SELECT organizationName FROM Organization"))
        assertEquals(listOf("$client test category"), query("SELECT categoryName FROM Category"))
        assertEquals(listOf("$client test item"), query("SELECT itemName FROM Item"))
        assertEquals(
            listOf("1:$ACCENTED_TITLE -> 2:$contact", "2:$contact -> 3:$organization"),
            query(
                "SELECT l.record1TypeId || ':' || COALESCE(n1.title, c1.nameLast || '|' || c1.nameFirst) || ' -> ' || " +
                    "l.record2TypeId || ':' || COALESCE(c2.nameLast || '|' || c2.nameFirst, o2.organizationName) " +
                    "FROM LinkedRecord l " +
                    "LEFT JOIN Note n1 ON n1.id = l.record1Id LEFT JOIN Contact c1 ON c1.id = l.record1Id " +
                    "LEFT JOIN Contact c2 ON c2.id = l.record2Id LEFT JOIN Organization o2 ON o2.id = l.record2Id " +
                    "ORDER BY l.record1TypeId"
            ),
        )
    }

    // ---------- Helpers ----------

    private fun counts(): Map<String, Int> =
        EXPECTED_COUNTS.keys.associateWith { query("SELECT COUNT(*) FROM `$it`").single().toInt() }

    private fun query(sql: String): List<String> =
        db().openHelper.writableDatabase.query(sql).use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }

    private fun utf8Hex(text: String) = text.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }

    private companion object {
        const val DB_NAME = "gnoseis_data"
        const val SEED_OWNER = "yyy"
        const val SEED_TITLE = "This is a default note"
        const val ANDROID_FILE = "plain-v3-android.backup"
        const val ANDROID_SHA256 = "07c6058ba09088fe016e095e955ac9d21b220799218fbc9a34f0706ecadcf41d"
        const val WINDOWS_FILE = "plain-v3-windows.backup"
        const val WINDOWS_SHA256 = "39d38a759f2b23c2178136e4c0ed47dd6b6674245853bef970257cc49c5a0ff4"
        const val ACCENTED_TITLE = "Zürich café ✓ 東京"
        const val TWO_LINES = "First line\nSecond line"

        /** The same in both files: the seed note and two more, one of each other type, and two links. */
        val EXPECTED_COUNTS = mapOf(
            "Note" to 3, "Contact" to 1, "Organization" to 1, "Category" to 1, "Item" to 1, "LinkedRecord" to 2,
        )
    }
}
