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

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.gnoseis.ui.category.CategoryDetailsViewModel
import org.gnoseis.ui.category.CategoryEditViewModel
import org.gnoseis.ui.category.CategoryListViewModel
import org.gnoseis.ui.contact.ContactDetailsViewModel
import org.gnoseis.ui.contact.ContactEditViewModel
import org.gnoseis.ui.contact.ContactListViewModel
import org.gnoseis.ui.item.ItemDetailsViewModel
import org.gnoseis.ui.item.ItemEditViewModel
import org.gnoseis.ui.item.ItemListViewModel
import org.gnoseis.ui.link.LinkRecordsViewModel
import org.gnoseis.ui.note.NoteDetailsViewModel
import org.gnoseis.ui.note.NoteEditViewModel
import org.gnoseis.ui.note.NoteListViewModel
import org.gnoseis.ui.organization.OrganizationDetailsViewModel
import org.gnoseis.ui.organization.OrganizationEditViewModel
import org.gnoseis.ui.organization.OrganizationListViewModel
import org.gnoseis.ui.search.SearchPageViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            CategoryListViewModel(
                gnoseisApplication().container.categoryRepository
            )
        }
        initializer {
            CategoryDetailsViewModel(
                this.createSavedStateHandle(),
                gnoseisApplication().container.categoryRepository,
                gnoseisApplication().container.linkedRecordRepository
            )
        }
        initializer {
            CategoryEditViewModel(
                this.createSavedStateHandle(),
                gnoseisApplication().container.categoryRepository
            )
        }
        initializer {
            ContactListViewModel(
                gnoseisApplication().container.contactRepository
            )
        }
        initializer {
            ContactDetailsViewModel(
                this.createSavedStateHandle(),
                gnoseisApplication().container.contactRepository,
                gnoseisApplication().container.linkedRecordRepository
            )
        }
        initializer {
            ContactEditViewModel(
                this.createSavedStateHandle(),
                gnoseisApplication().container.contactRepository,
                gnoseisApplication().container.linkedRecordRepository,
            )
        }
        initializer {
            ItemListViewModel(
                gnoseisApplication().container.itemRepository
            )
        }
        initializer {
            ItemDetailsViewModel(
                this.createSavedStateHandle(),
                gnoseisApplication().container.itemRepository,
                gnoseisApplication().container.linkedRecordRepository
            )
        }
        initializer {
            ItemEditViewModel(
                this.createSavedStateHandle(),
                gnoseisApplication().container.itemRepository
            )
        }
        initializer {
            NoteListViewModel(
                gnoseisApplication().container.noteRepository,
            )
        }
        initializer {
            NoteDetailsViewModel(
                this.createSavedStateHandle(),
                gnoseisApplication().container.noteRepository,
                gnoseisApplication().container.linkedRecordRepository
            )
        }
        initializer {
            NoteEditViewModel(
                this.createSavedStateHandle(),
                gnoseisApplication().container.noteRepository,
                gnoseisApplication().container.linkedRecordRepository,
            )
        }
        initializer {
            OrganizationListViewModel(
                gnoseisApplication().container.organizationRepository
            )
        }
        initializer {
            OrganizationDetailsViewModel(
                this.createSavedStateHandle(),
                gnoseisApplication().container.organizationRepository,
                gnoseisApplication().container.linkedRecordRepository
            )
        }
        initializer {
            OrganizationEditViewModel(
                this.createSavedStateHandle(),
                gnoseisApplication().container.organizationRepository
            )
        }


        initializer {
            LinkRecordsViewModel(
                this.createSavedStateHandle(),
                gnoseisApplication().container.linkedRecordRepository,
                gnoseisApplication().container.searchRepository
            )
        }
        initializer {
            SearchPageViewModel(
                this.createSavedStateHandle(),
                gnoseisApplication().container.searchRepository
            )
        }
    }
}

/**
 * Extension function to queries for [Application] object and returns an instance of
 * [GnoseisApplication].
 */

fun CreationExtras.gnoseisApplication(): GnoseisApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as GnoseisApplication)