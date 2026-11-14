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

package org.gnoseis.ui.settings

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.backup.BackupKeys
import org.gnoseis.data.backup.FakeKeyWrapper
import org.gnoseis.data.backup.StoredBackupKey
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.repository.DatabaseRepository
import org.gnoseis.testutil.MainDispatcherRule
import org.gnoseis.testutil.waitFor
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val backup = File(context.cacheDir, "vm_test.backup")
    private val backupUri: Uri get() = Uri.fromFile(backup)
    private val keyFile = File(context.noBackupFilesDir, "vm_test_key")
    private val wrapper = FakeKeyWrapper()
    private val backupKey = StoredBackupKey(keyFile, wrapper)
    private val repository = DatabaseRepository(context, backupKey = backupKey)
    private val vm by lazy { mainDispatcherRule.viewModel { BackupSettingsViewModel(repository, backupKey) } }

    @Before
    fun setUp() {
        GnoseisDatabase.resetInstanceForTesting()
        context.deleteDatabase("gnoseis_data")
        backup.delete()
        keyFile.delete()
        runBlocking {
            val dao = GnoseisDatabase.getDatabase(context).noteDao()
            waitFor { runBlocking { dao.getNotes().first().isNotEmpty() } } // seed note (#20)
            dao.addNote(Note(title = "A"))
        }
    }

    @After
    fun tearDown() {
        mainDispatcherRule.clearViewModels() // before the database closes (#25)
        GnoseisDatabase.resetInstanceForTesting()
        context.deleteDatabase("gnoseis_data")
        backup.delete()
        keyFile.delete()
    }

    private fun startsWith(prefix: String) = backup.readBytes().copyOf(prefix.length).contentEquals(prefix.toByteArray())

    // ---------- Plain backups, as before ----------

    @Test
    fun initialState_nothingShown_encryptionOff() = runTest {
        waitFor { vm.encryption.value == BackupEncryption.OFF }
        assertNull(vm.message.value)
        assertNull(vm.passphrasePrompt.value)
        assertFalse(vm.keepOfferWaiting)
    }

    @Test
    fun export_reportsAPlainBackup() = runTest {
        vm.exportDatabase(backupUri)

        waitFor { vm.message.value == BackupMessage.EXPORTED_PLAIN }
        assertTrue(startsWith("SQLite format 3"))
    }

    @Test
    fun export_failure_reportsFailure() = runTest {
        vm.exportDatabase(Uri.fromFile(File(context.cacheDir, "missing-dir/x.backup")))
        waitFor { vm.message.value == BackupMessage.EXPORT_FAILED }
    }

    @Test
    fun import_reportsSuccess() = runTest {
        vm.exportDatabase(backupUri)
        waitFor { vm.message.value == BackupMessage.EXPORTED_PLAIN }
        vm.dismissResult()

        vm.importDatabase(backupUri)

        waitFor { vm.message.value == BackupMessage.RESTORED }
    }

    @Test
    fun import_invalidFile_reportsNotABackup_untilDismissed() = runTest {
        backup.writeText("this is not an sqlite database")

        vm.importDatabase(backupUri)

        waitFor { vm.message.value == BackupMessage.NOT_A_BACKUP }
        vm.dismissResult()
        assertNull(vm.message.value)
    }

    @Test
    fun import_missingFile_reportsReadFailed() = runTest {
        vm.importDatabase(Uri.fromFile(File(context.cacheDir, "does-not-exist.backup")))
        waitFor { vm.message.value == BackupMessage.READ_FAILED }
    }

    @Test
    fun suggestedFileName_isMarkedEncryptedOnlyWhileEncrypting() = runTest {
        waitFor { vm.encryption.value == BackupEncryption.OFF }
        assertEquals("gnoseis_data_X.backup", vm.suggestedFileName("X"))

        backupKey.setPassphrase(PASSPHRASE.toByteArray())
        vm.refresh()
        waitFor { vm.encryption.value == BackupEncryption.ON }

        assertEquals("gnoseis_data_X.encrypted.backup", vm.suggestedFileName("X"))
        assertEquals("gnoseis_data_X.backup", vm.suggestedFileName("X", plain = true))
    }

    // ---------- With encryption on ----------

    @Test
    fun withEncryptionOn_exportIsEncrypted_andPlainExportIsPlain() = runTest {
        backupKey.setPassphrase(PASSPHRASE.toByteArray())

        vm.exportDatabase(backupUri)
        waitFor { vm.message.value == BackupMessage.EXPORTED_ENCRYPTED }
        assertTrue(startsWith("GNOSEISB"))
        vm.dismissResult()

        vm.exportDatabase(backupUri, plain = true)
        waitFor { vm.message.value == BackupMessage.EXPORTED_PLAIN }
        assertTrue(startsWith("SQLite format 3"))
    }

    @Test
    fun withAnUnreadableKey_encryptionShowsUnreadable_andExportIsRefused() = runTest {
        backupKey.setPassphrase(PASSPHRASE.toByteArray())
        wrapper.failUnwrap = true

        waitFor { vm.encryption.value == BackupEncryption.UNREADABLE }
        vm.exportDatabase(backupUri)

        waitFor { vm.message.value == BackupMessage.KEY_UNREADABLE }
        assertFalse(backup.exists())
    }

    @Test
    fun turnOff_deletesTheKey() = runTest {
        backupKey.setPassphrase(PASSPHRASE.toByteArray())
        waitFor { vm.encryption.value == BackupEncryption.ON }

        vm.turnOffEncryption()

        waitFor { vm.encryption.value == BackupEncryption.OFF }
        assertFalse(keyFile.exists())
    }

    // ---------- Restore prompt and keep offer ----------

    /** An encrypted backup made with [PASSPHRASE] on a device whose own key is then turned off. */
    private suspend fun encryptedBackupFromAnotherDevice() {
        backupKey.setPassphrase(PASSPHRASE.toByteArray())
        repository.exportDatabase(backupUri)
        backupKey.turnOff()
    }

    @Test
    fun import_encryptedWithoutAMatchingKey_asksForThePassphrase() = runTest {
        encryptedBackupFromAnotherDevice()

        vm.importDatabase(backupUri)

        waitFor { vm.passphrasePrompt.value == PassphrasePrompt(backupUri) }
        assertNull(vm.message.value)
    }

    @Test
    fun wrongPassphrase_keepsThePromptOpen_thenTheRightOneRestores() = runTest {
        encryptedBackupFromAnotherDevice()
        vm.importDatabase(backupUri)
        waitFor { vm.passphrasePrompt.value != null }

        val wrong = "not the passphrase".toCharArray()
        vm.submitPassphrase(wrong)
        waitFor { vm.passphrasePrompt.value?.wrongPassphrase == true }
        assertTrue(wrong.all { it == '\u0000' })
        assertNull(vm.message.value)

        vm.submitPassphrase(PASSPHRASE.toCharArray())
        waitFor { vm.message.value == BackupMessage.RESTORED }
        assertNull(vm.passphrasePrompt.value)
    }

    @Test
    fun cancelPassphrase_closesThePrompt() = runTest {
        encryptedBackupFromAnotherDevice()
        vm.importDatabase(backupUri)
        waitFor { vm.passphrasePrompt.value != null }

        vm.cancelPassphrase()

        assertNull(vm.passphrasePrompt.value)
        assertNull(vm.message.value)
    }

    @Test
    fun keepOffer_appearsAfterTheRestoreMessage_andKeepsTheKey() = runTest {
        encryptedBackupFromAnotherDevice()
        vm.importDatabase(backupUri)
        waitFor { vm.passphrasePrompt.value != null }
        vm.submitPassphrase(PASSPHRASE.toCharArray())
        waitFor { vm.message.value == BackupMessage.RESTORED }
        assertFalse(vm.keepOfferWaiting)

        vm.dismissResult()
        assertTrue(vm.keepOfferWaiting)
        vm.keepEncrypting()

        waitFor { vm.encryption.value == BackupEncryption.ON }
        assertFalse(vm.keepOfferWaiting)
        val state = backupKey.state() as StoredBackupKey.On
        assertArrayEquals(BackupKeys.deriveMasterKey(PASSPHRASE.toByteArray(), state.kdfSalt, state.kdf), state.masterKey)
    }

    @Test
    fun keepOffer_notNow_leavesEncryptionOff() = runTest {
        encryptedBackupFromAnotherDevice()
        vm.importDatabase(backupUri)
        waitFor { vm.passphrasePrompt.value != null }
        vm.submitPassphrase(PASSPHRASE.toCharArray())
        waitFor { vm.message.value == BackupMessage.RESTORED }
        vm.dismissResult()

        vm.declineKeepOffer()

        assertFalse(vm.keepOfferWaiting)
        assertEquals(StoredBackupKey.Off, backupKey.state())
    }

    @Test
    fun import_newerEncryptedFormat_reportsNewerFormat() = runTest {
        backupKey.setPassphrase(PASSPHRASE.toByteArray())
        repository.exportDatabase(backupUri)
        backup.writeBytes(backup.readBytes().also { it[8] = 2 })

        vm.importDatabase(backupUri)

        waitFor { vm.message.value == BackupMessage.NEWER_FORMAT }
    }

    @Test
    fun import_damagedEncryptedFile_withTheStoredKey_reportsWrongPassphraseOrDamaged() = runTest {
        backupKey.setPassphrase(PASSPHRASE.toByteArray())
        repository.exportDatabase(backupUri)
        val bytes = backup.readBytes()
        bytes[200] = (bytes[200].toInt() xor 1).toByte()
        backup.writeBytes(bytes)

        vm.importDatabase(backupUri)

        waitFor { vm.message.value == BackupMessage.WRONG_PASSPHRASE_OR_DAMAGED }
        assertNull(vm.passphrasePrompt.value)
    }

    private companion object {
        const val PASSPHRASE = "the view model test passphrase"
    }
}
