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

package org.gnoseis.ui.record

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import org.gnoseis.data.entity.category.Category
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.repository.LinkedRecordRepository

/**
 * The records linked to one record, by type. The list for the record's own type stays empty:
 * records of the same type can't be linked.
 */
data class LinkedRecords(
    val linkedNotes: List<Note> = emptyList(),
    val linkedContacts: List<Contact> = emptyList(),
    val linkedOrganizations: List<Organization> = emptyList(),
    val linkedCategories: List<Category> = emptyList(),
    val linkedItems: List<Item> = emptyList(),
) {
    val count: Int
        get() = linkedNotes.size + linkedContacts.size + linkedOrganizations.size +
            linkedCategories.size + linkedItems.size
}

/** The records linked to the record [recordId] of type [recordType], kept up to date. */
fun linkedRecordsFlow(
    linkedRecordRepository: LinkedRecordRepository,
    recordId: String,
    recordType: RecordType,
): Flow<LinkedRecords> = with(linkedRecordRepository) {
    val notes = when (recordType) {
        RecordType.Contact -> getLinkedNoteListByContactId(recordId)
        RecordType.Organization -> getLinkedNoteListByOrganizationId(recordId)
        RecordType.Category -> getLinkedNoteListByCategoryId(recordId)
        RecordType.Item -> getLinkedNoteListByItemId(recordId)
        else -> flowOf(emptyList())
    }
    val contacts = when (recordType) {
        RecordType.Note -> getLinkedContactListByNoteId(recordId)
        RecordType.Organization -> getLinkedContactListByOrganizationId(recordId)
        RecordType.Category -> getLinkedContactListByCategoryId(recordId)
        RecordType.Item -> getLinkedContactListByItemId(recordId)
        else -> flowOf(emptyList())
    }
    val organizations = when (recordType) {
        RecordType.Note -> getLinkedOrganizationListByNoteId(recordId)
        RecordType.Contact -> getLinkedOrganizationListByContactId(recordId)
        RecordType.Category -> getLinkedOrganizationListByCategoryId(recordId)
        RecordType.Item -> getLinkedOrganizationListByItemId(recordId)
        else -> flowOf(emptyList())
    }
    val categories = when (recordType) {
        RecordType.Note -> getLinkedCategoryListByNoteId(recordId)
        RecordType.Contact -> getLinkedCategoryListByContactId(recordId)
        RecordType.Organization -> getLinkedCategoryListByOrganizationId(recordId)
        RecordType.Item -> getLinkedCategoryListByItemId(recordId)
        else -> flowOf(emptyList())
    }
    val items = when (recordType) {
        RecordType.Note -> getLinkedItemListByNoteId(recordId)
        RecordType.Contact -> getLinkedItemListByContactId(recordId)
        RecordType.Organization -> getLinkedItemListByOrganizationId(recordId)
        RecordType.Category -> getLinkedItemListByCategoryId(recordId)
        else -> flowOf(emptyList())
    }
    combine(notes, contacts, organizations, categories, items) { n, c, o, ca, i ->
        LinkedRecords(
            linkedNotes = n,
            linkedContacts = c,
            linkedOrganizations = o,
            linkedCategories = ca,
            linkedItems = i,
        )
    }
}
