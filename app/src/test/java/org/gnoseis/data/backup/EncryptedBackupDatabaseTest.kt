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
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.repository.DatabaseRepository
import org.gnoseis.testutil.waitFor
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.SecureRandom

/**
 * Encrypted backups of real backup files (#66): a round trip of a fresh export, and the shared
 * test file that Gnoseis-Windows restores too.
 */
@RunWith(AndroidJUnit4::class)
class EncryptedBackupDatabaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val plainBackup = File(context.cacheDir, "plain.backup")
    private val decrypted = File(context.cacheDir, "decrypted.db")

    @Before
    fun setUp() {
        GnoseisDatabase.resetInstanceForTesting()
        context.deleteDatabase(DB_NAME)
        // Let the asynchronous seed-note insert finish first
        runBlocking {
            val dao = db().noteDao()
            waitFor { runBlocking { dao.getNotes().first().any { it.ownerDbId == SEED_OWNER } } }
        }
    }

    @After
    fun tearDown() {
        GnoseisDatabase.resetInstanceForTesting()
        context.deleteDatabase(DB_NAME)
        plainBackup.delete()
        decrypted.delete()
    }

    private fun db() = GnoseisDatabase.getDatabase(context)

    @Test
    fun roundTrip_realBackup() = runTest {
        db().noteDao().addNote(Note(title = "Encrypted round trip"))
        assertTrue(DatabaseRepository(context).exportDatabase(Uri.fromFile(plainBackup)))
        val masterKey = ByteArray(32).also(SecureRandom()::nextBytes)
        val kdfSalt = ByteArray(32).also(SecureRandom()::nextBytes)

        val sealed = ByteArrayOutputStream()
            .also { EncryptedBackup.encrypt(plainBackup, it, masterKey, kdfSalt) }
            .toByteArray()
        val input = ByteArrayInputStream(sealed)
        EncryptedBackup.decrypt(input, BackupHeader.read(input), masterKey, decrypted)

        assertArrayEquals(plainBackup.readBytes(), decrypted.readBytes())
        assertEquals(setOf("Encrypted round trip"), titlesIn(decrypted))
    }

    /**
     * The shared test file made by this client, `backup/encrypted-v1-android.backup`. It is a
     * schema version 3 backup whose user notes are titled [SHARED_TITLES], encrypted with
     * [SHARED_PASSPHRASE] (plain ASCII, so normalising leaves it as it is) and the written
     * Argon2id settings. Gnoseis-Windows copies the file and checks the same.
     */
    @Test
    fun sharedFile_madeByAndroid_decrypts() {
        val input = javaClass.classLoader!!.getResourceAsStream("backup/encrypted-v1-android.backup")
        input.use { EncryptedBackup.decrypt(it, decrypted, SHARED_PASSPHRASE.toByteArray(Charsets.UTF_8)) }

        SQLiteDatabase.openDatabase(decrypted.path, null, SQLiteDatabase.OPEN_READONLY).use {
            assertEquals(3, it.version)
        }
        assertEquals(SHARED_TITLES, titlesIn(decrypted))
    }

    private fun titlesIn(file: File): Set<String> =
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { raw ->
            raw.rawQuery("SELECT title FROM Note WHERE ownerDbId != ?", arrayOf(SEED_OWNER)).use { c ->
                buildSet { while (c.moveToNext()) add(c.getString(0)) }
            }
        }

    companion object {
        const val SHARED_PASSPHRASE = "gnoseis shared test backup made on android"
        val SHARED_TITLES = setOf("Shared backup note one", "Shared backup note two", "Shared backup note three")
        private const val DB_NAME = "gnoseis_data"
        private const val SEED_OWNER = "yyy"
    }
}
