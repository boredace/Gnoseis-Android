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

package org.gnoseis.data.entity.settings

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Settings that belong to the database rather than the device (#51). They travel with the
 * file and its backups, and are never synced. Each row fills [valueInt] or [valueText].
 * A missing row means the setting was never set.
 */
@Entity
data class Settings(
    @PrimaryKey
    val settingId: Int,
    val settingName: String,
    val valueInt: Long? = null,
    val valueText: String? = null,
)

/** The fixed rows of [Settings]. Code finds a row by [id]; [settingName] is for people. */
enum class SettingKey(val id: Int, val settingName: String) {
    /** Reserved for the marker that says the file is a Gnoseis database. Not written yet. */
    APP_ID(1, "appId"),
    /** UTC epoch ms of the last successful export. */
    LAST_BACKUP_AT(2, "lastBackupAt"),
    /** UTC epoch ms of the last record or link deletion. */
    LAST_DELETION_AT(3, "lastDeletionAt");

    fun row(valueInt: Long) = Settings(settingId = id, settingName = settingName, valueInt = valueInt)
}
