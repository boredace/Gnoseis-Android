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

package org.gnoseis.domain.usecase

import org.gnoseis.data.repository.CategoryRepository
import org.gnoseis.data.repository.ContactRepository
import org.gnoseis.data.repository.ItemRepository
import org.gnoseis.data.repository.LinkedRecordRepository
import org.gnoseis.data.repository.NoteRepository
import org.gnoseis.data.repository.OrganizationRepository

class DeleteRecordUseCase(
    private val categoryRepository: CategoryRepository? = null,
    private val contactRepository: ContactRepository? = null,
    private val itemRepository: ItemRepository? = null,
    private val noteRepository: NoteRepository? = null,
    private val organizationRepository: OrganizationRepository? = null,
    private val linkedRecordRepository: LinkedRecordRepository,
) {
    suspend operator fun invoke(recordId: String) {
        val deletedLinkedRecordCount = linkedRecordRepository.deleteLinksToRecordID(recordId)

        contactRepository?.deleteContactById(recordId)
        categoryRepository?.deleteCategoryById(recordId)
        itemRepository?.deleteItemById(recordId)
        noteRepository?.deleteNoteById(recordId)
        organizationRepository?.deleteOrganizationById(recordId)

//        return "Hello Kitty"
    }

}