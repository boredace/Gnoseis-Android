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

package org.gnoseis.ui.organization

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.gnoseis.data.entity.links.LinkedRecordTypeCount
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.repository.LinkedRecordRepository
import org.gnoseis.ui.record.RecordDetailsViewModel
import org.gnoseis.data.repository.SettingsRepository
import org.gnoseis.data.repository.OrganizationRepository
import org.gnoseis.domain.usecase.DeleteRecordUseCase

class OrganizationDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val organizationRepository: OrganizationRepository,
    linkedRecordRepository: LinkedRecordRepository,
    settingsRepository: SettingsRepository,
) : RecordDetailsViewModel(
    recordId = checkNotNull(savedStateHandle.toRoute<OrganizationDetailsRoute>().organizationId),
    recordType = RecordType.Organization,
    linkedRecordRepository = linkedRecordRepository,
    settingsRepository = settingsRepository,
) {
    private val deleteRecord : DeleteRecordUseCase =
        DeleteRecordUseCase(
            organizationRepository = organizationRepository,
            linkedRecordRepository = linkedRecordRepository,
            settingsRepository = settingsRepository,
        )
    private val organizationId: String = recordId


    val organizationDetailsPageState: StateFlow<OrganizationDetailsPageState> =
        combine(
            organizationRepository.getOrganizationStream(organizationId),
            linkedRecordRepository.getLinkedRecordTypesForRecordIdStream(organizationId)
        ){
                organization, linkedRecordTypes ->
            OrganizationDetailsPageState(
                organization = organization,
                linkedRecordTabs = linkedRecordTypes
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            OrganizationDetailsPageState()
        )
    fun deleteOrganization() {
        viewModelScope.launch {
            deleteRecord(organizationId)
        }
    }
    data class OrganizationDetailsPageState(
        val organization: Organization? = Organization(ownerDbId = "dbx", organizationName = "My Organization"),
        val linkedRecordTabs: List<LinkedRecordTypeCount> = emptyList()
    )




    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}