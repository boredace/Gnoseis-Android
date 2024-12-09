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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.repository.NoteRepository

class NoteListViewModel(
    private val noteRepository: NoteRepository,
) : ViewModel(){
    val notePageState : StateFlow<NotePageState> =
        noteRepository.getNotesStream()
            .map{
                NotePageState(it
                    /*it.map {

                        Note(
                            id = it.id,
                            ownerDbId = it.ownerDbId,
                            title = it.title,
                            textContents = it.textContents,
                            createDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(it.createDateTime), ZoneId.of("Europe/London")).toInstant().toEpochMilli(),
                            createDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(it.createDate), ZoneId.of("GMT+9")).toInstant().toEpochMilli()

                        )
                    }*/
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                NotePageState()
            )



    data class ListEntryAnnotatedString(
        val boldUntil: Int,
        val consolidatedString: String
    )

    data class NotePageState(
        val notes: List<Note> = emptyList()

    )


    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}