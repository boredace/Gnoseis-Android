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

import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.enums.singularName
import org.gnoseis.data.repository.CategoryRepository
import org.gnoseis.data.repository.ContactRepository
import org.gnoseis.data.repository.ItemRepository
import org.gnoseis.data.repository.NoteRepository
import org.gnoseis.data.repository.OrganizationRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The record a new record will be linked to (#7): its type and display name. */
data class LinkSource(val type: RecordType, val name: String)

class GetLinkSourceUseCase(
    private val categoryRepository: CategoryRepository,
    private val contactRepository: ContactRepository,
    private val itemRepository: ItemRepository,
    private val noteRepository: NoteRepository,
    private val organizationRepository: OrganizationRepository,
) {
    /**
     * The record [id] of [type] with its display name, named as Home's Most linked names records
     * (a note by its title, else its date as the note list shows it). A blank name falls back
     * to the type name. Null when the record doesn't exist.
     */
    suspend operator fun invoke(type: RecordType, id: String): LinkSource? {
        // The DAOs return null for a missing row despite their non-null return types.
        val name: String? = runCatching {
            when (type) {
                RecordType.Contact -> contactRepository.getContact(id)
                    ?.let { listOfNotNull(it.nameFirst, it.nameLast).joinToString(" ").trim() }
                RecordType.Organization -> organizationRepository.getOrganization(id)?.organizationName
                RecordType.Category -> categoryRepository.getCategory(id)?.categoryName
                RecordType.Item -> itemRepository.getItem(id)?.itemName
                RecordType.Note -> noteRepository.getNote(id)
                    ?.let { note -> note.title?.takeIf { it.isNotBlank() } ?: noteDateText(note.createDate) }
                RecordType.LinkedRecord -> null
            }
        }.getOrNull()
        return name?.let { LinkSource(type, it.ifBlank { singularName(type) }) }
    }

    /** A note date as the note list shows it: the stored day, read in UTC. */
    private fun noteDateText(createDate: Long): String =
        Instant.ofEpochMilli(createDate).atZone(ZoneId.of("GMT"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
}
