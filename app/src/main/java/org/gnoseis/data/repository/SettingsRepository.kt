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

package org.gnoseis.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.data.entity.settings.SettingKey
import org.gnoseis.data.entity.settings.SettingsDao

/**
 * Database settings (#51). [now] gives the system time (UTC epoch ms) stored as lastDeletionAt.
 * The export writes lastBackupAt itself (see [DatabaseRepository.exportDatabase]).
 */
class SettingsRepository(
    private val database: GnoseisDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val settingsDao: SettingsDao = database.settingsDao()

    /** UTC epoch ms of the last successful export, or null if there never was one. */
    fun getLastBackupAtStream(): Flow<Long?> = settingsDao.getValueIntFlow(SettingKey.LAST_BACKUP_AT.id)

    /** UTC epoch ms of the last record or link deletion, or null if there never was one. */
    fun getLastDeletionAtStream(): Flow<Long?> = settingsDao.getValueIntFlow(SettingKey.LAST_DELETION_AT.id)

    /**
     * Runs [delete] and saves lastDeletionAt in the same transaction, so the time is saved
     * exactly when the deletion is. Every path that deletes a record or a link goes through here.
     */
    suspend fun <R> recordingDeletion(delete: suspend () -> R): R =
        recordingDeletion(delete) { true }

    /**
     * Like [recordingDeletion], but saves lastDeletionAt only if [deleted] says, from the result
     * of [delete], that something was deleted. For deletes that may find nothing to delete.
     */
    suspend fun <R> recordingDeletion(delete: suspend () -> R, deleted: (R) -> Boolean): R =
        database.withTransaction {
            delete().also { if (deleted(it)) settingsDao.upsert(SettingKey.LAST_DELETION_AT.row(now())) }
        }
}
