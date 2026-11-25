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

package org.gnoseis.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.backup.BackupKeys
import org.gnoseis.data.backup.BackupPassphrase.OwnCheck
import org.gnoseis.data.backup.EffWordList
import org.gnoseis.data.backup.FakeKeyWrapper
import org.gnoseis.data.backup.KeyWrapper
import org.gnoseis.data.backup.StoredBackupKey
import org.gnoseis.testutil.MainDispatcherRule
import org.gnoseis.testutil.waitFor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Setting and changing the backup passphrase (#66). */
@RunWith(AndroidJUnit4::class)
class PassphraseSetupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val keyFile = File(context.noBackupFilesDir, "setup_test_key").apply { delete() }
    private val backupKey = StoredBackupKey(keyFile, FakeKeyWrapper())
    private val words = EffWordList.load(context)
    private val vm by lazy { mainDispatcherRule.viewModel { PassphraseSetupViewModel(backupKey) { words } } }

    @After
    fun tearDown() {
        mainDispatcherRule.clearViewModels()
        keyFile.delete()
    }

    private fun storedKeyIsFor(passphrase: String): Boolean {
        val state = backupKey.state() as? StoredBackupKey.On ?: return false
        return BackupKeys.deriveMasterKey(passphrase.toByteArray(), state.kdfSalt, state.kdf).contentEquals(state.masterKey)
    }

    // ---------- Generated ----------

    @Test
    fun generated_showsTwentyWordsFromTheList_andCopyLeavesOutTheNumbers() = runTest {
        vm.generate()
        waitFor { vm.step.value == SetupStep.GENERATED }

        val shown = vm.generatedWords.value
        assertEquals(20, shown.size)
        assertTrue(shown.all { it in words })
        assertEquals(shown.joinToString(" "), vm.generatedText())
    }

    @Test
    fun generated_confirmedWithLineBreaksAndSpaces_turnsEncryptionOn() = runTest {
        vm.generate()
        waitFor { vm.step.value == SetupStep.GENERATED }
        val shown = vm.generatedWords.value
        vm.generatedNext()

        // Written down in rows of four, then typed back
        vm.onConfirmChange(shown.chunked(4).joinToString("\n") { it.joinToString("  ") } + "\n")
        vm.turnOn()

        waitFor { vm.step.value == SetupStep.DONE }
        assertTrue(storedKeyIsFor(shown.joinToString(" ")))
    }

    @Test
    fun generated_wordsInAnotherOrder_doNotMatch() = runTest {
        vm.generate()
        waitFor { vm.step.value == SetupStep.GENERATED }
        val shown = vm.generatedWords.value
        vm.generatedNext()

        vm.onConfirmChange(shown.reversed().joinToString(" "))
        vm.turnOn()

        assertTrue(vm.confirmMismatch.value)
        assertEquals(SetupStep.CONFIRM, vm.step.value)
        assertEquals(StoredBackupKey.Off, backupKey.state())
    }

    // ---------- Own ----------

    @Test
    fun own_underTwelveCharacters_isRefused() {
        vm.useOwn()
        vm.onOwnChange("short one")

        assertEquals(OwnCheck.TOO_SHORT, vm.ownCheck.value)
        assertEquals(9, vm.ownLength.value)
        vm.ownNext()

        assertTrue(vm.showOwnError.value)
        assertFalse(vm.showUseAnyway.value)
        assertEquals(SetupStep.OWN, vm.step.value)
    }

    @Test
    fun own_withAnUnusableCharacter_isRefused() {
        vm.useOwn()
        vm.onOwnChange("long enough but ￾")
        vm.ownNext()

        assertEquals(OwnCheck.UNUSABLE_CHARACTER, vm.ownCheck.value)
        assertTrue(vm.showOwnError.value)
        assertFalse(vm.showUseAnyway.value)
    }

    @Test
    fun own_alwaysNeedsUseItAnyway_becauseThereIsNoEstimate() {
        vm.useOwn()
        vm.onOwnChange("a long and sensible passphrase")
        vm.ownNext()

        assertTrue(vm.showUseAnyway.value)
        assertEquals(SetupStep.OWN, vm.step.value)

        vm.dismissUseAnyway()
        assertFalse(vm.showUseAnyway.value)
        assertEquals(SetupStep.OWN, vm.step.value)
    }

    @Test
    fun own_useItAnyway_thenConfirm_turnsEncryptionOn() = runTest {
        vm.useOwn()
        vm.onOwnChange("  my own   backup passphrase ")
        vm.ownNext()
        vm.useAnyway()
        assertEquals(SetupStep.CONFIRM, vm.step.value)

        vm.onConfirmChange("my own backup passphrase")
        vm.turnOn()

        waitFor { vm.step.value == SetupStep.DONE }
        assertTrue(storedKeyIsFor("my own backup passphrase"))
    }

    @Test
    fun back_goesStepByStep_thenCloses() {
        vm.useOwn()
        vm.onOwnChange("a long and sensible passphrase")
        vm.ownNext()
        vm.useAnyway()
        vm.onConfirmChange("something")

        assertTrue(vm.back())
        assertEquals(SetupStep.OWN, vm.step.value)
        assertEquals("", vm.confirm.value)
        assertTrue(vm.back())
        assertEquals(SetupStep.CHOOSE, vm.step.value)
        assertFalse(vm.back())
    }

    // ---------- Change ----------

    @Test
    fun change_replacesTheStoredKey() = runTest {
        backupKey.setPassphrase("the old passphrase".toByteArray())
        waitFor { vm.isChange.value }

        vm.useOwn()
        vm.onOwnChange("the new passphrase")
        vm.ownNext()
        vm.useAnyway()
        vm.onConfirmChange("the new passphrase")
        vm.turnOn()

        waitFor { vm.step.value == SetupStep.DONE }
        assertTrue(storedKeyIsFor("the new passphrase"))
        assertFalse(storedKeyIsFor("the old passphrase"))
    }

    // ---------- Failure ----------

    @Test
    fun outOfMemory_whileSaving_showsTheFailureInsteadOfSpinning() = runTest {
        val noMemory = object : KeyWrapper {
            override fun wrap(plain: ByteArray, associatedData: ByteArray): ByteArray = throw OutOfMemoryError()
            override fun unwrap(wrapped: ByteArray, associatedData: ByteArray): ByteArray = throw IllegalStateException()
            override fun delete() = Unit
        }
        val failing = mainDispatcherRule.viewModel {
            PassphraseSetupViewModel(StoredBackupKey(keyFile, noMemory)) { words }
        }
        failing.useOwn()
        failing.onOwnChange("a long and sensible passphrase")
        failing.ownNext()
        failing.useAnyway()
        failing.onConfirmChange("a long and sensible passphrase")
        failing.turnOn()

        waitFor { failing.saveFailed.value }
        assertEquals(SetupStep.CONFIRM, failing.step.value)
        assertEquals(StoredBackupKey.Off, backupKey.state())
    }

    @Test
    fun firstSetup_isNotAChange() = runTest {
        vm.useOwn()
        assertFalse(vm.isChange.value)
    }
}
