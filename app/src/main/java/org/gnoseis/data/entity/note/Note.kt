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

package org.gnoseis.data.entity.note

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
data class Note (
    @PrimaryKey
    val id : String = UUID.randomUUID().toString(),
    val ownerDbId: String = "db1",
    val title : String? = null,
    val textContents: String? = null,
    val createDateTime: Long =
        ZonedDateTime
            .now(ZoneId.systemDefault())
            .withZoneSameInstant(
                ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli(),
    val createDate: Long =
        ZonedDateTime
            .now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.DAYS)
            .withZoneSameInstant(
                ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
)