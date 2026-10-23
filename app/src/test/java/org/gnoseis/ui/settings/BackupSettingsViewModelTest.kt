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
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.repository.DatabaseRepository
import org.gnoseis.testutil.MainDispatcherRule
import org.gnoseis.testutil.waitFor
import org.junit.After
import org.junit.Assert.assertFalse
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
    private val vm = mainDispatcherRule.viewModel { BackupSettingsViewModel(DatabaseRepository(context)) }

    @Before
    fun setUp() {
        GnoseisDatabase.resetInstanceForTesting()
        context.deleteDatabase("gnoseis_data")
        backup.delete()
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
    }

    @Test
    fun initialState_nothingSucceededYet() {
        assertFalse(vm.exportuccess.value)
        assertFalse(vm.importuccess.value)
        assertFalse(vm.exportFailed.value)
        assertFalse(vm.importFailed.value)
    }

    @Test
    fun export_returnValueIsAlwaysFalse_butStateReportsSuccess() = runTest {
        // Documented: the Boolean return value is not the result; exportuccess is.
        assertFalse(vm.exportDatabase(Uri.fromFile(backup)))

        waitFor { vm.exportuccess.value }
        assertTrue(backup.length() > 0)
    }

    @Test
    fun export_failure_reportsFailure() = runTest {
        vm.exportDatabase(Uri.fromFile(File(context.cacheDir, "missing-dir/x.backup")))
        waitFor { vm.exportFailed.value }
        assertFalse(vm.exportuccess.value)
    }

    @Test
    fun import_reportsSuccess() = runTest {
        vm.exportDatabase(Uri.fromFile(backup))
        waitFor { vm.exportuccess.value }

        assertFalse(vm.importDatabase(Uri.fromFile(backup)))

        waitFor { vm.importuccess.value }
        assertFalse(vm.importFailed.value)
    }

    @Test
    fun import_invalidFile_reportsFailure_untilDismissed() = runTest {
        backup.writeText("this is not an sqlite database")

        vm.importDatabase(Uri.fromFile(backup))

        waitFor { vm.importFailed.value }
        assertFalse(vm.importuccess.value)
        vm.dismissResult()
        assertFalse(vm.importFailed.value)
    }
}
