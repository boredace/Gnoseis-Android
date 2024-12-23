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

package org.gnoseis.ui.contact

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.gnoseis.data.entity.category.Category
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.links.LinkedRecordTypeCount
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.repository.ContactRepository
import org.gnoseis.data.repository.LinkedRecordRepository
import org.gnoseis.domain.usecase.DeleteRecordUseCase

class ContactDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val contactRepository: ContactRepository,
    private var linkedRecordRepository: LinkedRecordRepository


) : ViewModel(){
    private val deleteRecord : DeleteRecordUseCase =
        DeleteRecordUseCase(
            contactRepository = contactRepository,
            linkedRecordRepository = linkedRecordRepository,
        )
    private var contactId : String = checkNotNull(savedStateHandle[ContactDetailsPageDestination.contactIdArg])

/*
    val item: StateFlow<Item> =
        itemRepository
            .getItemStream(itemId)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                Item(ownerDbId = "zzz", itemName = "xxx")
            )
*/


    val linkedRecords: StateFlow<ContactLinkedRecords> =
        combine(
            linkedRecordRepository.getLinkedCategoryListByContactId(contactId),
            linkedRecordRepository.getLinkedItemListByContactId(contactId),
            linkedRecordRepository.getLinkedNoteListByContactId(contactId),
            linkedRecordRepository.getLinkedOrganizationListByContactId(contactId)
        ){
            linkedCategories, linkedItems, linkedNotes, linkedOrganizations ->
            ContactLinkedRecords(
                linkedCategories = linkedCategories,
                linkedItems = linkedItems,
                linkedNotes = linkedNotes,
                linkedOrganizations = linkedOrganizations
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            ContactLinkedRecords()
        )

    fun deleteContact() {
        viewModelScope.launch {
            deleteRecord(contactId)
        }
    }
    val contactDetailsPageState: StateFlow<ContactDetailsPageState> =
        combine(
            contactRepository.getContactStream(contactId),
            linkedRecordRepository.getLinkedRecordTypesForRecordIdStream(contactId)
        ){
                contact, linkedRecordTypes ->
            ContactDetailsPageState(
                contact = contact,
                linkedRecordTabs = linkedRecordTypes
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            ContactDetailsPageState()
        )

    data class ContactDetailsPageState(
        val contact: Contact? = Contact(ownerDbId = "dbx", nameLast = "xxx", nameFirst = "yyy"),
        val linkedRecordTabs: List<LinkedRecordTypeCount> = emptyList()
    )

    data class ContactLinkedRecords(
        val linkedCategories: List<Category> = emptyList(),
        val linkedItems: List<Item> = emptyList(),
        val linkedNotes: List<Note> = emptyList(),
        val linkedOrganizations: List<Organization> = emptyList(),
    )



    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}