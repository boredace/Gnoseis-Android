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
import kotlinx.coroutines.flow.flowOf
import org.gnoseis.data.entity.home.DayCount
import org.gnoseis.data.entity.home.HomeDao
import org.gnoseis.data.entity.home.LinkEnds
import org.gnoseis.data.entity.home.LinkedRecordCount
import org.gnoseis.data.entity.home.NoteSummary
import org.gnoseis.data.entity.home.RecordCounts

class HomeRepository(
    private val homeDao: HomeDao,
) {
    fun getRecordCountsStream(): Flow<RecordCounts> = homeDao.getRecordCounts()
    fun getMostLinkedStream(limit: Int): Flow<List<LinkedRecordCount>> = homeDao.getMostLinked(limit)
    fun getLinksAmongStream(recordIds: List<String>): Flow<List<LinkEnds>> =
        if (recordIds.isEmpty()) flowOf(emptyList()) else homeDao.getLinksAmong(recordIds)
    fun getRecentNotesStream(limit: Int): Flow<List<NoteSummary>> = homeDao.getRecentNotes(limit)
    fun getNotesPerDayStream(fromDay: Long, toDay: Long): Flow<List<DayCount>> =
        homeDao.getNotesPerDay(fromDay, toDay)
    fun getUnlinkedNotesStream(limit: Int): Flow<List<NoteSummary>> = homeDao.getUnlinkedNotes(limit)
    fun getUnlinkedNoteCountStream(): Flow<Int> = homeDao.getUnlinkedNoteCount()
    fun getChangeCountSinceStream(since: Long): Flow<Int> = homeDao.getChangeCountSince(since)
}
