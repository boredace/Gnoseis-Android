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

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.gnoseis.data.entity.links.LinkedRecord
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.enums.OrganizationEditPageMode
import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.repository.LinkedRecordRepository
import org.gnoseis.data.repository.OrganizationRepository

class OrganizationEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val organizationRepository: OrganizationRepository,
    private val linkedRecordRepository: LinkedRecordRepository,

    ) : ViewModel(){

    private val organizationId : String? = savedStateHandle.toRoute<OrganizationEditRoute>().organizationId

    private val _editState = mutableStateOf(
        EditState()
    )

    val editState: State<EditState> = _editState

    private val _pageState = mutableStateOf(
        PageState(
            isNew = true,
            pageMode = checkNotNull(savedStateHandle.toRoute<OrganizationEditRoute>().pageMode),
            organizationId =  null,
            linkFromType = savedStateHandle.toRoute<OrganizationEditRoute>().linkFromType,
            linkFromId = savedStateHandle.toRoute<OrganizationEditRoute>().linkFromId,
            organization = null
        )
    )
    val pageState: State<PageState> = _pageState

    init {
        if (!organizationId.isNullOrEmpty()) {
            viewModelScope.launch {
                organizationRepository.getOrganization(organizationId).let {

                    _pageState.value = _pageState.value.copy(
                        isNew = false,
                        pageMode = checkNotNull(savedStateHandle.toRoute<OrganizationEditRoute>().pageMode),
                        organizationId = it.id,
                        linkFromType = savedStateHandle.toRoute<OrganizationEditRoute>().linkFromType,
                        linkFromId = savedStateHandle.toRoute<OrganizationEditRoute>().linkFromId,
                        organization = it
                    )

                    _editState.value = _editState.value.copy(
                        isValid = true,
                        editOrganizationName = it.organizationName,
                        editOrganizationDescription = it.comments,
                    )
                }
            }
        }
    }

    fun onEvent(event: OrganizationEditPageEvent) {
        when (event) {
            is OrganizationEditPageEvent.OrganizationNameChanged -> {
                updateOrganizationName(event.organizationName)
            }
            is OrganizationEditPageEvent.OrganizationDescriptionChanged -> {
                updateItemDescription(event.organizationDescription)
            }
            is OrganizationEditPageEvent.Save -> {}
        }
    }

    private var debounceJob: Job? = null

    private fun updateOrganizationName(it: String) {
        // update editOrganizationName only immediately
        _editState.value = _editState.value.copy(editOrganizationName = it)
        debounceJob?.cancel()
        // perform validations and update only when stopped typing
        debounceJob = CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            _editState.value = _editState.value.copy(
                isValid = validateInput(it),
                editOrganizationName = it,
            )
        }
    }

    private fun updateItemDescription(it: String) {
        // update editOrganizationDescription only immediately
        _editState.value = _editState.value.copy(editOrganizationDescription = it)
        debounceJob?.cancel()
        // perform validations and update only when stopped typing
        debounceJob = CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            _editState.value = _editState.value.copy(
                isValid = validateInput(_editState.value.editOrganizationName),
                editOrganizationDescription = it,
            )
        }
    }

    private fun validateInput(itemName: String?):Boolean {
        var result = false
        if(!itemName.isNullOrEmpty()) result = true
        return result
    }

    suspend fun onSave(): String? {
        if(
        // Add new Organization
            _editState.value.isValid &&
            _pageState.value.isNew
        ) {
            val addedRecordRowId = organizationRepository.addOrganization(
                Organization(
                    ownerDbId = "db1",
                    organizationName = _editState.value.editOrganizationName!!, // assuming cannot be valid if null
                    comments = _editState.value.editOrganizationDescription,
                )
            )
            val addedRecordId = organizationRepository.getOrganizationByRowId(addedRecordRowId).id
            if (
            // Also link to source record
                _pageState.value.pageMode == OrganizationEditPageMode.NEWLINK &&
                _pageState.value.linkFromId != null &&
                _pageState.value.linkFromType != null
            ) {
                linkedRecordRepository.addLinkedRecord(
                    LinkedRecord(
                        ownerDbId = "db1",
                        record1Id = _pageState.value.linkFromId!!,
                        record2Id = addedRecordId,
                        record1TypeId = _pageState.value.linkFromType!!.recordTypeId,
                        record2TypeId = RecordType.Organization.recordTypeId
                    )
                )
            }
            return addedRecordId

        } else if (
        // Update Existing Organization
            _editState.value.isValid &&
            !_pageState.value.isNew
        ) {
            organizationRepository.updateOrganization(_pageState.value.organization!!.copy(
                organizationName = _editState.value.editOrganizationName!!,  // assuming cannot be valid if null
                comments = _editState.value.editOrganizationDescription,
            ))
            return null
        } else {
            return null
        }
    }

    data class PageState(
        val isNew: Boolean = true,
        val pageMode: OrganizationEditPageMode? = null,
        val organizationId: String? = null,
        val linkFromType: RecordType? = null,
        val linkFromId: String? = null,
        val organization: Organization? = Organization(),
    )

    data class EditState(
        val isValid: Boolean = false,
        val editOrganizationName: String? = null,
        val editOrganizationDescription: String? = null,
    )

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}