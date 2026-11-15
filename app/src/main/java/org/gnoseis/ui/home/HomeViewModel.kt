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

package org.gnoseis.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.gnoseis.data.entity.home.DayCount
import org.gnoseis.data.entity.home.LinkEnds
import org.gnoseis.data.entity.home.LinkedRecordCount
import org.gnoseis.data.entity.home.NoteSummary
import org.gnoseis.data.entity.home.RecordCounts
import org.gnoseis.data.entity.home.TypeLinkCount
import org.gnoseis.data.repository.HomeRepository
import org.gnoseis.data.repository.SettingsRepository
import org.gnoseis.data.time.NoteTimes
import java.time.LocalDate
import java.time.ZoneId

/**
 * The Home page (#45). Every part is a Flow, so the page updates when the user comes back from
 * adding, editing, linking or deleting a record. [zone] is the device's time zone and [today]
 * the local date in it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    homeRepository: HomeRepository,
    settingsRepository: SettingsRepository,
    zone: () -> ZoneId = { ZoneId.systemDefault() },
    today: () -> LocalDate = { LocalDate.now(zone()) },
) : ViewModel() {
    private val todayDate = today()
    private val zoneId = zone()

    /**
     * The record type picked in the Most linked type picker, or null for all types. Kept here so
     * it survives going to a Details page and back.
     */
    private val mostLinkedTypeId = MutableStateFlow<Int?>(null)

    private val mostLinked: Flow<MostLinked> =
        combine(
            mostLinkedTypeId.flatMapLatest { typeId ->
                homeRepository.getMostLinkedStream(MOST_LINKED_COUNT, typeId).flatMapLatest { records ->
                    homeRepository.getLinksAmongStream(records.map { it.id }).map { MostLinked(typeId, records, it) }
                }
            },
            homeRepository.getLinksPerTypeStream(),
        ) { mostLinked, linksPerType -> mostLinked.copy(types = linksPerType) }

    fun setMostLinkedTypeId(typeId: Int?) {
        mostLinkedTypeId.value = typeId
    }

    /** Notes per local day (#68): from the start of the first day to the start of tomorrow. */
    private val notesPerDay: Flow<List<DayCount>> = run {
        val firstDay = todayDate.minusDays(CHART_DAYS - 1L)
        homeRepository.getNoteTimesStream(
            from = NoteTimes.startOfDay(firstDay, zoneId),
            until = NoteTimes.startOfDay(todayDate.plusDays(1), zoneId),
        ).map { times ->
            val byDay = times.groupingBy { NoteTimes.shownDay(it, zoneId).toEpochDay() }.eachCount()
            (firstDay.toEpochDay()..todayDate.toEpochDay()).map { DayCount(it, byDay[it] ?: 0) }
        }
    }

    private val unlinked: Flow<Pair<List<NoteSummary>, Int>> =
        combine(
            homeRepository.getUnlinkedNotesStream(UNLINKED_COUNT),
            homeRepository.getUnlinkedNoteCountStream(),
        ) { notes, count -> notes to count }

    private val backup: Flow<BackupState> =
        combine(
            settingsRepository.getLastBackupAtStream(),
            settingsRepository.getLastDeletionAtStream(),
        ) { lastBackupAt, lastDeletionAt -> lastBackupAt to lastDeletionAt }
            .flatMapLatest { (lastBackupAt, lastDeletionAt) ->
                if (lastBackupAt == null) {
                    flowOf(BackupState.NeverBackedUp)
                } else {
                    val deletedSince = lastDeletionAt != null && lastDeletionAt > lastBackupAt
                    homeRepository.getChangeCountSinceStream(lastBackupAt).map { changes ->
                        if (changes == 0 && !deletedSince) BackupState.Hidden
                        else BackupState.ChangedSince(lastBackupAt, changes, deletedSince)
                    }
                }
            }

    val homePageState: StateFlow<HomePageState> =
        combine(
            homeRepository.getRecordCountsStream(),
            mostLinked,
            homeRepository.getRecentNotesStream(RECENT_COUNT),
            notesPerDay,
            combine(unlinked, backup) { u, b -> u to b },
        ) { counts, mostLinked, recentNotes, notesPerDay, (unlinked, backup) ->
            HomePageState(
                today = todayDate,
                counts = counts,
                mostLinkedTypeId = mostLinked.typeId,
                mostLinkedTypes = mostLinked.types,
                mostLinked = mostLinked.records,
                mostLinkedLinks = mostLinked.links,
                recentNotes = recentNotes,
                notesPerDay = notesPerDay,
                unlinkedNotes = unlinked.first,
                unlinkedNoteCount = unlinked.second,
                backup = backup,
                isLoaded = true,
            )
        }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                HomePageState(today = todayDate),
            )

    private data class MostLinked(
        val typeId: Int?,
        val records: List<LinkedRecordCount>,
        val links: List<LinkEnds>,
        val types: List<TypeLinkCount> = emptyList(),
    )

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
        const val MOST_LINKED_COUNT = 10
        const val RECENT_COUNT = 3
        const val UNLINKED_COUNT = 3
        const val CHART_DAYS = 14
    }
}

data class HomePageState(
    val today: LocalDate,
    val counts: RecordCounts = RecordCounts(),
    /** The record type [mostLinked] is filtered to, or null for all types. */
    val mostLinkedTypeId: Int? = null,
    /** The types offered in the type picker: those with links, in picker order, with their link counts. */
    val mostLinkedTypes: List<TypeLinkCount> = emptyList(),
    /** Most linked first, at most [HomeViewModel.MOST_LINKED_COUNT]. */
    val mostLinked: List<LinkedRecordCount> = emptyList(),
    /** Links between two of [mostLinked]. */
    val mostLinkedLinks: List<LinkEnds> = emptyList(),
    val recentNotes: List<NoteSummary> = emptyList(),
    /** One entry per local day, oldest first, ending today. */
    val notesPerDay: List<DayCount> = emptyList(),
    val unlinkedNotes: List<NoteSummary> = emptyList(),
    val unlinkedNoteCount: Int = 0,
    val backup: BackupState = BackupState.Hidden,
    /** False until the first values arrive, so the page doesn't flash empty states. */
    val isLoaded: Boolean = false,
)

/** What the backup card shows. */
sealed interface BackupState {
    /** Backed up, and nothing changed since. */
    data object Hidden : BackupState

    data object NeverBackedUp : BackupState

    /**
     * [changeCount] records and links were created or edited after [lastBackupAt], and
     * [deletedSince] says whether anything was deleted after it.
     */
    data class ChangedSince(
        val lastBackupAt: Long,
        val changeCount: Int,
        val deletedSince: Boolean,
    ) : BackupState
}
