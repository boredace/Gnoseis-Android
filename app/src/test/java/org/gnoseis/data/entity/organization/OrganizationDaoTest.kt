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

package org.gnoseis.data.entity.organization

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
class OrganizationDaoTest {
    private val db: GnoseisDatabase = inMemoryDatabase()
    private val dao = db.organizationDao()

    @After
    fun tearDown() = db.close()

    @Test
    fun add_get_update_delete() = runTest {
        val org = Organization(organizationName = "Acme", comments = "c")
        val rowId = dao.addOrganization(org)
        assertEquals(org, dao.getOrganization(org.id))
        assertEquals(org, dao.getOrganizationByRowId(rowId))

        dao.updateOrganization(org.copy(organizationName = "Acme Inc"))
        assertEquals("Acme Inc", dao.getOrganizationFlow(org.id).first().organizationName)

        dao.deleteOrganizationById(org.id)
        val deleted: Organization? = dao.getOrganization(org.id)
        assertNull(deleted)
    }

    @Test
    fun list_isSortedByName() = runTest {
        listOf("Zeta", "Alpha", "Mu").forEach { dao.addOrganization(Organization(organizationName = it)) }
        assertEquals(listOf("Alpha", "Mu", "Zeta"), dao.getOrganizationsFlow().first().map { it.organizationName })
    }

    @Test
    fun listFlow_emitsOnChanges() = runTest {
        val org = Organization(organizationName = "x")
        dao.getOrganizationsFlow().test {
            assertEquals(emptyList<Organization>(), awaitItem())
            dao.addOrganization(org)
            assertEquals(listOf(org), awaitItem())
            dao.deleteOrganizationById(org.id)
            assertEquals(emptyList<Organization>(), awaitItem())
        }
    }
}
