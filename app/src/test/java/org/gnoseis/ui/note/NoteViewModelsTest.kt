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
*   https://github.com/boredace/Gnoseis-Android/blob/main/LICENSE.
*
*   Dual Licensing Notice:
*
*   Gnoseis can also be licensed under commercial terms. See
*   https://github.com/boredace/Gnoseis-Android/blob/main/COMMERCIAL_LICENSE
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
import org.gnoseis.data.enums.RecordEditPageMode
import org.gnoseis.data.enums.RecordType
import org.gnoseis.domain.usecase.LinkSource
import org.gnoseis.testutil.MainDispatcherRule
import org.gnoseis.testutil.TestAppContainer
import org.gnoseis.testutil.link
import org.gnoseis.testutil.waitFor
import org.gnoseis.testutil.stamped
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
import java.time.ZoneId
import org.gnoseis.data.time.NoteTimes
import org.gnoseis.data.time.StoredNoteTimes
import org.gnoseis.testutil.TEST_NOW

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

    /** The editor's clock is [TEST_NOW] (2023-11-14 22:13:20 UTC) and its zone [zone] (#68). */
    private fun editVm(
        noteId: String?,
        mode: RecordEditPageMode,
        linkFromType: RecordType? = null,
        linkFromId: String? = null,
        zone: ZoneId = TOKYO,
    ) =
        mainDispatcherRule.viewModel {
            NoteEditViewModel(
                SavedStateHandle(mapOf("recordType" to RecordType.Note, "recordId" to noteId, "pageMode" to mode, "linkFromType" to linkFromType, "linkFromId" to linkFromId)),
                c.noteRepository,
                c.linkedRecordRepository,
                c.getLinkSourceUseCase(),
                now = { TEST_NOW },
                zone = { zone },
            )
        }

    private companion object {
        val TOKYO: ZoneId = ZoneId.of("Asia/Tokyo")
        val TORONTO: ZoneId = ZoneId.of("America/Toronto")
    }

    @Test
    fun list_reflectsRepository_newestFirst() = runTest {
        val vm = mainDispatcherRule.viewModel { NoteListViewModel(c.noteRepository) }
        vm.notePageState.test {
            assertEquals(emptyList<Note>(), awaitItem().notes)
            val old = Note(title = "old", createDateTime = 1).stamped()
            val new = Note(title = "new", createDateTime = 2).stamped()
            c.noteRepository.addNote(old)
            c.noteRepository.addNote(new)
            var notes = awaitItem().notes
            while (notes.size < 2) notes = awaitItem().notes
            assertEquals(listOf(new, old), notes)
        }
    }

    @Test
    fun details_loadsLinkedRecords_andDeleteCleansUp() = runTest {
        val note = Note(title = "n").stamped()
        val contact = Contact(nameLast = "Doe").stamped()
        val org = Organization(organizationName = "Acme").stamped()
        c.noteRepository.addNote(note)
        c.contactRepository.addContact(contact)
        c.organizationRepository.addOrganization(org)
        c.linkedRecordRepository.addLinkedRecord(link(note.id, RecordType.Note, contact.id, RecordType.Contact))
        c.linkedRecordRepository.addLinkedRecord(link(org.id, RecordType.Organization, note.id, RecordType.Note))
        val vm = mainDispatcherRule.viewModel { NoteDetailsViewModel(SavedStateHandle(mapOf("recordType" to RecordType.Note, "recordId" to note.id)), c.noteRepository, c.linkedRecordRepository, c.settingsRepository) }

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
            // The delete is one transaction (#51), so the tabs may update after the note is gone
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(c.linkedRecordRepository.getRecordIdsLinkedToRecordId(contact.id).first().isEmpty())
    }

    /** #68: a new note starts at the current local day and time, to the minute. */
    @Test
    fun edit_newMode_initialState_isTheCurrentLocalDayAndTime() {
        val vm = editVm(null, RecordEditPageMode.NEW)
        assertTrue(vm.pageState.value.isNew)
        assertFalse(vm.editState.value.isValid)
        // TEST_NOW is 2023-11-15 07:13:20 in Tokyo
        assertEquals(LocalDate.parse("2023-11-15"), vm.editState.value.editDay)
        assertEquals(LocalTime.parse("07:13"), vm.editState.value.editTime)
        assertFalse(vm.editState.value.isDayChanged || vm.editState.value.isTimeChanged)
    }

    @Test
    fun edit_validIfTitleOrContentPresent_afterDebounce() = runTest {
        val vm = editVm(null, RecordEditPageMode.NEW)

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
        assertNull(editVm(null, RecordEditPageMode.NEW).onSave())
        assertTrue(c.noteRepository.getNotesStream().first().isEmpty())
    }

    private suspend fun saveNew(zone: ZoneId = TOKYO, pick: NoteEditViewModel.() -> Unit = {}): Note {
        val vm = editVm(null, RecordEditPageMode.NEW, zone = zone)
        vm.updateTitle("t")
        vm.updateTextContent("body")
        mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(501)
        mainDispatcherRule.testDispatcher.scheduler.runCurrent()
        vm.pick()
        return c.noteRepository.getNote(vm.onSave()!!)
    }

    /** #68: nothing picked, so the note is the clock's moment, not a rounded time. */
    @Test
    fun edit_newMode_unchangedDayAndTime_savesTheMomentOfSaving() = runTest {
        val saved = saveNew()

        assertEquals("t", saved.title)
        assertEquals("body", saved.textContents)
        assertEquals(NoteTimes.forNow(java.time.Instant.ofEpochMilli(TEST_NOW), TOKYO), StoredNoteTimes(saved.createDateTime, saved.createDate))
        assertEquals(TEST_NOW, saved.createDateTime)
    }

    /** #68: OK on the time already shown isn't a change. */
    @Test
    fun edit_newMode_sameTimePicked_savesTheMomentOfSaving() = runTest {
        val saved = saveNew { updateTime(LocalTime.parse("07:13")); updateDate(LocalDate.parse("2023-11-15")) }

        assertEquals(TEST_NOW, saved.createDateTime)
    }

    @Test
    fun edit_newMode_pickedTime_savesThatLocalTimeOnTheDayShown() = runTest {
        val saved = saveNew { updateTime(LocalTime.parse("21:40")) }

        assertEquals(NoteTimes.toStored(LocalDate.parse("2023-11-15"), LocalTime.parse("21:40"), TOKYO), StoredNoteTimes(saved.createDateTime, saved.createDate))
    }

    @Test
    fun edit_newMode_pickedDay_savesTheShownTimeOnThatDay_toTheMinute() = runTest {
        val saved = saveNew(TORONTO) { updateDate(LocalDate.parse("2023-11-01")) }

        // TEST_NOW is 17:13:20 in Toronto; seconds aren't kept for a picked day
        assertEquals(NoteTimes.toStored(LocalDate.parse("2023-11-01"), LocalTime.parse("17:13"), TORONTO), StoredNoteTimes(saved.createDateTime, saved.createDate))
    }

    /** #68: the shared example 2026-07-15 14:30 in America/Toronto. */
    @Test
    fun edit_newMode_pickedDayAndTime_followsTheExample() = runTest {
        val saved = saveNew(TORONTO) { updateDate(LocalDate.parse("2026-07-15")); updateTime(LocalTime.parse("14:30")) }

        assertEquals(StoredNoteTimes(1784140200000, 1784088000000), StoredNoteTimes(saved.createDateTime, saved.createDate))
    }

    @Test
    fun edit_newLinkMode_savesAndLinks() = runTest {
        val contact = Contact(nameLast = "Doe").stamped()
        c.contactRepository.addContact(contact)
        val vm = editVm(null, RecordEditPageMode.NEWLINK, RecordType.Contact, contact.id)
        vm.updateTitle("call notes")
        advanceTimeBy(501); runCurrent()

        val id = vm.onSave()!!

        assertEquals(listOf(id), c.linkedRecordRepository.getRecordIdsLinkedToRecordId(contact.id).first())
    }

    @Test
    fun edit_editMode_loadsAndUpdates() = runTest {
        // The Tokyo example: 2026-11-14 08:15 in Tokyo
        val note = Note(title = "Old", textContents = "body", createDateTime = 1794611700000, createDate = 1794582000000).stamped()
        c.noteRepository.addNote(note)
        val vm = editVm(note.id, RecordEditPageMode.EDIT)

        waitFor { !vm.pageState.value.isNew }
        assertEquals("Old", vm.editState.value.editTitle)
        assertEquals("body", vm.editState.value.editTextContent)
        assertEquals(LocalDate.parse("2026-11-14"), vm.editState.value.editDay)
        assertEquals(LocalTime.parse("08:15"), vm.editState.value.editTime)

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
        val createDateTime = 1_700_000_123_456L
        val createDate = 1_699_920_000_000L
        val note = Note(title = "Old", createDateTime = createDateTime, createDate = createDate).stamped()
        c.noteRepository.addNote(note)
        val vm = editVm(note.id, RecordEditPageMode.EDIT)
        waitFor { !vm.pageState.value.isNew }

        vm.updateTitle("New")
        advanceTimeBy(501); runCurrent()
        vm.onSave()

        val saved = c.noteRepository.getNote(note.id)
        assertEquals(createDateTime, saved.createDateTime)
        assertEquals(createDate, saved.createDate)
    }

    /** Opens [note] in the editor in [zone], applies [edit] and saves; returns the saved columns. */
    private suspend fun saveEdited(note: Note, zone: ZoneId, edit: NoteEditViewModel.() -> Unit): StoredNoteTimes {
        c.noteRepository.addNote(note)
        val vm = editVm(note.id, RecordEditPageMode.EDIT, zone = zone)
        waitFor { !vm.pageState.value.isNew }
        vm.edit()
        vm.onSave()
        return c.noteRepository.getNote(note.id).let { StoredNoteTimes(it.createDateTime, it.createDate) }
    }

    /** #68: a note saved in Tokyo, opened and saved in Toronto, keeps both columns exactly. */
    @Test
    fun edit_editMode_tokyoNote_savedInTorontoUnchanged_keepsBothColumns() = runTest {
        val note = Note(title = "Old", createDateTime = 1794611700000, createDate = 1794582000000).stamped()

        val saved = saveEdited(note, TORONTO) { updateTitle("New") }

        assertEquals(StoredNoteTimes(1794611700000, 1794582000000), saved)
    }

    /** #68: OK on the day and time already shown isn't a change, even with seconds stored. */
    @Test
    fun edit_editMode_sameDayAndTimePicked_keepsBothColumns() = runTest {
        val note = Note(title = "Old", createDateTime = 1794611712345, createDate = 1794582000000).stamped()

        val saved = saveEdited(note, TOKYO) { updateDate(LocalDate.parse("2026-11-14")); updateTime(LocalTime.parse("08:15")) }

        assertEquals(StoredNoteTimes(1794611712345, 1794582000000), saved)
    }

    @Test
    fun edit_editMode_pickedDay_keepsTheShownTime() = runTest {
        // 2026-11-13 18:15 in Toronto
        val note = Note(title = "Old", createDateTime = 1794611700000, createDate = 1794582000000).stamped()

        val saved = saveEdited(note, TORONTO) { updateDate(LocalDate.parse("2026-07-15")) }

        assertEquals(NoteTimes.toStored(LocalDate.parse("2026-07-15"), LocalTime.parse("18:15"), TORONTO), saved)
    }

    @Test
    fun edit_editMode_pickedTime_keepsTheShownDay() = runTest {
        // 2026-11-13 18:15 in Toronto
        val note = Note(title = "Old", createDateTime = 1794611700000, createDate = 1794582000000).stamped()

        val saved = saveEdited(note, TORONTO) { updateTime(LocalTime.parse("07:05")) }

        assertEquals(NoteTimes.toStored(LocalDate.parse("2026-11-13"), LocalTime.parse("07:05"), TORONTO), saved)
    }

    /** #68: a time in the spring-forward gap moves later by the gap (the shared example). */
    @Test
    fun edit_editMode_pickedTimeInAGap_followsTheRule() = runTest {
        val note = Note(
            title = "Old",
            createDateTime = NoteTimes.toStored(LocalDate.parse("2026-03-08"), LocalTime.parse("10:00"), TORONTO).createDateTime,
            createDate = 1772946000000,
        ).stamped()

        val saved = saveEdited(note, TORONTO) { updateTime(LocalTime.parse("02:30")) }

        assertEquals(StoredNoteTimes(1772955000000, 1772946000000), saved)
    }

    // ---------- Edit: line breaks (#67) ----------

    @Test
    fun edit_textWithCarriageReturns_loadsWithLineFeeds() = runTest {
        val stored = Note(title = "Old", textContents = "one\rtwo\r\nthree")
        c.database.noteDao().addNote(stored)
        val vm = editVm(stored.id, RecordEditPageMode.EDIT)

        waitFor { !vm.pageState.value.isNew }
        assertEquals("one\ntwo\nthree", vm.editState.value.editTextContent)
    }

    @Test
    fun edit_savingUneditedText_keepsStoredLineBreaksAndUpdatedAt() = runTest {
        val stored = Note(title = "Old", textContents = "one\rtwo\r\nthree").copy(createdAt = 1, updatedAt = 1)
        c.database.noteDao().addNote(stored)
        val vm = editVm(stored.id, RecordEditPageMode.EDIT)
        waitFor { !vm.pageState.value.isNew }

        assertNull(vm.onSave())

        assertEquals(stored, c.noteRepository.getNote(stored.id))
    }

    @Test
    fun edit_savingEditedText_writesLineFeedsOnly() = runTest {
        val stored = Note(title = "Old", textContents = "one\rtwo\r\nthree").copy(createdAt = 1, updatedAt = 1)
        c.database.noteDao().addNote(stored)
        val vm = editVm(stored.id, RecordEditPageMode.EDIT)
        waitFor { !vm.pageState.value.isNew }

        vm.updateTextContent(vm.editState.value.editTextContent + "\r\nfour")
        advanceTimeBy(501); runCurrent()
        assertNull(vm.onSave())

        val saved = c.noteRepository.getNote(stored.id)
        assertEquals("one\ntwo\nthree\nfour", saved.textContents)
        assertEquals(TEST_NOW, saved.updatedAt)
    }

    @Test
    fun edit_newRecord_textWithCarriageReturns_isStoredWithLineFeeds() = runTest {
        val vm = editVm(null, RecordEditPageMode.NEW)
        vm.updateTitle("Name")
        vm.updateTextContent("one\r\ntwo\rthree")
        advanceTimeBy(501); runCurrent()

        val saved = c.noteRepository.getNote(vm.onSave()!!)

        assertEquals("one\ntwo\nthree", saved.textContents)
    }

    // ---------- Edit: link source (#7) ----------

    @Test
    fun edit_newLinkMode_pageStateHasLinkSource() = runTest {
        val source = Contact(nameFirst = "Anna", nameLast = "Kowalski")
        c.contactRepository.addContact(source)
        val vm = editVm(null, RecordEditPageMode.NEWLINK, RecordType.Contact, source.id)

        waitFor { vm.pageState.value.linkSource != null }
        assertEquals(LinkSource(RecordType.Contact, "Anna Kowalski"), vm.pageState.value.linkSource)
    }

    @Test
    fun edit_newAndEditMode_haveNoLinkSource() = runTest {
        val source = Contact(nameFirst = "Anna", nameLast = "Kowalski")
        val record = Note(title = "Old")
        c.contactRepository.addContact(source)
        c.noteRepository.addNote(record)

        val newVm = editVm(null, RecordEditPageMode.NEW, RecordType.Contact, source.id)
        val editVm = editVm(record.id, RecordEditPageMode.EDIT, RecordType.Contact, source.id)
        waitFor { editVm.pageState.value.note != null }

        assertNull(newVm.pageState.value.linkSource)
        assertNull(editVm.pageState.value.linkSource)
    }

    @Test
    fun edit_newLinkMode_missingSource_hasNoLinkSource_andSaveStillAdds() = runTest {
        val vm = editVm(null, RecordEditPageMode.NEWLINK, RecordType.Contact, "missing-id")
        vm.updateTitle("call notes")
        advanceTimeBy(501); runCurrent()

        val id = vm.onSave()!!

        assertNull(vm.pageState.value.linkSource)
        assertEquals("call notes", c.noteRepository.getNote(id).title)
    }
}
