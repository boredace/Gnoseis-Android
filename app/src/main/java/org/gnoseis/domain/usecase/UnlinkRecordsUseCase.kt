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

package org.gnoseis.domain.usecase

import org.gnoseis.data.entity.links.LinkedRecord
import org.gnoseis.data.repository.LinkedRecordRepository
import org.gnoseis.data.repository.SettingsRepository

class UnlinkRecordsUseCase(
    private val linkedRecordRepository: LinkedRecordRepository,
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Unlinks the two records (#11), whichever way round the link is stored, and saves
     * lastDeletionAt in the same transaction, but only if a link was there to delete.
     * Returns the deleted link, so Undo can link the records again the same way round, or
     * null if they weren't linked (already unlinked, or one of them deleted meanwhile).
     */
    suspend operator fun invoke(recordId: String, linkedRecordId: String): LinkedRecord? =
        settingsRepository.recordingDeletion(
            delete = {
                val link = linkedRecordRepository.getLinkBetween(recordId, linkedRecordId)
                link.takeIf { linkedRecordRepository.deleteLinkBetween(recordId, linkedRecordId) > 0 }
            },
            deleted = { it != null },
        )
}
