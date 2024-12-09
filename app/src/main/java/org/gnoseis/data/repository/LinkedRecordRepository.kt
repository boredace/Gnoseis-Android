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

class LinkedRecordRepository(
    private val linkedRecordDao: LinkedRecordDao

) {

    // ********** Linked Categories **********

    fun getLinkedCategoryListByContactId(contactId: String): Flow<List<Category>> =
        linkedRecordDao.getLinkedCategoryListByContactId(contactId)

    fun getLinkedCategoryListByItemId(itemId: String): Flow<List<Category>> =
        linkedRecordDao.getLinkedCategoryListByItemId(itemId)

    fun getLinkedCategoryListByNoteId(noteId: String): Flow<List<Category>> =
        linkedRecordDao.getLinkedCategoryListByNoteId(noteId)

    fun getLinkedCategoryListByOrganizationId(organizationId: String): Flow<List<Category>> =
        linkedRecordDao.getLinkedCategoryListByOrganizationId(organizationId)

    // ********** Linked Contacts **********

    fun getLinkedContactListByCategoryId(categoryId: String): Flow<List<Contact>> =
        linkedRecordDao.getLinkedContactListByCategoryId(categoryId)

    fun getLinkedContactListByItemId(itemId: String): Flow<List<Contact>> =
        linkedRecordDao.getLinkedContactListByItemId(itemId)

    fun getLinkedContactListByNoteId(noteId: String): Flow<List<Contact>> =
        linkedRecordDao.getLinkedContactListByNoteId(noteId)

    fun getLinkedContactListByOrganizationId(organizationId: String): Flow<List<Contact>> =
        linkedRecordDao.getLinkedContactListByOrganizationId(organizationId)


    // ********** Linked Items **********

    fun getLinkedItemListByCategoryId(categoryId: String): Flow<List<Item>> =
        linkedRecordDao.getLinkedItemListByCategoryId(categoryId)

    fun getLinkedItemListByContactId(contactId: String): Flow<List<Item>> =
        linkedRecordDao.getLinkedItemListByContactId(contactId)

    fun getLinkedItemListByNoteId(noteId: String): Flow<List<Item>> =
        linkedRecordDao.getLinkedItemListByNoteId(noteId)

    fun getLinkedItemListByOrganizationId(organizationId: String): Flow<List<Item>> =
        linkedRecordDao.getLinkedItemListByOrganizationId(organizationId)


    // ********** Linked Notes ***********

    fun getLinkedNoteListByCategoryId(categoryId: String): Flow<List<Note>> =
        linkedRecordDao.getLinkedNoteListByCategoryId(categoryId)

    fun getLinkedNoteListByContactId(contactId: String): Flow<List<Note>> =
        linkedRecordDao.getLinkedNoteListByContactId(contactId)

    fun getLinkedNoteListByItemId(itemId: String): Flow<List<Note>> =
        linkedRecordDao.getLinkedNoteListByItemId(itemId)

    fun getLinkedNoteListByOrganizationId(organizationId: String): Flow<List<Note>> =
        linkedRecordDao.getLinkedNoteListByItemId(organizationId)


    // ********** Linked Organizations ***********

    fun getLinkedOrganizationListByCategoryId(categoryId: String): Flow<List<Organization>> =
        linkedRecordDao.getLinkedOrganizationListByCategoryId(categoryId)

    fun getLinkedOrganizationListByContactId(contactId: String): Flow<List<Organization>> =
        linkedRecordDao.getLinkedOrganizationListByContactId(contactId)

    fun getLinkedOrganizationListByItemId(itemId: String): Flow<List<Organization>> =
        linkedRecordDao.getLinkedOrganizationListByItemId(itemId)

    fun getLinkedOrganizationListByNoteId(noteId: String): Flow<List<Organization>> =
        linkedRecordDao.getLinkedOrganizationListByNoteId(noteId)



    fun getLinkedRecordTypesForRecordIdStream(recordId: String): Flow<List<LinkedRecordTypeCount>> =
        linkedRecordDao.getLinkedRecordTypesForRecordIdStream(recordId)



    fun getRecordIdsLinkedToRecordId(recordId: String): Flow<List<String>> =
        linkedRecordDao.getRecordIdsLinkedToRecordId(recordId)

    suspend fun addLinkedRecord(linkedRecord: LinkedRecord) =
        linkedRecordDao.addLinkedRecord(linkedRecord)


    suspend fun deleteLinksToRecordID(id: String): Int =
        linkedRecordDao.deleteLinksToRecordID(id)
}