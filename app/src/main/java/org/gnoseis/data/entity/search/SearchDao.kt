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

@Dao
interface SearchDao {

    @Query("SELECT *\n" +
            "  FROM (\n" +
            "           SELECT t1.id AS recordId,\n" +
            "                  t1.title AS recordTitle,\n" +
            "                  1 AS recordTypeId\n" +
            "             FROM Note AS t1\n" +
            "            WHERE t1.title LIKE :query\n" +
            "           UNION\n" +
            "           SELECT t2.id AS recordId,\n" +
            "                  t2.NameLast || ', ' || t2.NameFirst AS recordTitle,\n" +
            "                  2 AS recordTypeId\n" +
            "             FROM Contact AS t2\n" +
            "            WHERE t2.NameLast LIKE :query OR \n" +
            "                  t2.NameFirst LIKE :query\n" +
            "           UNION\n" +
            "           SELECT t3.id AS recordId,\n" +
            "                  t3.OrganizationName AS recordTitle,\n" +
            "                  3 AS recordTypeId\n" +
            "             FROM Organization AS t3\n" +
            "            WHERE t3.OrganizationName LIKE :query\n" +
            "           UNION\n" +
            "           SELECT t4.id AS recordId,\n" +
            "                  t4.CategoryName AS recordTitle,\n" +
            "                  4 AS recordTypeId\n" +
            "             FROM Category AS t4\n" +
            "            WHERE t4.CategoryName LIKE :query\n" +
            "           UNION\n" +
            "           SELECT t5.id AS recordId,\n" +
            "                  t5.itemName AS recordTitle,\n" +
            "                  5 AS recordTypeId\n" +
            "             FROM Item AS t5\n" +
            "            WHERE t5.itemName LIKE :query\n" +
            "       )")
    fun getSearchResultsForQuery(query: String): Flow<List<SearchResult>>
}