/*
*   Gnoseis is an Android native CRM application and general knowledge manager.
*
*   Copyright (C) 2024-2026 Gnoseis.org
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

import org.gnoseis.data.constants.TITLE_LENGTH_NOTE
import org.gnoseis.ui.record.prefillName
import android.util.Log
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
import kotlinx.coroutines.launch
import org.gnoseis.data.entity.links.LinkedRecord
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.enums.RecordEditPageMode
import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.repository.LinkedRecordRepository
import org.gnoseis.data.repository.NoteRepository
import org.gnoseis.domain.usecase.GetLinkSourceUseCase
import org.gnoseis.domain.usecase.LinkSource
import org.gnoseis.data.time.NoteTimes
import org.gnoseis.data.time.StoredNoteTimes
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import org.gnoseis.ui.record.RecordEditRoute
import org.gnoseis.ui.record.alsoLinkRecords
import org.gnoseis.data.text.lineBreaksToSave
import org.gnoseis.data.text.normalizeLineBreaks

/**
 * #68: the note's day and time are edited as the local day and time in the device's time zone
 * ([zone]) and saved by [NoteTimes]. [now] is the system time (UTC epoch ms).
 */
class NoteEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
    private val linkedRecordRepository: LinkedRecordRepository,
    private val getLinkSource: GetLinkSourceUseCase,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) : ViewModel(){
    /** Records to link to the source as well when the new record is saved. */
    private val alsoLink = savedStateHandle.toRoute<RecordEditRoute>().alsoLinkRecords()


    private val noteId : String? = savedStateHandle.toRoute<RecordEditRoute>().recordId

    /** The day and time the editor opened with: a new note's opening moment, or the note's. */
    private var shownAtStart: LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(now()), zone()).truncatedTo(ChronoUnit.MINUTES)

    private val _editState = mutableStateOf(EditState(
        editDay = shownAtStart.toLocalDate(),
        editTime = shownAtStart.toLocalTime(),
    ))
    val editState: State<EditState> = _editState

    private val _pageState = mutableStateOf(
        PageState(
            isNew = true,
            pageMode = checkNotNull(savedStateHandle.toRoute<RecordEditRoute>().pageMode),
            noteId =  null,
            linkFromType = savedStateHandle.toRoute<RecordEditRoute>().linkFromType,
            linkFromId = savedStateHandle.toRoute<RecordEditRoute>().linkFromId,
            note = null
        )
    )
    val pageState: State<PageState> = _pageState

    init {
        val route = savedStateHandle.toRoute<RecordEditRoute>()
        if (_pageState.value.pageMode != RecordEditPageMode.EDIT) {
            prefillName(route.prefillName, TITLE_LENGTH_NOTE)?.let { title ->
                _editState.value = _editState.value.copy(
                    editTitle = title,
                    isValid = validateInput(title, _editState.value.editTextContent ?: ""),
                )
                _pageState.value = _pageState.value.copy(namePrefilled = true)
            }
        }
        val linkFromType = _pageState.value.linkFromType
        val linkFromId = _pageState.value.linkFromId
        if (_pageState.value.pageMode == RecordEditPageMode.NEWLINK && linkFromType != null && linkFromId != null) {
            viewModelScope.launch {
                _pageState.value = _pageState.value.copy(linkSource = getLinkSource(linkFromType, linkFromId))
            }
        }
        if (!noteId.isNullOrEmpty()) {
            viewModelScope.launch {
                noteRepository.getNote(noteId).let {
                    _pageState.value = _pageState.value.copy(
                        isNew = false,
                        pageMode = checkNotNull(savedStateHandle.toRoute<RecordEditRoute>().pageMode),
                        noteId = it.id,
                        linkFromType = savedStateHandle.toRoute<RecordEditRoute>().linkFromType,
                        linkFromId = savedStateHandle.toRoute<RecordEditRoute>().linkFromId,
                        note = it
                    )

                    shownAtStart = NoteTimes.shown(it.createDateTime, zone()).truncatedTo(ChronoUnit.MINUTES)
                    _editState.value = _editState.value.copy(
                        isValid = true,
                        editTitle = it.title?:"",
                        editTextContent = normalizeLineBreaks(it.textContents?:""),
                        editDay = shownAtStart.toLocalDate(),
                        editTime = shownAtStart.toLocalTime(),
                        isDayChanged = false,
                        isTimeChanged = false,
                    )
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
            val edit = _editState.value
            // Nothing changed: the moment of saving. Otherwise the day and time on the buttons.
            val times = if (edit.isDayChanged || edit.isTimeChanged) {
                NoteTimes.toStored(edit.editDay, edit.editTime, zone())
            } else {
                NoteTimes.forNow(Instant.ofEpochMilli(now()), zone())
            }
            val addedNoteRowId = noteRepository.addNote(
                Note(
                    ownerDbId = "db1",
                    title = edit.editTitle,
                    textContents = lineBreaksToSave(edit.editTextContent, null),
                    createDate = times.createDate,
                    createDateTime = times.createDateTime,
                )
            )
            val addedNoteId = noteRepository.getNoteByRowId(addedNoteRowId).id
            if (
                // Also link to source record
                _pageState.value.pageMode == RecordEditPageMode.NEWLINK &&
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
                // Records picked on Link Records before creating this one are linked to the source too
                linkedRecordRepository.linkRecordsTo(
                    _pageState.value.linkFromId!!,
                    _pageState.value.linkFromType!!,
                    alsoLink,
                )
            }
            return addedNoteId
        } else if (
            // Update Existing Note
            _editState.value.isValid &&
            !_pageState.value.isNew
        ) {
            val note = _pageState.value.note!!
            val edit = _editState.value
            // Both stored columns stay exactly as they are unless the day or time was changed
            val times = NoteTimes.edited(
                StoredNoteTimes(createDateTime = note.createDateTime, createDate = note.createDate),
                pickedDay = edit.editDay.takeIf { edit.isDayChanged },
                pickedTime = edit.editTime.takeIf { edit.isTimeChanged },
                zone = zone(),
            )
            noteRepository.updateNote(note.copy(
                title = edit.editTitle,
                textContents = lineBreaksToSave(edit.editTextContent, note.textContents),
                createDate = times.createDate,
                createDateTime = times.createDateTime,
            ))
            return null
        } else {
            return null
        }
    }
    /** The day picked in the date picker. Changed only if it differs from the day at the start. */
    fun updateDate(day: LocalDate) {
        _editState.value = _editState.value.copy(
            editDay = day,
            isDayChanged = day != shownAtStart.toLocalDate(),
        )
    }

    /** The time picked in the time picker. Changed only if it differs from the time at the start. */
    fun updateTime(time: LocalTime) {
        val picked = time.truncatedTo(ChronoUnit.MINUTES)
        _editState.value = _editState.value.copy(
            editTime = picked,
            isTimeChanged = picked != shownAtStart.toLocalTime(),
        )
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
        val pageMode: RecordEditPageMode? = null,
        val noteId: String? = null,
        val linkFromType: RecordType? = null,
        val linkFromId: String? = null,
        val linkSource: LinkSource? = null,
        /** The name came from a search (#10): the page starts in its field. */
        val namePrefilled: Boolean = false,
        val note: Note? = Note(),
    )

    data class EditState(
        val isValid: Boolean = false,
        val editTitle: String? = null,
        val editTextContent: String? = null,
        /** The local day and time (to the minute) on the date and time buttons. */
        val editDay: LocalDate = LocalDate.of(2000, 1, 1),
        val editTime: LocalTime = LocalTime.MIDNIGHT,
        val isDayChanged: Boolean = false,
        val isTimeChanged: Boolean = false,
    )
}