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
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.enums.NoteEditPageMode
import org.gnoseis.data.enums.RecordType
import org.gnoseis.testutil.MainDispatcherRule
import org.gnoseis.testutil.TestAppContainer
import org.gnoseis.testutil.knownBug
import org.gnoseis.testutil.link
import org.gnoseis.testutil.waitFor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class NoteViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val c = TestAppContainer()

    @After
    fun tearDown() {
        mainDispatcherRule.clearViewModels() // before the database closes (#25)
        c.close()
    }

    private fun editVm(noteId: String?, mode: NoteEditPageMode, linkFromType: RecordType? = null, linkFromId: String? = null) =
        mainDispatcherRule.viewModel {
            NoteEditViewModel(
                SavedStateHandle(mapOf("noteId" to noteId, "pageMode" to mode, "linkFromType" to linkFromType, "linkFromId" to linkFromId)),
                c.noteRepository,
                c.linkedRecordRepository,
            )
        }

    private fun todayUtcMidnightMillis() =
        LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT).toEpochSecond(ZoneOffset.UTC) * 1000

    @Test
    fun list_reflectsRepository_newestFirst() = runTest {
        val vm = mainDispatcherRule.viewModel { NoteListViewModel(c.noteRepository) }
        vm.notePageState.test {
            assertEquals(emptyList<Note>(), awaitItem().notes)
            val old = Note(title = "old", createDateTime = 1)
            val new = Note(title = "new", createDateTime = 2)
            c.noteRepository.addNote(old)
            c.noteRepository.addNote(new)
            var notes = awaitItem().notes
            while (notes.size < 2) notes = awaitItem().notes
            assertEquals(listOf(new, old), notes)
        }
    }

    @Test
    fun details_loadsLinkedRecords_andDeleteCleansUp() = runTest {
        val note = Note(title = "n")
        val contact = Contact(nameLast = "Doe")
        val org = Organization(organizationName = "Acme")
        c.noteRepository.addNote(note)
        c.contactRepository.addContact(contact)
        c.organizationRepository.addOrganization(org)
        c.linkedRecordRepository.addLinkedRecord(link(note.id, RecordType.Note, contact.id, RecordType.Contact))
        c.linkedRecordRepository.addLinkedRecord(link(org.id, RecordType.Organization, note.id, RecordType.Note))
        val vm = mainDispatcherRule.viewModel { NoteDetailsViewModel(SavedStateHandle(mapOf("noteId" to note.id)), c.noteRepository, c.linkedRecordRepository) }

        vm.linkedRecords.test {
            var linked = awaitItem()
            while (linked.linkedContacts.isEmpty() || linked.linkedOrganizations.isEmpty()) linked = awaitItem()
            assertEquals(listOf(contact), linked.linkedContacts)
            assertEquals(listOf(org), linked.linkedOrganizations)
        }
        vm.noteDetailsPageState.test {
            var state = awaitItem()
            while (state.note != note) state = awaitItem()
            vm.deleteNote()
            while (state.note != null) state = awaitItem()
        }
        assertTrue(c.linkedRecordRepository.getRecordIdsLinkedToRecordId(contact.id).first().isEmpty())
    }

    @Test
    fun edit_newMode_initialState_defaultsDateToTodayUtcMidnight() {
        val vm = editVm(null, NoteEditPageMode.NEW)
        assertTrue(vm.pageState.value.isNew)
        assertFalse(vm.editState.value.isValid)
        assertEquals(todayUtcMidnightMillis(), vm.editState.value.editDate)
    }

    @Test
    fun edit_validIfTitleOrContentPresent_afterDebounce() = runTest {
        val vm = editVm(null, NoteEditPageMode.NEW)

        vm.updateTitle("t")
        assertFalse(vm.editState.value.isValid)
        advanceTimeBy(501); runCurrent()
        assertTrue(vm.editState.value.isValid)

        vm.updateTitle("")
        advanceTimeBy(501); runCurrent()
        assertFalse(vm.editState.value.isValid)

        vm.updateTextContent("body only")
        advanceTimeBy(501); runCurrent()
        assertTrue(vm.editState.value.isValid)
    }

    @Test
    fun edit_save_whenInvalid_doesNothing() = runTest {
        assertNull(editVm(null, NoteEditPageMode.NEW).onSave())
        assertTrue(c.noteRepository.getNotesStream().first().isEmpty())
    }

    @Test
    fun edit_newMode_saveUsesSelectedDate() = runTest {
        val vm = editVm(null, NoteEditPageMode.NEW)
        vm.updateTitle("t")
        vm.updateTextContent("body")
        vm.updateDate(86_400_000L)
        advanceTimeBy(501); runCurrent()

        val saved = c.noteRepository.getNote(vm.onSave()!!)

        assertEquals("t", saved.title)
        assertEquals("body", saved.textContents)
        assertEquals(86_400_000L, saved.createDate)
        assertEquals(86_400_000L, saved.createDateTime)
    }

    @Test
    fun edit_newLinkMode_savesAndLinks() = runTest {
        val contact = Contact(nameLast = "Doe")
        c.contactRepository.addContact(contact)
        val vm = editVm(null, NoteEditPageMode.NEWLINK, RecordType.Contact, contact.id)
        vm.updateTitle("call notes")
        advanceTimeBy(501); runCurrent()

        val id = vm.onSave()!!

        assertEquals(listOf(id), c.linkedRecordRepository.getRecordIdsLinkedToRecordId(contact.id).first())
    }

    @Test
    fun edit_editMode_loadsAndUpdates() = runTest {
        val note = Note(title = "Old", textContents = "body", createDateTime = 5_000, createDate = 0)
        c.noteRepository.addNote(note)
        val vm = editVm(note.id, NoteEditPageMode.EDIT)

        waitFor { !vm.pageState.value.isNew }
        assertEquals("Old", vm.editState.value.editTitle)
        assertEquals("body", vm.editState.value.editTextContent)
        assertEquals(5_000L, vm.editState.value.editDate)

        vm.updateTitle("New")
        advanceTimeBy(501); runCurrent()
        assertNull(vm.onSave())

        assertEquals("New", c.noteRepository.getNote(note.id).title)
    }

    /**
     * createDate is the creation day (truncated), createDateTime the exact instant.
     * Editing only the title should not change either of them.
     */
    @Test
    fun edit_editMode_savingWithoutChangingDate_keepsCreateDate() = runTest {
        knownBug(
            issue = 21,
            "editing a note overwrites its creation day. NoteEditViewModel.onSave() sets createDate = editDate, which holds the full createDateTime timestamp. Fix: only change the dates when the user picks a new date.",
        ) {
            val createDateTime = 1_700_000_123_456L
            val createDate = 1_699_920_000_000L
            val note = Note(title = "Old", createDateTime = createDateTime, createDate = createDate)
            c.noteRepository.addNote(note)
            val vm = editVm(note.id, NoteEditPageMode.EDIT)
            waitFor { !vm.pageState.value.isNew }

            vm.updateTitle("New")
            advanceTimeBy(501); runCurrent()
            vm.onSave()

            val saved = c.noteRepository.getNote(note.id)
            assertEquals(createDateTime, saved.createDateTime)
            assertEquals(createDate, saved.createDate)
        }
    }
}
