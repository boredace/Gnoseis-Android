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

sealed class ContactEditPageEvent {
    data class NameLastChanged(val nameLast: String): ContactEditPageEvent()
    data class NameFirstChanged(val nameFirst: String): ContactEditPageEvent()
    data class JobTitleChanged(val jobTitle: String): ContactEditPageEvent()
    data class CompanyChanged(val company: String): ContactEditPageEvent()
    data class PhoneMainChanged(val phoneMain: String): ContactEditPageEvent()
    data class PhoneMobileChanged(val phoneMobile: String): ContactEditPageEvent()
    data class PhoneHomeChanged(val phoneHome: String): ContactEditPageEvent()
    data class PhoneWorkChanged(val phoneWork: String): ContactEditPageEvent()
    data class EmailMainChanged(val emailMain: String): ContactEditPageEvent()
    data class EmailMobileChanged(val emailMobile: String): ContactEditPageEvent()
    data class EmailHomeChanged(val emailHome: String): ContactEditPageEvent()
    data class EmailWorkChanged(val emailWork: String): ContactEditPageEvent()
    data class CommentsChanged(val comments: String): ContactEditPageEvent()

    object Save: ContactEditPageEvent()
}