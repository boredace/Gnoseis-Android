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

@Dao
interface LinkedRecordDao {
    @Query("select typeid as recordTypeId, count(typeid) as count\n" +
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
            "order by count(typeid) desc")
    fun getLinkedRecordTypesForRecordIdStream(recordId: String): Flow<List<LinkedRecordTypeCount>>


    // ***************************************
    // ********** Linked Categories **********
    // ***************************************
    @Query(
        "SELECT category.* \n" +
                "FROM (SELECT lrt1.record2Id AS linkedRecordId FROM linkedrecord AS lrt1 WHERE record1Id = :contactId UNION\n" +
                "SELECT lrt2.record1Id AS linkedRecordId FROM linkedrecord AS lrt2 WHERE record2Id = :contactId) AS lr\n" +
                "JOIN Category AS category ON category.id = lr.linkedRecordId")
    fun getLinkedCategoryListByContactId(contactId: String): Flow<List<Category>>
    @Query(
        "SELECT category.* \n" +
                "FROM (SELECT lrt1.record2Id AS linkedRecordId FROM linkedrecord AS lrt1 WHERE record1Id = :itemId UNION\n" +
                "SELECT lrt2.record1Id AS linkedRecordId FROM linkedrecord AS lrt2 WHERE record2Id = :itemId) AS lr\n" +
                "JOIN Category AS category ON category.id = lr.linkedRecordId")
    fun getLinkedCategoryListByItemId(itemId: String): Flow<List<Category>>
    @Query(
        "SELECT category.* \n" +
                "FROM (SELECT lrt1.record2Id AS linkedRecordId FROM linkedrecord AS lrt1 WHERE record1Id = :noteId UNION\n" +
                "SELECT lrt2.record1Id AS linkedRecordId FROM linkedrecord AS lrt2 WHERE record2Id = :noteId) AS lr\n" +
                "JOIN Category AS category ON category.id = lr.linkedRecordId")
    fun getLinkedCategoryListByNoteId(noteId: String): Flow<List<Category>>
    @Query(
        "SELECT category.* \n" +
                "FROM (SELECT lrt1.record2Id AS linkedRecordId FROM linkedrecord AS lrt1 WHERE record1Id = :organizationId UNION\n" +
                "SELECT lrt2.record1Id AS linkedRecordId FROM linkedrecord AS lrt2 WHERE record2Id = :organizationId) AS lr\n" +
                "JOIN Category AS category ON category.id = lr.linkedRecordId"  )
    fun getLinkedCategoryListByOrganizationId(organizationId: String): Flow<List<Category>>


    // *************************************
    // ********** Linked Contacts **********
    // *************************************
    @Query(
        "SELECT contact.* \n" +
                "FROM (SELECT lrt1.record2Id AS linkedRecordId FROM linkedrecord AS lrt1 WHERE record1Id = :categoryId UNION\n" +
                "SELECT lrt2.record1Id AS linkedRecordId FROM linkedrecord AS lrt2 WHERE record2Id = :categoryId) AS lr\n" +
                "JOIN Contact AS contact ON contact.id = lr.linkedRecordId")
    fun getLinkedContactListByCategoryId(categoryId: String): Flow<List<Contact>>
    @Query(
        "SELECT contact.* \n" +
                "FROM (SELECT lrt1.record2Id AS linkedRecordId FROM linkedrecord AS lrt1 WHERE record1Id = :noteId UNION\n" +
                "SELECT lrt2.record1Id AS linkedRecordId FROM linkedrecord AS lrt2 WHERE record2Id = :noteId) AS lr\n" +
                "JOIN Contact AS contact ON contact.id = lr.linkedRecordId")
    fun getLinkedContactListByNoteId(noteId: String): Flow<List<Contact>>
    @Query(
        "SELECT contact.* \n" +
                "FROM (SELECT lrt1.record2Id AS linkedRecordId FROM linkedrecord AS lrt1 WHERE record1Id = :itemId UNION\n" +
                "SELECT lrt2.record1Id AS linkedRecordId FROM linkedrecord AS lrt2 WHERE record2Id = :itemId) AS lr\n" +
                "JOIN Contact AS contact ON contact.id = lr.linkedRecordId")
    fun getLinkedContactListByItemId(itemId: String): Flow<List<Contact>>
    @Query(
        "SELECT contact.* \n" +
                "FROM (SELECT lrt1.record2Id AS linkedRecordId FROM linkedrecord AS lrt1 WHERE record1Id = :organizationId UNION\n" +
                "SELECT lrt2.record1Id AS linkedRecordId FROM linkedrecord AS lrt2 WHERE record2Id = :organizationId) AS lr\n" +
                "JOIN Contact AS contact ON contact.id = lr.linkedRecordId")
    fun getLinkedContactListByOrganizationId(organizationId: String): Flow<List<Contact>>


