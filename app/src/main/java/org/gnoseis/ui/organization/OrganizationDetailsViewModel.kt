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

package org.gnoseis.ui.organization

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
import org.gnoseis.data.repository.LinkedRecordRepository
import org.gnoseis.data.repository.OrganizationRepository
import org.gnoseis.domain.usecase.DeleteRecordUseCase

class OrganizationDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val organizationRepository: OrganizationRepository,
    private var linkedRecordRepository: LinkedRecordRepository


) : ViewModel(){
    private val deleteRecord : DeleteRecordUseCase =
        DeleteRecordUseCase(
            organizationRepository = organizationRepository,
            linkedRecordRepository = linkedRecordRepository,
        )
    private var organizationId : String = checkNotNull(savedStateHandle[OrganizationDetailsPageDestination.organizationIdArg])

    val linkedRecords: StateFlow<OrganizationLinkedRecords> =
        combine(
            linkedRecordRepository.getLinkedCategoryListByOrganizationId(organizationId),
            linkedRecordRepository.getLinkedContactListByOrganizationId(organizationId),
            linkedRecordRepository.getLinkedItemListByOrganizationId(organizationId),
            linkedRecordRepository.getLinkedNoteListByOrganizationId(organizationId)
        ){
            linkedCategories, linkedContacts, linkedItems, linkedNotes ->
            OrganizationLinkedRecords(
                linkedCategories = linkedCategories,
                linkedContacts = linkedContacts,
                linkedItems = linkedItems,
                linkedNotes = linkedNotes
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            OrganizationLinkedRecords()
        )

    val organizationDetailsPageState: StateFlow<OrganizationDetailsPageState> =
        combine(
            organizationRepository.getOrganizationStream(organizationId),
            linkedRecordRepository.getLinkedRecordTypesForRecordIdStream(organizationId)
        ){
                organization, linkedRecordTypes ->
            OrganizationDetailsPageState(
                organization = organization,
                linkedRecordTabs = linkedRecordTypes
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            OrganizationDetailsPageState()
        )
    fun deleteOrganization() {
        viewModelScope.launch {
            deleteRecord(organizationId)
        }
    }
    data class OrganizationDetailsPageState(
        val organization: Organization? = Organization(ownerDbId = "dbx", organizationName = "My Organization"),
        val linkedRecordTabs: List<LinkedRecordTypeCount> = emptyList()
    )

    data class OrganizationLinkedRecords(
        val linkedCategories: List<Category> = emptyList(),
        val linkedContacts: List<Contact> = emptyList(),
        val linkedItems: List<Item> = emptyList(),
        val linkedNotes: List<Note> = emptyList()
    )



    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}