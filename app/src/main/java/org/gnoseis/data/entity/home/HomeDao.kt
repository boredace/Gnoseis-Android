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

package org.gnoseis.data.entity.home

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Read-only queries for the Home page (#45). */
@Dao
interface HomeDao {
    @Query(
        "SELECT (SELECT COUNT(*) FROM Contact) AS contacts, " +
                "(SELECT COUNT(*) FROM Organization) AS organizations, " +
                "(SELECT COUNT(*) FROM Note) AS notes, " +
                "(SELECT COUNT(*) FROM Category) AS categories, " +
                "(SELECT COUNT(*) FROM Item) AS items, " +
                "(SELECT COUNT(*) FROM LinkedRecord) AS links"
    )
    fun getRecordCounts(): Flow<RecordCounts>

    /**
     * Contacts, organizations, categories and items with the most links, counting links in
     * either direction (links from notes too). Notes are never included. Ties go by name.
     * With a [recordTypeId], only records of that type.
     */
    @Query(
        "SELECT r.id, r.name, r.recordTypeId, COUNT(*) AS linkCount FROM (" +
                "SELECT id, TRIM(COALESCE(nameFirst, '') || ' ' || COALESCE(nameLast, '')) AS name, 2 AS recordTypeId FROM Contact " +
                "UNION ALL SELECT id, COALESCE(organizationName, ''), 3 FROM Organization " +
                "UNION ALL SELECT id, COALESCE(categoryName, ''), 4 FROM Category " +
                "UNION ALL SELECT id, COALESCE(itemName, ''), 5 FROM Item" +
                ") AS r JOIN LinkedRecord AS l ON l.record1Id = r.id OR l.record2Id = r.id " +
                "WHERE :recordTypeId IS NULL OR r.recordTypeId = :recordTypeId " +
                "GROUP BY r.id ORDER BY linkCount DESC, r.name COLLATE NOCASE, r.id LIMIT :limit"
    )
    fun getMostLinked(limit: Int, recordTypeId: Int?): Flow<List<LinkedRecordCount>>

    /** The links whose both ends are in [recordIds]. */
    @Query(
        "SELECT record1Id, record2Id FROM LinkedRecord " +
                "WHERE record1Id IN (:recordIds) AND record2Id IN (:recordIds)"
    )
    fun getLinksAmong(recordIds: List<String>): Flow<List<LinkEnds>>

    @Query(
        "SELECT n.id, n.title, n.textContents, n.createDate, " +
                "(SELECT COUNT(*) FROM LinkedRecord AS l WHERE l.record1Id = n.id OR l.record2Id = n.id) AS linkCount " +
                "FROM Note AS n ORDER BY n.createDateTime DESC LIMIT :limit"
    )
    fun getRecentNotes(limit: Int): Flow<List<NoteSummary>>

    /** Notes per note date (UTC day number, as the note list shows them) from [fromDay] to [toDay]. */
    @Query(
        "SELECT createDate / 86400000 AS day, COUNT(*) AS count FROM Note " +
                "WHERE createDate >= :fromDay * 86400000 AND createDate < (:toDay + 1) * 86400000 " +
                "GROUP BY day"
    )
    fun getNotesPerDay(fromDay: Long, toDay: Long): Flow<List<DayCount>>

    @Query(
        "SELECT n.id, n.title, n.textContents, n.createDate, 0 AS linkCount FROM Note AS n " +
                "WHERE NOT EXISTS (SELECT 1 FROM LinkedRecord AS l WHERE l.record1Id = n.id OR l.record2Id = n.id) " +
                "ORDER BY n.createDateTime DESC LIMIT :limit"
    )
    fun getUnlinkedNotes(limit: Int): Flow<List<NoteSummary>>

    @Query(
        "SELECT COUNT(*) FROM Note AS n " +
                "WHERE NOT EXISTS (SELECT 1 FROM LinkedRecord AS l WHERE l.record1Id = n.id OR l.record2Id = n.id)"
    )
    fun getUnlinkedNoteCount(): Flow<Int>

    /**
     * Records created or edited, and links created, after [since] (UTC epoch ms). Rows with
     * NULL times (from before schema v3) never count.
     */
    @Query(
        "SELECT (SELECT COUNT(*) FROM Note WHERE createdAt > :since OR updatedAt > :since) + " +
                "(SELECT COUNT(*) FROM Contact WHERE createdAt > :since OR updatedAt > :since) + " +
                "(SELECT COUNT(*) FROM Organization WHERE createdAt > :since OR updatedAt > :since) + " +
                "(SELECT COUNT(*) FROM Category WHERE createdAt > :since OR updatedAt > :since) + " +
                "(SELECT COUNT(*) FROM Item WHERE createdAt > :since OR updatedAt > :since) + " +
                "(SELECT COUNT(*) FROM LinkedRecord WHERE createdAt > :since)"
    )
    fun getChangeCountSince(since: Long): Flow<Int>
}

data class RecordCounts(
    val contacts: Int = 0,
    val organizations: Int = 0,
    val notes: Int = 0,
    val categories: Int = 0,
    val items: Int = 0,
    val links: Int = 0,
)

data class LinkedRecordCount(
    val id: String,
    val name: String,
    val recordTypeId: Int,
    val linkCount: Int,
)

data class LinkEnds(
    val record1Id: String,
    val record2Id: String,
)

data class NoteSummary(
    val id: String,
    val title: String?,
    val textContents: String?,
    val createDate: Long,
    val linkCount: Int,
)

data class DayCount(
    val day: Long,
    val count: Int,
)
