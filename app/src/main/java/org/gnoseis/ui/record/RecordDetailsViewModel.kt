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

package org.gnoseis.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.gnoseis.data.entity.links.LinkedRecord
import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.repository.LinkedRecordRepository
import org.gnoseis.data.repository.SettingsRepository
import org.gnoseis.domain.usecase.UnlinkRecordsUseCase

/**
 * What every Details view model shares: the records linked to the record [recordId] of
 * [recordType], and unlinking them (#11).
 */
abstract class RecordDetailsViewModel(
    protected val recordId: String,
    recordType: RecordType,
    private val linkedRecordRepository: LinkedRecordRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val unlinkRecords = UnlinkRecordsUseCase(linkedRecordRepository, settingsRepository)

    val linkedRecords: StateFlow<LinkedRecords> =
        linkedRecordsFlow(linkedRecordRepository, recordId, recordType).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            LinkedRecords()
        )

    /**
     * Unlinks the record [linkedRecordId] from this one. [onUnlinked] gets the removed link,
     * for Undo; it isn't called if the two weren't linked any more.
     */
    fun unlink(linkedRecordId: String, onUnlinked: (LinkedRecord) -> Unit = {}) {
        viewModelScope.launch {
            unlinkRecords(recordId, linkedRecordId)?.let(onUnlinked)
        }
    }

    /** Undo of [unlink]: links the two records of [link] again, the same way round, as a new link. */
    fun undoUnlink(link: LinkedRecord) {
        viewModelScope.launch {
            linkedRecordRepository.addLinkedRecord(
                LinkedRecord(
                    ownerDbId = link.ownerDbId,
                    record1Id = link.record1Id,
                    record2Id = link.record2Id,
                    record1TypeId = link.record1TypeId,
                    record2TypeId = link.record2TypeId,
                )
            )
        }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}
