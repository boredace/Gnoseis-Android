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

import androidx.activity.ComponentActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory.Companion.VIEW_MODEL_KEY
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.gnoseis.data.enums.RecordEditPageMode
import org.gnoseis.data.enums.RecordType
import org.gnoseis.testutil.TestAppContainer
import org.gnoseis.ui.category.CategoryEditViewModel
import org.gnoseis.ui.category.CategoryListViewModel
import org.gnoseis.ui.contact.ContactEditViewModel
import org.gnoseis.ui.contact.ContactListViewModel
import org.gnoseis.ui.home.HomeViewModel
import org.gnoseis.ui.item.ItemEditViewModel
import org.gnoseis.ui.item.ItemListViewModel
import org.gnoseis.ui.note.NoteEditViewModel
import org.gnoseis.ui.note.NoteListViewModel
import org.gnoseis.ui.organization.OrganizationEditViewModel
import org.gnoseis.ui.organization.OrganizationListViewModel
import org.gnoseis.ui.settings.BackupSettingsViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

/**
 * The factory resolves dependencies from GnoseisApplication.container, which tests can swap
 * for a TestAppContainer. ViewModels needing a SavedStateHandle get their route from an
 * activity's saved state; their behaviour is covered by their own tests.
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
            HomeViewModel::class.java,
            ItemListViewModel::class.java,
            NoteListViewModel::class.java,
            OrganizationListViewModel::class.java,
        ).forEach { clazz ->
            assertTrue(clazz.isInstance(create(clazz)))
        }
    }

    @Test
    fun createsEditViewModelsWithTheirRouteAndLinkSourceUseCase() {
        val owner = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        fun <T : ViewModel> createWithRoute(clazz: Class<T>, vararg args: Pair<String, Any?>): T =
            AppViewModelProvider.Factory.create(clazz, MutableCreationExtras().apply {
                set(APPLICATION_KEY, app)
                set(SAVED_STATE_REGISTRY_OWNER_KEY, owner)
                set(VIEW_MODEL_STORE_OWNER_KEY, owner)
                set(VIEW_MODEL_KEY, clazz.name)
                set(DEFAULT_ARGS_KEY, bundleOf(*args))
            })
        val link = arrayOf("linkFromType" to RecordType.Note, "linkFromId" to "note-id")

        val vms = listOf(
            createWithRoute(CategoryEditViewModel::class.java, "recordType" to RecordType.Category, "recordId" to null, "pageMode" to RecordEditPageMode.NEWLINK, *link),
            createWithRoute(ContactEditViewModel::class.java, "recordType" to RecordType.Contact, "recordId" to null, "pageMode" to RecordEditPageMode.NEWLINK, *link),
            createWithRoute(ItemEditViewModel::class.java, "recordType" to RecordType.Item, "recordId" to null, "pageMode" to RecordEditPageMode.NEWLINK, *link),
            createWithRoute(NoteEditViewModel::class.java, "recordType" to RecordType.Note, "recordId" to null, "pageMode" to RecordEditPageMode.NEWLINK, *link),
            createWithRoute(OrganizationEditViewModel::class.java, "recordType" to RecordType.Organization, "recordId" to null, "pageMode" to RecordEditPageMode.NEWLINK, *link),
        )

        assertEquals(
            listOf(CategoryEditViewModel::class, ContactEditViewModel::class, ItemEditViewModel::class, NoteEditViewModel::class, OrganizationEditViewModel::class),
            vms.map { it::class },
        )
    }
}
