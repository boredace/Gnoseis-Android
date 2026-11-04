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


package org.gnoseis.data.entity.search

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * How many distinct records the record [idColumn] is linked to, in either direction. A pair
 * linked more than once counts once, as on the record pages.
 */
private const val LINK_COUNT_OF = "(SELECT COUNT(DISTINCT CASE WHEN lr.record1Id = "

/**
 * Every titled record of every type, as search results: id, title, type id, link count, the
 * fields search shows as context, and two columns ([matchA], [matchB]) the query is matched on.
 * A contact's title is "Last, First" and it's matched on either name.
 */
private const val ALL_RECORDS =
    "SELECT t1.id AS recordId, t1.title AS recordTitle, 1 AS recordTypeId, " +
        LINK_COUNT_OF + "t1.id THEN lr.record2Id ELSE lr.record1Id END) FROM LinkedRecord AS lr " +
        "WHERE lr.record1Id = t1.id OR lr.record2Id = t1.id) AS linkCount, " +
        "t1.createDate AS noteDate, NULL AS jobTitle, NULL AS company, t1.updatedAt AS updatedAt, " +
        "t1.title AS matchA, NULL AS matchB " +
        "FROM Note AS t1 WHERE t1.title IS NOT NULL " +
    "UNION ALL " +
    "SELECT t2.id, t2.nameLast || COALESCE(', ' || t2.nameFirst, ''), 2, " +
        LINK_COUNT_OF + "t2.id THEN lr.record2Id ELSE lr.record1Id END) FROM LinkedRecord AS lr " +
        "WHERE lr.record1Id = t2.id OR lr.record2Id = t2.id), " +
        "NULL, t2.jobTitle, t2.company, t2.updatedAt, t2.nameLast, t2.nameFirst " +
        "FROM Contact AS t2 " +
    "UNION ALL " +
    "SELECT t3.id, t3.organizationName, 3, " +
        LINK_COUNT_OF + "t3.id THEN lr.record2Id ELSE lr.record1Id END) FROM LinkedRecord AS lr " +
        "WHERE lr.record1Id = t3.id OR lr.record2Id = t3.id), " +
        "NULL, NULL, NULL, t3.updatedAt, t3.organizationName, NULL " +
        "FROM Organization AS t3 " +
    "UNION ALL " +
    "SELECT t4.id, t4.categoryName, 4, " +
        LINK_COUNT_OF + "t4.id THEN lr.record2Id ELSE lr.record1Id END) FROM LinkedRecord AS lr " +
        "WHERE lr.record1Id = t4.id OR lr.record2Id = t4.id), " +
        "NULL, NULL, NULL, t4.updatedAt, t4.categoryName, NULL " +
        "FROM Category AS t4 " +
    "UNION ALL " +
    "SELECT t5.id, t5.itemName, 5, " +
        LINK_COUNT_OF + "t5.id THEN lr.record2Id ELSE lr.record1Id END) FROM LinkedRecord AS lr " +
        "WHERE lr.record1Id = t5.id OR lr.record2Id = t5.id), " +
        "NULL, NULL, NULL, t5.updatedAt, t5.itemName, NULL " +
        "FROM Item AS t5"

private const val RESULT_COLUMNS =
    "SELECT recordId, recordTitle, recordTypeId, linkCount, noteDate, jobTitle, company, updatedAt FROM ("

@Dao
interface SearchDao {

    /** Records whose title (a contact: either name) is LIKE [query]. */
    @Query(RESULT_COLUMNS + ALL_RECORDS + ") WHERE matchA LIKE :query OR matchB LIKE :query")
    fun getSearchResultsForQuery(query: String): Flow<List<SearchResult>>

    /**
     * Up to [limit] records of the type [recordTypeId] with a change time, newest change first,
     * leaving out [excludeIds].
     */
    @Query(
        RESULT_COLUMNS + ALL_RECORDS + ") WHERE recordTypeId = :recordTypeId " +
            "AND updatedAt IS NOT NULL AND recordId NOT IN (:excludeIds) " +
            "ORDER BY updatedAt DESC LIMIT :limit"
    )
    fun getRecentlyChanged(recordTypeId: Int, excludeIds: List<String>, limit: Int): Flow<List<SearchResult>>
}
