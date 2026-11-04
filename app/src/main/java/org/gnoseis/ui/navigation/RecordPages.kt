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

package org.gnoseis.ui.navigation

import androidx.compose.runtime.Composable
import org.gnoseis.data.enums.RecordEditPageMode
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.category.CategoryDetailsPage
import org.gnoseis.ui.category.CategoryEditPage
import org.gnoseis.ui.contact.ContactDetailsPage
import org.gnoseis.ui.contact.ContactEditPage
import org.gnoseis.ui.item.ItemDetailsPage
import org.gnoseis.ui.item.ItemEditPage
import org.gnoseis.ui.note.NoteDetailsPage
import org.gnoseis.ui.note.NoteEditPage
import org.gnoseis.ui.organization.OrganizationDetailsPage
import org.gnoseis.ui.organization.OrganizationEditPage
import org.gnoseis.ui.record.RecordDetailsNavigation
import org.gnoseis.ui.record.RecordEditNavigation

/**
 * A record's Details page, given its navigation and the #6 linked-new-record tab: the type id of
 * the record just created and linked from it, and what to call once that tab is shown.
 */
internal typealias RecordDetailsPageContent = @Composable (
    navigation: RecordDetailsNavigation,
    linkedNewRecordTypeId: Int?,
    onLinkedNewRecordTypeShown: () -> Unit,
) -> Unit

/** A record's Edit page, given its mode and navigation. */
internal typealias RecordEditPageContent = @Composable (
    pageMode: RecordEditPageMode,
    navigation: RecordEditNavigation,
) -> Unit

/** The Details page for a record of [type]; null for a type that has no page. */
internal fun recordDetailsPage(type: RecordType): RecordDetailsPageContent? = when (type) {
    RecordType.Category -> { navigation, linkedNewRecordTypeId, onShown ->
        CategoryDetailsPage(navigation = navigation, linkedNewRecordTypeId = linkedNewRecordTypeId, onLinkedNewRecordTypeShown = onShown)
    }
    RecordType.Contact -> { navigation, linkedNewRecordTypeId, onShown ->
        ContactDetailsPage(navigation = navigation, linkedNewRecordTypeId = linkedNewRecordTypeId, onLinkedNewRecordTypeShown = onShown)
    }
    RecordType.Item -> { navigation, linkedNewRecordTypeId, onShown ->
        ItemDetailsPage(navigation = navigation, linkedNewRecordTypeId = linkedNewRecordTypeId, onLinkedNewRecordTypeShown = onShown)
    }
    RecordType.Note -> { navigation, linkedNewRecordTypeId, onShown ->
        NoteDetailsPage(navigation = navigation, linkedNewRecordTypeId = linkedNewRecordTypeId, onLinkedNewRecordTypeShown = onShown)
    }
    RecordType.Organization -> { navigation, linkedNewRecordTypeId, onShown ->
        OrganizationDetailsPage(navigation = navigation, linkedNewRecordTypeId = linkedNewRecordTypeId, onLinkedNewRecordTypeShown = onShown)
    }
    RecordType.LinkedRecord -> null
}

/** The Edit page for a record of [type]; null for a type that has no page. */
internal fun recordEditPage(type: RecordType): RecordEditPageContent? = when (type) {
    RecordType.Category -> { pageMode, navigation -> CategoryEditPage(pageMode = pageMode, navigation = navigation) }
    RecordType.Contact -> { pageMode, navigation -> ContactEditPage(pageMode = pageMode, navigation = navigation) }
    RecordType.Item -> { pageMode, navigation -> ItemEditPage(pageMode = pageMode, navigation = navigation) }
    RecordType.Note -> { pageMode, navigation -> NoteEditPage(pageMode = pageMode, navigation = navigation) }
    RecordType.Organization -> { pageMode, navigation -> OrganizationEditPage(pageMode = pageMode, navigation = navigation) }
    RecordType.LinkedRecord -> null
}
