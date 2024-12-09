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

package org.gnoseis.data.entity.organization

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OrganizationDao {
    @Query("SELECT * FROM Organization WHERE id=:id")
    suspend fun getOrganization(id: String) : Organization

    @Query("SELECT * FROM Organization WHERE id=:id")
    fun getOrganizationFlow(id: String) : Flow<Organization>

    @Query("SELECT * FROM Organization ORDER BY organizationName ASC")
    fun getOrganizationsFlow(): Flow<List<Organization>>

    @Query("SELECT * FROM Organization WHERE _rowid_ = :rowId")
    suspend fun getOrganizationByRowId(rowId: Long): Organization
    @Insert
    suspend fun addOrganization(organization: Organization): Long

    @Update
    suspend fun updateOrganization(organization: Organization)
    @Query (
        "DELETE FROM Organization WHERE id = :recordId"
    )
    suspend fun deleteOrganizationById(recordId: String)
}
