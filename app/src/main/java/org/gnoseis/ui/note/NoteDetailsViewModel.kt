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

package org.gnoseis.ui.note

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
import org.gnoseis.data.repository.NoteRepository
import org.gnoseis.domain.usecase.DeleteRecordUseCase

class NoteDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
    private var linkedRecordRepository: LinkedRecordRepository

) : ViewModel(){
    private val deleteRecord : DeleteRecordUseCase =
        DeleteRecordUseCase(
            noteRepository = noteRepository,
            linkedRecordRepository = linkedRecordRepository,
        )
    private var noteId : String = checkNotNull(savedStateHandle[NoteDetailsPageDestination.noteIdArg])

    val linkedRecords: StateFlow<NoteLinkedRecords> =
        combine(
            linkedRecordRepository.getLinkedCategoryListByNoteId(noteId),
            linkedRecordRepository.getLinkedContactListByNoteId(noteId),
            linkedRecordRepository.getLinkedItemListByNoteId(noteId),
            linkedRecordRepository.getLinkedOrganizationListByNoteId(noteId)
        ){
            linkedCategories, linkedContacts, linkedItems, linkedOrganizations ->
            NoteLinkedRecords(
                linkedCategories = linkedCategories,
                linkedContacts = linkedContacts,
                linkedItems = linkedItems,
                linkedOrganizations = linkedOrganizations
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            NoteLinkedRecords()
        )
    fun deleteNote() {
        viewModelScope.launch {
            deleteRecord(noteId)
        }
    }
    val noteDetailsPageState: StateFlow<NoteDetailsPageState> =
        combine(
            noteRepository.getNoteStream(noteId),
            linkedRecordRepository.getLinkedRecordTypesForRecordIdStream(noteId)
        ){
            note, linkedRecordTypes ->
            NoteDetailsPageState(
                note = note,
                linkedRecordTabs = linkedRecordTypes
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            NoteDetailsPageState(note = Note(ownerDbId = "xx"), linkedRecordTabs = emptyList())
        )
    data class NoteDetailsPageState(
        val note: Note? = Note(ownerDbId = "dbx"),
        val linkedRecordTabs: List<LinkedRecordTypeCount> = emptyList()
    )
    data class NoteLinkedRecords(
        val linkedCategories: List<Category> = emptyList(),
        val linkedContacts: List<Contact> = emptyList(),
        val linkedItems: List<Item> = emptyList(),
        val linkedOrganizations: List<Organization> = emptyList(),
    )



    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}