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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import org.gnoseis.data.entity.search.SearchDao
import org.gnoseis.data.entity.search.SearchResult
import org.gnoseis.data.enums.RecordType

class SearchRepository(
    private val searchDao: SearchDao
) {
    fun getSearchResultsForQuery(query:String) : Flow<List<SearchResult>> = searchDao.getSearchResultsForQuery(query)

    /**
     * The [limit] most recently changed records of [types], newest change first, leaving out
     * [excludeIds] and records that were never stamped with a change time.
     */
    fun getRecentlyChanged(
        types: Collection<RecordType>,
        excludeIds: Collection<String> = emptyList(),
        limit: Int = RECENTLY_CHANGED_LIMIT,
    ): Flow<List<SearchResult>> {
        if (types.isEmpty()) return flowOf(emptyList())
        val exclude = excludeIds.toList()
        return combine(types.map { searchDao.getRecentlyChanged(it.recordTypeId, exclude, limit) }) { perType ->
            perType.flatMap { it.toList() }
                .sortedByDescending { it.updatedAt }
                .take(limit)
        }
    }

    companion object {
        /** How many recently changed records search shows before anything is typed. */
        const val RECENTLY_CHANGED_LIMIT = 5
    }
}