    // **********************************
    // ********** Linked Items **********
    // **********************************
    @Query(
        "SELECT item.* \n" +
                "FROM (SELECT lrt1.record2Id AS linkedRecordId FROM linkedrecord AS lrt1 WHERE record1Id = :categoryId UNION\n" +
                "SELECT lrt2.record1Id AS linkedRecordId FROM linkedrecord AS lrt2 WHERE record2Id = :categoryId) AS lr\n" +
                "JOIN Item AS item ON item.id = lr.linkedRecordId"  )
    fun getLinkedItemListByCategoryId(categoryId: String): Flow<List<Item>>
    @Query(
        "SELECT item.* \n" +
                "FROM (SELECT lrt1.record2Id AS linkedRecordId FROM linkedrecord AS lrt1 WHERE record1Id = :contactId UNION\n" +
                "SELECT lrt2.record1Id AS linkedRecordId FROM linkedrecord AS lrt2 WHERE record2Id = :contactId) AS lr\n" +
                "JOIN Item AS item ON item.id = lr.linkedRecordId")
    fun getLinkedItemListByContactId(contactId: String): Flow<List<Item>>
    @Query(
        "SELECT item.* \n" +
                "FROM (SELECT lrt1.record2Id AS linkedRecordId FROM linkedrecord AS lrt1 WHERE record1Id = :noteId UNION\n" +
                "SELECT lrt2.record1Id AS linkedRecordId FROM linkedrecord AS lrt2 WHERE record2Id = :noteId) AS lr\n" +
                "JOIN Item AS item ON item.id = lr.linkedRecordId")
    fun getLinkedItemListByNoteId(noteId: String): Flow<List<Item>>
    @Query(
        "SELECT item.* \n" +
                "FROM (SELECT lrt1.record2Id AS linkedRecordId FROM linkedrecord AS lrt1 WHERE record1Id = :organizationId UNION\n" +
                "SELECT lrt2.record1Id AS linkedRecordId FROM linkedrecord AS lrt2 WHERE record2Id = :organizationId) AS lr\n" +
                "JOIN Item AS item ON item.id = lr.linkedRecordId" )
    fun getLinkedItemListByOrganizationId(organizationId: String): Flow<List<Item>>


