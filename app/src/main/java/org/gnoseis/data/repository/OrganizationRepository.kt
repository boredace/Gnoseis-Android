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
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.entity.organization.OrganizationDao

/**
 * [now] gives the system time (UTC epoch ms) stamped on inserts and updates (#50).
 */
class OrganizationRepository(
    private val organizationDao: OrganizationDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun getOrganization(id: String): Organization = organizationDao.getOrganization(id)
    fun getOrganizationStream(id: String): Flow<Organization> = organizationDao.getOrganizationFlow(id)
    fun getOrganizationsStream(): Flow<List<Organization>> = organizationDao.getOrganizationsFlow()
    suspend fun getOrganizationByRowId(id: Long) : Organization = organizationDao.getOrganizationByRowId(id)
    suspend fun addOrganization(organization: Organization) : Long =
        now().let { organizationDao.addOrganization(organization.copy(createdAt = it, updatedAt = it)) }
    /** Saves the organization's editable fields. Returns false if nothing changed. */
    suspend fun updateOrganization(organization: Organization): Boolean =
        organizationDao.updateOrganization(
            id = organization.id,
            organizationName = organization.organizationName,
            comments = organization.comments,
            now = now(),
        ) > 0
    suspend fun deleteOrganizationById(recordId: String) = organizationDao.deleteOrganizationById(recordId)

}