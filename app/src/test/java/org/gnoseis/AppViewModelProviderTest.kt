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

package org.gnoseis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.gnoseis.testutil.TestAppContainer
import org.gnoseis.ui.category.CategoryListViewModel
import org.gnoseis.ui.contact.ContactListViewModel
import org.gnoseis.ui.item.ItemListViewModel
import org.gnoseis.ui.note.NoteListViewModel
import org.gnoseis.ui.organization.OrganizationListViewModel
import org.gnoseis.ui.settings.BackupSettingsViewModel
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The factory resolves dependencies from GnoseisApplication.container, which tests can swap
 * for a TestAppContainer. ViewModels needing a SavedStateHandle are covered by their own tests.
 */
@RunWith(AndroidJUnit4::class)
class AppViewModelProviderTest {
    private val app: GnoseisApplication = ApplicationProvider.getApplicationContext()
    private val container = TestAppContainer(app).also { app.container = it }

    @After
    fun tearDown() = container.close()

    private fun <T : ViewModel> create(clazz: Class<T>): T =
        AppViewModelProvider.Factory.create(clazz, MutableCreationExtras().apply { set(APPLICATION_KEY, app) })

    @Test
    fun createsViewModelsWithoutSavedState() {
        listOf(
            BackupSettingsViewModel::class.java,
            CategoryListViewModel::class.java,
            ContactListViewModel::class.java,
            ItemListViewModel::class.java,
            NoteListViewModel::class.java,
            OrganizationListViewModel::class.java,
        ).forEach { clazz ->
            assertTrue(clazz.isInstance(create(clazz)))
        }
    }
}
