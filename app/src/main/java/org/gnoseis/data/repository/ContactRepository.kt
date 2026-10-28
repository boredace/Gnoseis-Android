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
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.contact.ContactDao

/**
 * [now] gives the system time (UTC epoch ms) stamped on inserts and updates (#50).
 */
class ContactRepository(
    private val contactDao: ContactDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun getContact(id: String): Contact = contactDao.getContact(id)
    fun getContactStream(id: String): Flow<Contact> = contactDao.getContactFlow(id)
    fun getContactsStream(): Flow<List<Contact>> = contactDao.getContactsFlow()
    suspend fun getContactByRowId(id: Long) : Contact = contactDao.getContactByRowId(id)
    suspend fun addContact(contact: Contact): Long =
        now().let { contactDao.addContact(contact.copy(createdAt = it, updatedAt = it)) }
    /** Saves the contact's editable fields. Returns false if nothing changed. */
    suspend fun updateContact(contact: Contact): Boolean = contactDao.update(
        id = contact.id,
        nameLast = contact.nameLast,
        nameFirst = contact.nameFirst,
        jobTitle = contact.jobTitle,
        company = contact.company,
        phoneMain = contact.phoneMain,
        phoneMobile = contact.phoneMobile,
        phoneHome = contact.phoneHome,
        phoneWork = contact.phoneWork,
        emailMain = contact.emailMain,
        emailMobile = contact.emailMobile,
        emailHome = contact.emailHome,
        emailWork = contact.emailWork,
        comments = contact.comments,
        now = now(),
    ) > 0
    suspend fun deleteContactById(recordId: String)  = contactDao.deleteById(recordId)

}