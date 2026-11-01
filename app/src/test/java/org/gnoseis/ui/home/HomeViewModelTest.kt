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

package org.gnoseis.ui.home

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.AppDataContainer
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.home.DayCount
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.entity.settings.SettingKey
import org.gnoseis.data.enums.RecordType
import org.gnoseis.domain.usecase.DeleteRecordUseCase
import org.gnoseis.testutil.MainDispatcherRule
import org.gnoseis.testutil.inMemoryDatabase
import org.gnoseis.testutil.link
import org.gnoseis.testutil.waitFor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var database: GnoseisDatabase = inMemoryDatabase()
    private var clock = 100L
    private var c = container()
    private val today = LocalDate.of(2026, 10, 25)

    private fun container() = AppDataContainer(context, { database }, { clock })

    @After
    fun tearDown() {
        mainDispatcherRule.clearViewModels() // before the database closes (#25)
        database.close()
        GnoseisDatabase.resetInstanceForTesting()
        context.deleteDatabase(DB_NAME)
    }

    private fun vm() = mainDispatcherRule.viewModel { HomeViewModel(c.homeRepository, c.settingsRepository) { today } }

    /** Waits until the page state matches [condition] and returns it. */
    private suspend fun HomeViewModel.awaitState(condition: (HomePageState) -> Boolean): HomePageState {
        var result: HomePageState? = null
        homePageState.test {
            var state = awaitItem()
            while (!(state.isLoaded && condition(state))) state = awaitItem()
            result = state
            cancelAndIgnoreRemainingEvents()
        }
        return result!!
    }

    private suspend fun setLastBackupAt(time: Long) = database.settingsDao().upsert(SettingKey.LAST_BACKUP_AT.row(time))

    private fun deleteRecord() = DeleteRecordUseCase(
        noteRepository = c.noteRepository,
        contactRepository = c.contactRepository,
        linkedRecordRepository = c.linkedRecordRepository,
        settingsRepository = c.settingsRepository,
    )

    // ---------- Backup card ----------

    @Test
    fun neverBackedUp_showsTheWarning() = runTest {
        c.noteRepository.addNote(Note(title = "n"))

        vm().awaitState { it.backup == BackupState.NeverBackedUp }
    }

    @Test
    fun backedUp_withNoChangesSince_hidesTheCard() = runTest {
        c.noteRepository.addNote(Note(title = "n"))
        setLastBackupAt(200)

        vm().awaitState { it.backup == BackupState.Hidden }
    }

    @Test
    fun recordsAndLinksAddedOrEditedAfterTheBackup_areCounted_andTheCardUpdatesLive() = runTest {
        val note = Note(title = "n")
        c.noteRepository.addNote(note)
        setLastBackupAt(200)
        val vm = vm()
        vm.awaitState { it.backup == BackupState.Hidden }

        clock = 300
        val contact = Contact(nameLast = "Doe")
        c.contactRepository.addContact(contact)
        c.linkedRecordRepository.addLinkedRecord(link(contact.id, RecordType.Contact, note.id, RecordType.Note))
        c.noteRepository.updateNote(c.noteRepository.getNote(note.id).copy(title = "edited"))

        vm.awaitState { it.backup == BackupState.ChangedSince(200, changeCount = 3, deletedSince = false) }
    }

    @Test
    fun deletionAfterTheBackup_bringsTheCardBack() = runTest {
        val note = Note(title = "n")
        c.noteRepository.addNote(note)
        setLastBackupAt(200)
        val vm = vm()
        vm.awaitState { it.backup == BackupState.Hidden }

        clock = 300
        deleteRecord()(note.id)

        vm.awaitState { it.backup == BackupState.ChangedSince(200, changeCount = 0, deletedSince = true) }
    }

    @Test
    fun deletionBeforeTheBackup_doesNotCount() = runTest {
        val note = Note(title = "n")
        c.noteRepository.addNote(note)
        deleteRecord()(note.id) // at 100
        setLastBackupAt(200)

        vm().awaitState { it.backup == BackupState.Hidden }
    }

    @Test
    fun rowsWithNullTimestamps_neverCountAsChanges() = runTest {
        database.noteDao().addNote(Note(title = "from before v3")) // no createdAt / updatedAt
        setLastBackupAt(200)

        vm().awaitState { it.backup == BackupState.Hidden }
    }

    /** The real export and import, against the file database they work on. */
    @Test
    fun cardHides_afterAnExport_andAfterImportingABackupWithNoLaterChanges() = runTest {
        database.close()
        context.deleteDatabase(DB_NAME)
        GnoseisDatabase.resetInstanceForTesting()
        database = GnoseisDatabase.getDatabase(context)
        c = container()
        // Let the seed note (#20) arrive, then remove it so it doesn't count as a change
        waitFor { runBlocking { database.noteDao().getNotes().first().isNotEmpty() } }
        database.openHelper.writableDatabase.execSQL("DELETE FROM Note")
        val backup = File(context.cacheDir, "home_test.backup").apply { delete() }

        c.noteRepository.addNote(Note(title = "A")) // at 100
        val vm = vm()
        vm.awaitState { it.backup == BackupState.NeverBackedUp }

        clock = 200
        c.databaseRepository.exportDatabase(Uri.fromFile(backup))
        vm.awaitState { it.backup == BackupState.Hidden }

        clock = 300
        c.noteRepository.addNote(Note(title = "B"))
        vm.awaitState { it.backup == BackupState.ChangedSince(200, changeCount = 1, deletedSince = false) }

        c.databaseRepository.importDatabase(Uri.fromFile(backup))
        vm.awaitState { it.backup == BackupState.Hidden }
        backup.delete()
    }

    // ---------- Dashboard ----------

    @Test
    fun dashboard_showsCountsMostLinkedRecentAndUnlinkedNotes() = runTest {
        val contact = Contact(nameLast = "Doe")
        val org = Organization(organizationName = "Acme")
        val linked = Note(title = "linked", createDateTime = 3)
        val unlinked = Note(title = "unlinked", createDateTime = 2)
        c.contactRepository.addContact(contact)
        c.organizationRepository.addOrganization(org)
        c.noteRepository.addNote(linked)
        c.noteRepository.addNote(unlinked)
        c.linkedRecordRepository.addLinkedRecord(link(contact.id, RecordType.Contact, org.id, RecordType.Organization))
        c.linkedRecordRepository.addLinkedRecord(link(linked.id, RecordType.Note, contact.id, RecordType.Contact))

        val state = vm().awaitState { it.counts.links == 2 }

        assertEquals(2, state.counts.notes)
        assertEquals(listOf(contact.id, org.id), state.mostLinked.map { it.id })
        assertEquals(listOf(2, 1), state.mostLinked.map { it.linkCount })
        assertEquals(1, state.mostLinkedLinks.size)
        assertEquals(listOf(linked.id, unlinked.id), state.recentNotes.map { it.id })
        assertEquals(listOf(unlinked.id), state.unlinkedNotes.map { it.id })
        assertEquals(1, state.unlinkedNoteCount)
    }

    @Test
    fun notesPerDay_hasFourteenDaysEndingToday_withZeroForEmptyDays() = runTest {
        fun day(date: LocalDate) = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        c.noteRepository.addNote(Note(title = "today", createDate = day(today)))
        c.noteRepository.addNote(Note(title = "today too", createDate = day(today)))
        c.noteRepository.addNote(Note(title = "first day", createDate = day(today.minusDays(13))))
        c.noteRepository.addNote(Note(title = "too old", createDate = day(today.minusDays(14))))

        val perDay = vm().awaitState { it.notesPerDay.sumOf { day -> day.count } == 3 }.notesPerDay

        assertEquals(14, perDay.size)
        assertEquals(DayCount(today.minusDays(13).toEpochDay(), 1), perDay.first())
        assertEquals(DayCount(today.toEpochDay(), 2), perDay.last())
        assertEquals(List(12) { 0 }, perDay.subList(1, 13).map { it.count })
    }

    @Test
    fun mostLinkedFilter_showsTheTopRecordsOfThatType_andAllAgain() = runTest {
        val contact = Contact(nameLast = "Doe")
        val org = Organization(organizationName = "Acme")
        val note = Note(title = "n")
        c.contactRepository.addContact(contact)
        c.organizationRepository.addOrganization(org)
        c.noteRepository.addNote(note)
        c.linkedRecordRepository.addLinkedRecord(link(contact.id, RecordType.Contact, org.id, RecordType.Organization))
        c.linkedRecordRepository.addLinkedRecord(link(note.id, RecordType.Note, contact.id, RecordType.Contact))
        val vm = vm()
        vm.awaitState { it.mostLinked.size == 2 }

        vm.setMostLinkedTypeId(RecordType.Organization.recordTypeId)
        val orgs = vm.awaitState { it.mostLinkedTypeId == RecordType.Organization.recordTypeId }
        assertEquals(listOf(org.id), orgs.mostLinked.map { it.id })
        assertEquals(emptyList<Any>(), orgs.mostLinkedLinks)

        vm.setMostLinkedTypeId(RecordType.Item.recordTypeId)
        assertEquals(emptyList<Any>(), vm.awaitState { it.mostLinkedTypeId == RecordType.Item.recordTypeId }.mostLinked)

        vm.setMostLinkedTypeId(null)
        assertEquals(listOf(contact.id, org.id), vm.awaitState { it.mostLinkedTypeId == null && it.mostLinked.size == 2 }.mostLinked.map { it.id })
    }

    @Test
    fun noLinks_noMostLinked() = runTest {
        c.contactRepository.addContact(Contact(nameLast = "Doe"))

        val state = vm().awaitState { it.counts.contacts == 1 }

        assertEquals(emptyList<Any>(), state.mostLinked)
        assertEquals(emptyList<Any>(), state.mostLinkedLinks)
        assertNull(c.settingsRepository.getLastBackupAtStream().first())
    }

    // ---------- Backup card text ----------

    @Test
    fun backupCardText_matchesEachState() {
        val zone = ZoneOffset.UTC
        val backedUp = today.minusDays(12).atStartOfDay(zone).toInstant().toEpochMilli()

        assertNull(backupCardText(BackupState.Hidden, today, zone))
        assertEquals("Not backed up", backupCardText(BackupState.NeverBackedUp, today, zone)!!.first)
        assertEquals(
            "Last backup 12 days ago" to "2026-10-13 · 18 records changed since.",
            backupCardText(BackupState.ChangedSince(backedUp, 18, deletedSince = false), today, zone),
        )
        assertEquals(
            "2026-10-13 · 1 record changed since, and some records were deleted.",
            backupCardText(BackupState.ChangedSince(backedUp, 1, deletedSince = true), today, zone)!!.second,
        )
        assertEquals(
            "2026-10-13 · Some records were deleted since.",
            backupCardText(BackupState.ChangedSince(backedUp, 0, deletedSince = true), today, zone)!!.second,
        )
        assertEquals(
            "Last backup today",
            backupCardText(BackupState.ChangedSince(today.atStartOfDay(zone).toInstant().toEpochMilli(), 1, false), today, zone)!!.first,
        )
    }

    private companion object {
        const val DB_NAME = "gnoseis_data"
    }
}
