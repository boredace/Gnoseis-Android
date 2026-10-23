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

package org.gnoseis.ui.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
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
import org.gnoseis.data.repository.CategoryRepository
import org.gnoseis.data.repository.LinkedRecordRepository
import org.gnoseis.domain.usecase.DeleteRecordUseCase

class CategoryDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository,
    private var linkedRecordRepository: LinkedRecordRepository


) : ViewModel(){

    private val deleteRecord : DeleteRecordUseCase =
        DeleteRecordUseCase(
            categoryRepository = categoryRepository,
            linkedRecordRepository = linkedRecordRepository,
        )

    private var categoryId : String = checkNotNull(savedStateHandle.toRoute<CategoryDetailsRoute>().categoryId)

    val linkedRecords: StateFlow<CategoryLinkedRecords> =
        combine(
            linkedRecordRepository.getLinkedContactListByCategoryId(categoryId),
            linkedRecordRepository.getLinkedItemListByCategoryId(categoryId),
            linkedRecordRepository.getLinkedNoteListByCategoryId(categoryId),
            linkedRecordRepository.getLinkedOrganizationListByCategoryId(categoryId)
        ){
                linkedContacts, linkedItems, linkedNotes, linkedOrganizations ->
            CategoryLinkedRecords(
                linkedContacts = linkedContacts,
                linkedItems = linkedItems,
                linkedNotes = linkedNotes,
                linkedOrganizations = linkedOrganizations
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            CategoryLinkedRecords()
        )

    val categoryDetailsPageState: StateFlow<CategoryDetailsPageState> =
        combine(
            categoryRepository.getCategoryStream(categoryId),
            linkedRecordRepository.getLinkedRecordTypesForRecordIdStream(categoryId)
        ){
            category, linkedRecordTypes ->
            CategoryDetailsPageState(
                category = category,
                linkedRecordTabs = linkedRecordTypes
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            CategoryDetailsPageState()
        )

    fun deleteCategory() {
        viewModelScope.launch {
            deleteRecord(categoryId)
        }
    }
    data class CategoryDetailsPageState(
        val category: Category? = Category(ownerDbId = "dbx", categoryName = "xxx"),
        val linkedRecordTabs: List<LinkedRecordTypeCount> = emptyList()
    )

    data class CategoryLinkedRecords(
        val linkedContacts: List<Contact> = emptyList(),
        val linkedItems: List<Item> = emptyList(),
        val linkedNotes: List<Note> = emptyList(),
        val linkedOrganizations: List<Organization> = emptyList()
    )

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}