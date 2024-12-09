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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.repository.OrganizationRepository

class OrganizationEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val organizationRepository: OrganizationRepository

) : ViewModel(){
    private var organizationId : String = checkNotNull(savedStateHandle[OrganizationEditDestination.organizationIdArg])
    private var organization = MutableStateFlow(Organization())

    var organizationEditPageState by mutableStateOf(OrganizationEditPageState())

    init {
        if (organizationId != "new") {
            viewModelScope.launch {
                organizationRepository.getOrganization(organizationId).let {
                    organization.value = it
                    organizationEditPageState = organizationEditPageState.copy(
                        isNew = false,
                        isValid = true,
                        organizationName = it.organizationName,
                        organizationDescription = it.comments
                    )
                }
            }
        }
    }
    fun onEvent(event: OrganizationEditPageEvent) {
        when (event) {
            is OrganizationEditPageEvent.OrganizationNameChanged -> {
                var _isValid = false
                if(event.organizationName.length >= 1) {
                    _isValid = true
                }
                organizationEditPageState = organizationEditPageState.copy(organizationName = event.organizationName, isValid = _isValid)
            }
            is OrganizationEditPageEvent.OrganizationDescriptionChanged -> {
                organizationEditPageState = organizationEditPageState.copy(organizationDescription = event.organizationDescription)
            }
            is OrganizationEditPageEvent.Save -> {}
        }
    }

    suspend fun onSave() : String? {
        if(organizationEditPageState.isValid && organizationEditPageState.isNew) {
            val addOrganizationRowId = organizationRepository.addOrganization(
                Organization(
                    organizationName = organizationEditPageState.organizationName,
                    comments = organizationEditPageState.organizationDescription
                )
            )
            return organizationRepository.getOrganizationByRowId(addOrganizationRowId).id
        } else if (organizationEditPageState.isValid && !organizationEditPageState.isNew) {
            organizationRepository.updateOrganization(
                organization.value.copy(
                    organizationName = organizationEditPageState.organizationName,
                    comments = organizationEditPageState.organizationDescription
                )
            )
            return null
        } else {
            return null
        }
    }


    data class OrganizationEditPageState(
        val isNew: Boolean = true,
        val isValid: Boolean = false,
        val organizationName: String = "",
        val organizationDescription: String? = null,
    )


    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}