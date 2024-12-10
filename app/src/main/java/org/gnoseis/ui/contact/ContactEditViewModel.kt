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

package org.gnoseis.ui.contact

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.gnoseis.data.constants.TITLE_LENGTH_CONTACT
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.links.LinkedRecord
import org.gnoseis.data.enums.ContactEditPageMode
import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.repository.ContactRepository
import org.gnoseis.data.repository.LinkedRecordRepository
import org.gnoseis.domain.usecase.ValidateEmail
import org.gnoseis.domain.usecase.ValidatePhone
import org.gnoseis.domain.usecase.ValidationResult

class ContactEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val contactRepository: ContactRepository,
    private val linkedRecordRepository: LinkedRecordRepository,

    ) : ViewModel(){
    private val contactId : String? = savedStateHandle.toRoute<ContactEditRoute>().contactId

    private val validateEmail: ValidateEmail = ValidateEmail()
    private val validatePhone: ValidatePhone = ValidatePhone()


    private var contact = MutableStateFlow(Contact())


    private val _editState = mutableStateOf(
        EditState(
        )
    )
    val editState: State<EditState> = _editState


    private val _pageState = mutableStateOf(
        PageState(
            isNew = true,
            pageMode = checkNotNull(savedStateHandle.toRoute<ContactEditRoute>().pageMode),
            contactId =  null,
            linkFromType = savedStateHandle.toRoute<ContactEditRoute>().linkFromType,
            linkFromId = savedStateHandle.toRoute<ContactEditRoute>().linkFromId,
            contact = null
        )
    )
    val pageState: State<PageState> = _pageState

    init {
        if (!contactId.isNullOrEmpty()) {
            viewModelScope.launch {
                contactRepository.getContact(contactId).let {
                    _pageState.value = _pageState.value.copy(
                        isNew = false,
                        pageMode = checkNotNull(savedStateHandle.toRoute<ContactEditRoute>().pageMode),
                        contactId = it.id,
                        linkFromType = savedStateHandle.toRoute<ContactEditRoute>().linkFromType,
                        linkFromId = savedStateHandle.toRoute<ContactEditRoute>().linkFromId,
                        contact = it
                    )

                    _editState.value = _editState.value.copy(
                        isValid = true,
                        nameFirst = it.nameFirst,
                        nameLast = it.nameLast,
                        company = it.company,
                        jobTitle = it.jobTitle,
                        phoneMobile = it.phoneMobile,
                        phoneHome = it.phoneHome,
                        phoneWork = it.phoneWork,
                        phoneMain = it.phoneMain,
                        emailWork = it.emailWork,
                        emailMobile = it.emailMobile,
                        emailHome = it.emailHome,
                        emailMain = it.emailMain,
                        comments = it.comments
                    )
                }
            }
        }
    }
    fun onEvent(event: ContactEditPageEvent) {
        when(event) {
            is ContactEditPageEvent.NameLastChanged -> {
                var _isValid = false
                if (event.nameLast.length >= 1 && event.nameLast.length <= TITLE_LENGTH_CONTACT) {
                    _isValid = true}
                _editState.value = _editState.value.copy(nameLast = event.nameLast, isValid = _isValid)
            }
            is ContactEditPageEvent.NameFirstChanged -> {
                _editState.value = _editState.value.copy(nameFirst = event.nameFirst)
            }
            is ContactEditPageEvent.JobTitleChanged -> {
                _editState.value = _editState.value.copy(jobTitle = event.jobTitle)
            }
            is ContactEditPageEvent.CompanyChanged -> {
                _editState.value = _editState.value.copy(company = event.company)
            }
            is ContactEditPageEvent.PhoneMainChanged -> {
                _editState.value = _editState.value.copy(
                    phoneMain = event.phoneMain,
                    phoneMainValidationResult = validatePhone.execute(event.phoneMain)
                )

            }
            is ContactEditPageEvent.PhoneMobileChanged -> {
                _editState.value = _editState.value.copy(phoneMobile = event.phoneMobile)
            }
            is ContactEditPageEvent.PhoneHomeChanged -> {
                _editState.value = _editState.value.copy(phoneHome = event.phoneHome)
            }
            is ContactEditPageEvent.PhoneWorkChanged -> {
                _editState.value = _editState.value.copy(phoneWork = event.phoneWork)
            }
            is ContactEditPageEvent.EmailMainChanged -> {
                _editState.value = _editState.value.copy(emailMain = event.emailMain)
            }
            is ContactEditPageEvent.EmailMobileChanged -> {
                _editState.value = _editState.value.copy(emailMobile = event.emailMobile)
            }
            is ContactEditPageEvent.EmailHomeChanged -> {
                _editState.value = _editState.value.copy(emailHome = event.emailHome)
            }
            is ContactEditPageEvent.EmailWorkChanged -> {
                _editState.value = _editState.value.copy(emailWork = event.emailWork)
            }
            is ContactEditPageEvent.CommentsChanged -> {
                _editState.value = _editState.value.copy(comments = event.comments)
            }
            is ContactEditPageEvent.Save -> { /* already handled in Composable */ }
        }
    }
    suspend fun onSave() : String? {
        if( // Add new Contact
            _editState.value.isValid &&
            _pageState.value.isNew
        ) {
            val addedContactRowId = contactRepository.addContact(
                Contact(
                    ownerDbId = "db1",
                    nameLast = _editState.value.nameLast,
                    nameFirst = _editState.value.nameFirst,
                    company = _editState.value.company,
                    jobTitle = _editState.value.jobTitle,
                    phoneMain = _editState.value.phoneMain,
                    phoneWork = _editState.value.phoneWork,
                    phoneHome = _editState.value.phoneHome,
                    phoneMobile = _editState.value.phoneMobile,
                    emailMain = _editState.value.emailMain,
                    emailHome = _editState.value.emailHome,
                    emailMobile = _editState.value.emailMobile,
                    emailWork = _editState.value.emailWork,
                    comments = _editState.value.comments
                )
            )
            val addedContactId = contactRepository.getContactByRowId(addedContactRowId).id

            if ( // Also link to source record
                _pageState.value.pageMode == ContactEditPageMode.NEWLINK &&
                _pageState.value.linkFromId != null &&
                _pageState.value.linkFromType != null
            ) {
                linkedRecordRepository.addLinkedRecord(
                    LinkedRecord(
                        ownerDbId = "db1",
                        record1Id = _pageState.value.linkFromId!!,
                        record2Id = addedContactId,
                        record1TypeId = _pageState.value.linkFromType!!.recordTypeId,
                        record2TypeId = RecordType.Contact.recordTypeId
                    )
                )
            }
            return addedContactId
        } else if ( // Update existing Contact
            _editState.value.isValid &&
            !_pageState.value.isNew
        ) {
            contactRepository.updateContact(
                _pageState.value.contact!!.copy(
                    nameLast = _editState.value.nameLast,
                    nameFirst = _editState.value.nameFirst,
                    company = _editState.value.company,
                    jobTitle = _editState.value.jobTitle,
                    phoneMain = _editState.value.phoneMain,
                    phoneWork = _editState.value.phoneWork,
                    phoneHome = _editState.value.phoneHome,
                    phoneMobile = _editState.value.phoneMobile,
                    emailMain = _editState.value.emailMain,
                    emailHome = _editState.value.emailHome,
                    emailMobile = _editState.value.emailMobile,
                    emailWork = _editState.value.emailWork,
                    comments = _editState.value.comments
                )
            )
            return null
        } else {
            return null
        }
    }

    data class PageState(
        val isNew: Boolean = true,
        val pageMode: ContactEditPageMode? = null,
        val contactId: String? = null,
        val linkFromType: RecordType? = null,
        val linkFromId: String? = null,
        val contact: Contact? = Contact(),
    )

    data class EditState(
        val isValid: Boolean = false,
        val nameLast: String = "",
        val nameFirst: String? = null,
        val jobTitle: String? = null,
        val company: String? = null,
        val phoneMain: String? = null,
        val phoneMobile: String? = null,
        val phoneHome: String? = null,
        val phoneWork: String? = null,
        val emailMain: String? = null,
        val emailMobile: String? = null,
        val emailHome: String? = null,
        val emailWork: String? = null,
        val comments: String? = null,

        val phoneMainValidationResult: ValidationResult? = null,
    )

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}