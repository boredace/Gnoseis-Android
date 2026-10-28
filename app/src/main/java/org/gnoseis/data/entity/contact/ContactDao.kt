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

package org.gnoseis.data.entity.contact

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM Contact WHERE id=:id")
    suspend fun getContact(id: String) : Contact
    @Query("SELECT * FROM Contact WHERE id=:id")
    fun getContactFlow(id: String) : Flow<Contact>
    @Query("SELECT * FROM Contact ORDER BY nameLast, nameFirst ASC")
    fun getContactsFlow(): Flow<List<Contact>>
    @Query("SELECT* FROM Contact WHERE _rowid_ = :rowId")
    suspend fun getContactByRowId(rowId: Long) : Contact
    @Insert
    suspend fun addContact(item: Contact) : Long
    /**
     * Writes only the user-editable columns, and only when one of them changed, so saving
     * an unchanged contact doesn't move updatedAt. Never writes id, ownerDbId or createdAt.
     * Returns the number of rows changed (0 or 1).
     */
    @Query(
        "UPDATE Contact SET nameLast = :nameLast, nameFirst = :nameFirst, " +
                "jobTitle = :jobTitle, company = :company, " +
                "phoneMain = :phoneMain, phoneMobile = :phoneMobile, " +
                "phoneHome = :phoneHome, phoneWork = :phoneWork, " +
                "emailMain = :emailMain, emailMobile = :emailMobile, " +
                "emailHome = :emailHome, emailWork = :emailWork, " +
                "comments = :comments, updatedAt = :now " +
                "WHERE id = :id AND (nameLast IS NOT :nameLast OR nameFirst IS NOT :nameFirst " +
                "OR jobTitle IS NOT :jobTitle OR company IS NOT :company " +
                "OR phoneMain IS NOT :phoneMain OR phoneMobile IS NOT :phoneMobile " +
                "OR phoneHome IS NOT :phoneHome OR phoneWork IS NOT :phoneWork " +
                "OR emailMain IS NOT :emailMain OR emailMobile IS NOT :emailMobile " +
                "OR emailHome IS NOT :emailHome OR emailWork IS NOT :emailWork " +
                "OR comments IS NOT :comments)"
    )
    suspend fun update(
        id: String,
        nameLast: String,
        nameFirst: String?,
        jobTitle: String?,
        company: String?,
        phoneMain: String?,
        phoneMobile: String?,
        phoneHome: String?,
        phoneWork: String?,
        emailMain: String?,
        emailMobile: String?,
        emailHome: String?,
        emailWork: String?,
        comments: String?,
        now: Long,
    ): Int
    @Query (
        "DELETE FROM Contact WHERE id = :recordId"
    )
    suspend fun deleteById(recordId: String)
}