    // ***********************************
    // ********** Linked Notes ***********
    // ***********************************
    @Query(
        "SELECT note.* \n" +
                "FROM (SELECT lrt1.record2Id AS linkedRecordId FROM linkedrecord AS lrt1 WHERE record1Id = :categoryId UNION\n" +
                "SELECT lrt2.record1Id AS linkedRecordId FROM linkedrecord AS lrt2 WHERE record2Id = :categoryId) AS lr\n" +
                "JOIN Note AS note ON note.id = lr.linkedRecordId" )
    fun getLinkedNoteListByCategoryId(categoryId: String): Flow<List<Note>>
    @Query(
        "SELECT note.* \n" +
                "FROM (SELECT lrt1.record2Id AS linkedRecordId FROM linkedrecord AS lrt1 WHERE record1Id = :contactId UNION\n" +
                "SELECT lrt2.record1Id AS linkedRecordId FROM linkedrecord AS lrt2 WHERE record2Id = :contactId) AS lr\n" +
                "JOIN Note AS note ON note.id = lr.linkedRecordId"  )
    fun getLinkedNoteListByContactId(contactId: String): Flow<List<Note>>
    @Query(
        "SELECT note.* \n" +
                "FROM (SELECT lrt1.record2Id AS linkedRecordId FROM linkedrecord AS lrt1 WHERE record1Id = :itemId UNION\n" +
                "SELECT lrt2.record1Id AS linkedRecordId FROM linkedrecord AS lrt2 WHERE record2Id = :itemId) AS lr\n" +
                "JOIN Note AS note ON note.id = lr.linkedRecordId"  )
    fun getLinkedNoteListByItemId(itemId: String): Flow<List<Note>>
    @Query(
        "SELECT note.* \n" +
                "FROM (SELECT lrt1.record2Id AS linkedRecordId FROM linkedrecord AS lrt1 WHERE record1Id = :organizationId UNION\n" +
                "SELECT lrt2.record1Id AS linkedRecordId FROM linkedrecord AS lrt2 WHERE record2Id = :organizationId) AS lr\n" +
                "JOIN Note AS note ON note.id = lr.linkedRecordId"  )
    fun getLinkedNoteListByOrganizationId(organizationId: String): Flow<List<Note>>


    // *******************************************
    // ********** Linked Organizations ***********
    // *******************************************
    @Query(
        "SELECT organization.* \n" +
                "FROM (SELECT lrt1.record2Id AS linkedRecordId FROM linkedrecord AS lrt1 WHERE record1Id = :categoryId UNION\n" +
                "SELECT lrt2.record1Id AS linkedRecordId FROM linkedrecord AS lrt2 WHERE record2Id = :categoryId) AS lr\n" +
                "JOIN Organization AS organization ON organization.id = lr.linkedRecordId" )
    fun getLinkedOrganizationListByCategoryId(categoryId: String): Flow<List<Organization>>
   @Query(
        "SELECT organization.* \n" +
                "FROM (SELECT lrt1.record2Id AS linkedRecordId FROM linkedrecord AS lrt1 WHERE record1Id = :contactId UNION\n" +
                "SELECT lrt2.record1Id AS linkedRecordId FROM linkedrecord AS lrt2 WHERE record2Id = :contactId) AS lr\n" +
                "JOIN Organization AS organization ON organization.id = lr.linkedRecordId" )
    fun getLinkedOrganizationListByContactId(contactId: String): Flow<List<Organization>>
    @Query(
        "SELECT organization.* \n" +
                "FROM (SELECT lrt1.record2Id AS linkedRecordId FROM linkedrecord AS lrt1 WHERE record1Id = :noteId UNION\n" +
                "SELECT lrt2.record1Id AS linkedRecordId FROM linkedrecord AS lrt2 WHERE record2Id = :noteId) AS lr\n" +
                "JOIN Organization AS organization ON Organization.id = lr.linkedRecordId"  )
    fun getLinkedOrganizationListByNoteId(noteId: String): Flow<List<Organization>>
    @Query(
        "SELECT organization.* \n" +
                "FROM (SELECT lrt1.record2Id AS linkedRecordId FROM linkedrecord AS lrt1 WHERE record1Id = :itemId UNION\n" +
                "SELECT lrt2.record1Id AS linkedRecordId FROM linkedrecord AS lrt2 WHERE record2Id = :itemId) AS lr\n" +
                "JOIN Organization AS organization ON organization.id = lr.linkedRecordId"  )
    fun getLinkedOrganizationListByItemId(itemId: String): Flow<List<Organization>>





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

    @Insert
    suspend fun addLinkedRecord(linkedRecord: LinkedRecord)
}




