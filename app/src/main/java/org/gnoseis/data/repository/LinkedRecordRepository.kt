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
import org.gnoseis.data.entity.category.Category
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.links.LinkedRecord
import org.gnoseis.data.entity.links.LinkedRecordDao
import org.gnoseis.data.entity.links.LinkedRecordTypeCount
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.organization.Organization

/**
 * [now] gives the system time (UTC epoch ms) stamped on new links (#50).
 */
class LinkedRecordRepository(
    private val linkedRecordDao: LinkedRecordDao,
    private val now: () -> Long = System::currentTimeMillis,
) {

    // ********** Linked records, one method per target type **********

    fun getLinkedNotesForRecordId(recordId: String): Flow<List<Note>> =
        linkedRecordDao.getLinkedNotesForRecordId(recordId)

    fun getLinkedContactsForRecordId(recordId: String): Flow<List<Contact>> =
        linkedRecordDao.getLinkedContactsForRecordId(recordId)

    fun getLinkedOrganizationsForRecordId(recordId: String): Flow<List<Organization>> =
        linkedRecordDao.getLinkedOrganizationsForRecordId(recordId)

    fun getLinkedCategoriesForRecordId(recordId: String): Flow<List<Category>> =
        linkedRecordDao.getLinkedCategoriesForRecordId(recordId)

    fun getLinkedItemsForRecordId(recordId: String): Flow<List<Item>> =
        linkedRecordDao.getLinkedItemsForRecordId(recordId)


    fun getLinkedRecordTypesForRecordIdStream(recordId: String): Flow<List<LinkedRecordTypeCount>> =
        linkedRecordDao.getLinkedRecordTypesForRecordIdStream(recordId)



    fun getRecordIdsLinkedToRecordId(recordId: String): Flow<List<String>> =
        linkedRecordDao.getRecordIdsLinkedToRecordId(recordId)

    /**
     * Links two records. Does nothing if they are already linked (in either direction).
     */
    suspend fun addLinkedRecord(linkedRecord: LinkedRecord) =
        linkedRecordDao.addLinkedRecordIfAbsent(
            id = linkedRecord.id,
            ownerDbId = linkedRecord.ownerDbId,
            record1Id = linkedRecord.record1Id,
            record2Id = linkedRecord.record2Id,
            record1TypeId = linkedRecord.record1TypeId,
            record2TypeId = linkedRecord.record2TypeId,
            createdAt = now(),
        )


    suspend fun deleteLinksToRecordID(id: String): Int =
        linkedRecordDao.deleteLinksToRecordID(id)

    suspend fun getLinkBetween(recordIdA: String, recordIdB: String): LinkedRecord? =
        linkedRecordDao.getLinkBetween(recordIdA, recordIdB)

    /** Deletes every link between the two records, either way round. Returns how many. */
    suspend fun deleteLinkBetween(recordIdA: String, recordIdB: String): Int =
        linkedRecordDao.deleteLinkBetween(recordIdA, recordIdB)
}