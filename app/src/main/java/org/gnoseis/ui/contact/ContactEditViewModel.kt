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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.gnoseis.data.constants.TITLE_LENGTH_CONTACT
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.repository.ContactRepository
import org.gnoseis.domain.usecase.ValidateEmail
import org.gnoseis.domain.usecase.ValidatePhone
import org.gnoseis.domain.usecase.ValidationResult

class ContactEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val contactRepository: ContactRepository

) : ViewModel(){
    private val validateEmail: ValidateEmail = ValidateEmail()
    private val validatePhone: ValidatePhone = ValidatePhone()

    private var contactId : String = checkNotNull(savedStateHandle[ContactEditDestination.contactIdArg])

    private var contact = MutableStateFlow(Contact())

    var contactEditPageState by mutableStateOf(ContactEditPageState())

    /*val contactAddState : StateFlow<ContactEdit> =
        combine(
            newContact
        ){
            ContactEdit()
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            ContactEdit()
        )
*/

    init {
        if (contactId != "new") {
            viewModelScope.launch {
                contactRepository.getContact(contactId).let {
                    contact.value = it
                    contactEditPageState = contactEditPageState.copy(
                        isNew = false,
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
                contactEditPageState = contactEditPageState.copy(nameLast = event.nameLast, isValid = _isValid)
            }
            is ContactEditPageEvent.NameFirstChanged -> {
                contactEditPageState = contactEditPageState.copy(nameFirst = event.nameFirst)
            }
            is ContactEditPageEvent.JobTitleChanged -> {
                contactEditPageState = contactEditPageState.copy(jobTitle = event.jobTitle)
            }
            is ContactEditPageEvent.CompanyChanged -> {
                contactEditPageState = contactEditPageState.copy(company = event.company)
            }
            is ContactEditPageEvent.PhoneMainChanged -> {
                contactEditPageState = contactEditPageState.copy(
                    phoneMain = event.phoneMain,
                    phoneMainValidationResult = validatePhone.execute(event.phoneMain)
                )

            }
            is ContactEditPageEvent.PhoneMobileChanged -> {
                contactEditPageState = contactEditPageState.copy(phoneMobile = event.phoneMobile)
            }
            is ContactEditPageEvent.PhoneHomeChanged -> {
                contactEditPageState = contactEditPageState.copy(phoneHome = event.phoneHome)
            }
            is ContactEditPageEvent.PhoneWorkChanged -> {
                contactEditPageState = contactEditPageState.copy(phoneWork = event.phoneWork)
            }
            is ContactEditPageEvent.EmailMainChanged -> {
                contactEditPageState = contactEditPageState.copy(emailMain = event.emailMain)
            }
            is ContactEditPageEvent.EmailMobileChanged -> {
                contactEditPageState = contactEditPageState.copy(emailMobile = event.emailMobile)
            }
            is ContactEditPageEvent.EmailHomeChanged -> {
                contactEditPageState = contactEditPageState.copy(emailHome = event.emailHome)
            }
            is ContactEditPageEvent.EmailWorkChanged -> {
                contactEditPageState = contactEditPageState.copy(emailWork = event.emailWork)
            }
            is ContactEditPageEvent.CommentsChanged -> {
                contactEditPageState = contactEditPageState.copy(comments = event.comments)
            }
            is ContactEditPageEvent.Save -> { /* already handled in Composable */ }
        }
    }
    suspend fun onSave() : String? {
        if(contactEditPageState.isValid && contactEditPageState.isNew) {
            val addContactRowId = contactRepository.addContact(
                Contact(
                    ownerDbId = "db1",
                    nameLast = contactEditPageState.nameLast,
                    nameFirst = contactEditPageState.nameFirst,
                    company = contactEditPageState.company,
                    jobTitle = contactEditPageState.jobTitle,
                    phoneMain = contactEditPageState.phoneMain,
                    phoneWork = contactEditPageState.phoneWork,
                    phoneHome = contactEditPageState.phoneHome,
                    phoneMobile = contactEditPageState.phoneMobile,
                    emailMain = contactEditPageState.emailMain,
                    emailHome = contactEditPageState.emailHome,
                    emailMobile = contactEditPageState.emailMobile,
                    emailWork = contactEditPageState.emailWork,
                    comments = contactEditPageState.comments
                )
            )
            return contactRepository.getContactByRowId(addContactRowId).id
        } else if (contactEditPageState.isValid && !contactEditPageState.isNew) {
            contactRepository.updateContact(
                contact.value.copy(
                    nameLast = contactEditPageState.nameLast,
                    nameFirst = contactEditPageState.nameFirst,
                    company = contactEditPageState.company,
                    jobTitle = contactEditPageState.jobTitle,
                    phoneMain = contactEditPageState.phoneMain,
                    phoneWork = contactEditPageState.phoneWork,
                    phoneHome = contactEditPageState.phoneHome,
                    phoneMobile = contactEditPageState.phoneMobile,
                    emailMain = contactEditPageState.emailMain,
                    emailHome = contactEditPageState.emailHome,
                    emailMobile = contactEditPageState.emailMobile,
                    emailWork = contactEditPageState.emailWork,
                    comments = contactEditPageState.comments
                )
            )
            return null
        } else {
            return null
        }
    }
    data class ContactEditPageState(
        val isNew: Boolean = true,
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