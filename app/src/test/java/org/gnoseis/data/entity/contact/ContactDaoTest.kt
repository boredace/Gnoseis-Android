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

package org.gnoseis.data.entity.contact

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.testutil.inMemoryDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactDaoTest {
    private val db: GnoseisDatabase = inMemoryDatabase()
    private val dao = db.contactDao()

    @After
    fun tearDown() = db.close()

    private val fullContact = Contact(
        nameLast = "Doe", nameFirst = "Jane", jobTitle = "CTO", company = "Acme",
        phoneMain = "1", phoneMobile = "2", phoneHome = "3", phoneWork = "4",
        emailMain = "a@x.io", emailMobile = "b@x.io", emailHome = "c@x.io", emailWork = "d@x.io",
        comments = "hi",
    )

    @Test
    fun add_roundTripsEveryField() = runTest {
        val rowId = dao.addContact(fullContact)

        assertEquals(fullContact, dao.getContact(fullContact.id))
        assertEquals(fullContact, dao.getContactByRowId(rowId))
        assertEquals(fullContact, dao.getContactFlow(fullContact.id).first())
    }

    @Test
    fun getById_unknownId_returnsNull() = runTest {
        val result: Contact? = dao.getContact("missing")
        assertNull(result)
    }

    @Test
    fun update_andDelete() = runTest {
        dao.addContact(fullContact)
        dao.update(fullContact.copy(nameFirst = null, phoneMain = null))
        assertEquals(fullContact.copy(nameFirst = null, phoneMain = null), dao.getContact(fullContact.id))

        dao.deleteById(fullContact.id)
        assertEquals(emptyList<Contact>(), dao.getContactsFlow().first())
    }

    @Test
    fun list_isSortedByLastThenFirstName() = runTest {
        val c1 = Contact(nameLast = "Smith", nameFirst = "Zoe")
        val c2 = Contact(nameLast = "Adams", nameFirst = "Bob")
        val c3 = Contact(nameLast = "Smith", nameFirst = "Anna")
        val c4 = Contact(nameLast = "Smith", nameFirst = null)
        listOf(c1, c2, c3, c4).forEach { dao.addContact(it) }

        // NULL sorts first in SQLite
        assertEquals(listOf(c2, c4, c3, c1), dao.getContactsFlow().first())
    }

    @Test
    fun listFlow_emitsOnChanges() = runTest {
        dao.getContactsFlow().test {
            assertEquals(emptyList<Contact>(), awaitItem())
            dao.addContact(fullContact)
            assertEquals(listOf(fullContact), awaitItem())
            dao.deleteById(fullContact.id)
            assertEquals(emptyList<Contact>(), awaitItem())
        }
    }
}
