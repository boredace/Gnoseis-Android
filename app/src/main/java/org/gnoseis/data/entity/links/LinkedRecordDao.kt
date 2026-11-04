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

package org.gnoseis.data.entity.links

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.gnoseis.data.entity.category.Category
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.enums.RecordType

@Dao
interface LinkedRecordDao {
    /**
     * Counts distinct linked records per type, so a pair linked more than once (possible
     * before #13) shows the same number as the deduplicated lists (#23).
     */
    @Query("select typeid as recordTypeId, count(distinct lr) as count\n" +
            "FROM\n" +
            "(\n" +
            "select lrt1.record2TypeId as typeId, lrt1.id as id, lrt1.record2Id as lr \n" +
            "from linkedrecord as lrt1 \n" +
            "where record1Id = :recordId \n" +
            "union \n" +
            "select lrt2.record1TypeId as typeId, lrt2.id as id, lrt2.record1Id as lr \n" +
            "from linkedrecord as lrt2\n" +
            "where record2Id = :recordId \n" +
            ")\n" +
            "group by typeid\n" +
            "order by count(distinct lr) desc")
    fun getLinkedRecordTypesForRecordIdStream(recordId: String): Flow<List<LinkedRecordTypeCount>>


    // ***************************************
    // ********** Linked records **************
    // ***************************************
    // One query per target type. Record ids are UUIDs, so the source record's type doesn't
    // matter; [typeId] keeps each query to links whose other end is of the target type.

    @Query("""
        SELECT note.* FROM (
            SELECT record2Id AS linkedRecordId FROM LinkedRecord
             WHERE record1Id = :recordId AND record2TypeId = :typeId
            UNION
            SELECT record1Id FROM LinkedRecord
             WHERE record2Id = :recordId AND record1TypeId = :typeId
        ) AS lr
        JOIN Note AS note ON note.id = lr.linkedRecordId
    """)
    fun getLinkedNotesForRecordId(
        recordId: String,
        typeId: Int = RecordType.Note.recordTypeId,
    ): Flow<List<Note>>

    @Query("""
        SELECT contact.* FROM (
            SELECT record2Id AS linkedRecordId FROM LinkedRecord
             WHERE record1Id = :recordId AND record2TypeId = :typeId
            UNION
            SELECT record1Id FROM LinkedRecord
             WHERE record2Id = :recordId AND record1TypeId = :typeId
        ) AS lr
        JOIN Contact AS contact ON contact.id = lr.linkedRecordId
    """)
    fun getLinkedContactsForRecordId(
        recordId: String,
        typeId: Int = RecordType.Contact.recordTypeId,
    ): Flow<List<Contact>>

    @Query("""
        SELECT organization.* FROM (
            SELECT record2Id AS linkedRecordId FROM LinkedRecord
             WHERE record1Id = :recordId AND record2TypeId = :typeId
            UNION
            SELECT record1Id FROM LinkedRecord
             WHERE record2Id = :recordId AND record1TypeId = :typeId
        ) AS lr
        JOIN Organization AS organization ON organization.id = lr.linkedRecordId
    """)
    fun getLinkedOrganizationsForRecordId(
        recordId: String,
        typeId: Int = RecordType.Organization.recordTypeId,
    ): Flow<List<Organization>>

    @Query("""
        SELECT category.* FROM (
            SELECT record2Id AS linkedRecordId FROM LinkedRecord
             WHERE record1Id = :recordId AND record2TypeId = :typeId
            UNION
            SELECT record1Id FROM LinkedRecord
             WHERE record2Id = :recordId AND record1TypeId = :typeId
        ) AS lr
        JOIN Category AS category ON category.id = lr.linkedRecordId
    """)
    fun getLinkedCategoriesForRecordId(
        recordId: String,
        typeId: Int = RecordType.Category.recordTypeId,
    ): Flow<List<Category>>

    @Query("""
        SELECT item.* FROM (
            SELECT record2Id AS linkedRecordId FROM LinkedRecord
             WHERE record1Id = :recordId AND record2TypeId = :typeId
            UNION
            SELECT record1Id FROM LinkedRecord
             WHERE record2Id = :recordId AND record1TypeId = :typeId
        ) AS lr
        JOIN Item AS item ON item.id = lr.linkedRecordId
    """)
    fun getLinkedItemsForRecordId(
        recordId: String,
        typeId: Int = RecordType.Item.recordTypeId,
    ): Flow<List<Item>>


    @Query("SELECT guid FROM \n" +
            "(SELECT lrt1.record2Id AS guid\n" +
            "FROM LinkedRecord AS lrt1\n" +
            "WHERE record1Id = :recordId\n" +
            "UNION\n" +
            "SELECT lrt2.record1Id AS guid\n" +
            "FROM LinkedRecord AS lrt2\n" +
            "WHERE record2Id = :recordId)" )
    fun getRecordIdsLinkedToRecordId(recordId: String): Flow<List<String>>

    @Query(
        "DELETE FROM LinkedRecord " +
                "WHERE " +
                "record1Id = :id OR " +
                "record2Id = :id" )
    suspend fun deleteLinksToRecordID(id: String): Int

    /** A link between the two records, stored either way round; null if they aren't linked. */
    @Query(
        "SELECT * FROM LinkedRecord " +
                "WHERE (record1Id = :recordIdA AND record2Id = :recordIdB) " +
                "OR (record1Id = :recordIdB AND record2Id = :recordIdA) " +
                "LIMIT 1" )
    suspend fun getLinkBetween(recordIdA: String, recordIdB: String): LinkedRecord?

    /**
     * Unlinks the two records (#11): deletes every link between them, stored either way round,
     * including duplicates from before #13. Returns the number of links deleted.
     */
    @Query(
        "DELETE FROM LinkedRecord " +
                "WHERE (record1Id = :recordIdA AND record2Id = :recordIdB) " +
                "OR (record1Id = :recordIdB AND record2Id = :recordIdA)" )
    suspend fun deleteLinkBetween(recordIdA: String, recordIdB: String): Int

    @Insert
    suspend fun addLinkedRecord(linkedRecord: LinkedRecord)

    /**
     * Inserts the link only if the two records are not linked yet, in either direction.
     */
    @Query(
        "INSERT INTO LinkedRecord (id, ownerDbId, record1Id, record2Id, record1TypeId, record2TypeId, createdAt) " +
                "SELECT :id, :ownerDbId, :record1Id, :record2Id, :record1TypeId, :record2TypeId, :createdAt " +
                "WHERE NOT EXISTS (" +
                "SELECT 1 FROM LinkedRecord " +
                "WHERE (record1Id = :record1Id AND record2Id = :record2Id) " +
                "OR (record1Id = :record2Id AND record2Id = :record1Id))" )
    suspend fun addLinkedRecordIfAbsent(
        id: String,
        ownerDbId: String,
        record1Id: String,
        record2Id: String,
        record1TypeId: Int,
        record2TypeId: Int,
        createdAt: Long?,
    )
}




