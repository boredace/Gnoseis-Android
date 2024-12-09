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

import android.util.Log
import androidx.compose.material3.CalendarLocale
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.gnoseis.data.entity.links.LinkedRecord
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.enums.NoteEditPageMode
import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.repository.LinkedRecordRepository
import org.gnoseis.data.repository.NoteRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
class NoteEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
    private val linkedRecordRepository: LinkedRecordRepository,

    ) : ViewModel(){

    private val noteId : String? = savedStateHandle.toRoute<NoteEditRoute>().noteId

    private var editDate = MutableStateFlow(LocalDateTime.of(
        LocalDate.now(), LocalTime.MIDNIGHT).toEpochSecond(ZoneOffset.UTC)*1000)

//    private var note = MutableStateFlow(Note())


    @OptIn(ExperimentalMaterial3Api::class)
    private var _datePickerState = MutableStateFlow(DatePickerState(
        locale = CalendarLocale.getDefault(),
        initialSelectedDateMillis = LocalDateTime.of(
            LocalDate.now(), LocalTime.MIDNIGHT).toEpochSecond(ZoneOffset.UTC)*1000
    ))

    @OptIn(ExperimentalMaterial3Api::class)
    var datePickerState = _datePickerState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        DatePickerState(
            locale = CalendarLocale.getDefault(),
            )
        )

    private val _editState = mutableStateOf(EditState(
        editDate = editDate.value
    ))
    val editState: State<EditState> = _editState

    private val _pageState = mutableStateOf(
        PageState(
            isNew = true,
            pageMode = checkNotNull(savedStateHandle.toRoute<NoteEditRoute>().pageMode),
            noteId =  null,
            linkFromType = savedStateHandle.toRoute<NoteEditRoute>().linkFromType,
            linkFromId = savedStateHandle.toRoute<NoteEditRoute>().linkFromId,
            note = null
        )
    )
    val pageState: State<PageState> = _pageState

    init {
        if (!noteId.isNullOrEmpty()) {
            viewModelScope.launch {
                noteRepository.getNote(noteId).let {
                    _pageState.value = _pageState.value.copy(
                        isNew = false,
                        pageMode = checkNotNull(savedStateHandle.toRoute<NoteEditRoute>().pageMode),
                        noteId = it.id,
                        linkFromType = savedStateHandle.toRoute<NoteEditRoute>().linkFromType,
                        linkFromId = savedStateHandle.toRoute<NoteEditRoute>().linkFromId,
                        note = it
                    )

                    _editState.value = _editState.value.copy(
                        isValid = true,
                        editTitle = it.title?:"",
                        editTextContent = it.textContents?:"",
                        editDate = it.createDateTime,
                    )
                    _datePickerState.value.selectedDateMillis = it.createDate
                }
            }
        }
    }

    suspend fun onSave(): String? {
        if(
            // Add new Note
            _editState.value.isValid &&
            _pageState.value.isNew
        ) {
            val addedNoteRowId = noteRepository.addNote(
                Note(
                    ownerDbId = "db1",
                    title = _editState.value.editTitle,
                    textContents =_editState.value.editTextContent,
                    createDate = _editState.value.editDate,
                    createDateTime = _editState.value.editDate
                        ?: 0 //TODO: Right now this is only date (time set to GMT 00:00) but in future need to enable time picker and pupulate this with acutal date/time
                )
            )
            val addedNoteId = noteRepository.getNoteByRowId(addedNoteRowId).id
            if (
                // Also link to source record
                _pageState.value.pageMode == NoteEditPageMode.NEWLINK &&
                _pageState.value.linkFromId != null &&
                _pageState.value.linkFromType != null
            ){
                linkedRecordRepository.addLinkedRecord(
                    LinkedRecord(
                        ownerDbId = "db1",
                        record1Id = _pageState.value.linkFromId!!,
                        record2Id = addedNoteId,
                        record1TypeId = _pageState.value.linkFromType!!.recordTypeId,
                        record2TypeId = RecordType.Note.recordTypeId
                    )
                )
            }
            return addedNoteId
        } else if (
            // Update Existing Note
            _editState.value.isValid &&
            !_pageState.value.isNew
        ) {
            noteRepository.updateNote(_pageState.value.note!!.copy(
                title = _editState.value.editTitle,
                textContents = _editState.value.editTextContent,
                createDate = _editState.value.editDate,
                createDateTime = _editState.value.editDate,
            ))
            return null
        } else {
            return null
        }
    }
    fun updateDate(it: Long) {
        _editState.value = _editState.value.copy(
        editDate = it
        )
        //        dateTimeHolder.value.dateString = Instant.ofEpochMilli(it).toString()
    }
    fun updateTime(h: Int, m: Int) {
//        dateTimeHolder.value.localHour = h
//        dateTimeHolder.value.localMinute = m
    }

    fun updateTitle(it: String) {
        // update editTitle only immediately
        _editState.value = _editState.value.copy(editTitle = it)
        debounceJob?.cancel()
        // perform validations and update only when stopped typing
        debounceJob = CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            _editState.value = _editState.value.copy(
                isValid = validateInput(it, _editState.value.editTextContent?:""),
                editTitle = it,
            )
            Log.i("DEBOUNCE", "title updated")
        }
    }

    private var debounceJob: Job? = null

    fun updateTextContent(it: String) {
        // update editTextContent only immediately
        _editState.value = _editState.value.copy(editTextContent = it)
        debounceJob?.cancel()
        // perform validations and update only when stopped typing
        debounceJob = CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            _editState.value = _editState.value.copy(
                isValid = validateInput(_editState.value.editTitle?:"", it),
                editTextContent = it,
            )
            Log.i("DEBOUNCE", "content updated")
        }
    }

    private fun validateInput(title: String, textContent: String):Boolean {
        var tempVal = false
        if(title.isNotEmpty()) tempVal = true
        if(textContent.isNotEmpty()) tempVal = true
        return tempVal
    }

    data class PageState(
        val isNew: Boolean = true,
        val pageMode: NoteEditPageMode? = null,
        val noteId: String? = null,
        val linkFromType: RecordType? = null,
        val linkFromId: String? = null,
        val note: Note? = Note(),
    )

    data class EditState(
        val isValid: Boolean = false,
        val editTitle: String? = null,
        val editTextContent: String? = null,
        val editDate: Long = 0L,
//        val selectedDate: Long? = 0L,
//        val selectedTime: Long? = 0L,
    )

    data class DateTimeHolder(
        var dateMillis: Long = 0L,
//        var localHour: Int? = 0,
//        var localMinute: Int? = 0,
    )

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}