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

class OrganizationRepository(
    private val organizationDao: OrganizationDao
) {
    suspend fun getOrganization(id: String): Organization = organizationDao.getOrganization(id)
    fun getOrganizationStream(id: String): Flow<Organization> = organizationDao.getOrganizationFlow(id)
    fun getOrganizationsStream(): Flow<List<Organization>> = organizationDao.getOrganizationsFlow()
    suspend fun getOrganizationByRowId(id: Long) : Organization = organizationDao.getOrganizationByRowId(id)
    suspend fun addOrganization(organization: Organization) : Long = organizationDao.addOrganization(organization)
    suspend fun  updateOrganization(organization: Organization) = organizationDao.updateOrganization(organization)
    suspend fun deleteOrganizationById(recordId: String) = organizationDao.deleteOrganizationById(recordId)

}