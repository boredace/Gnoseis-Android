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

package org.gnoseis.ui.components

import org.gnoseis.data.entity.links.LinkedRecordTypeCount
import org.gnoseis.data.enums.RecordType
import org.junit.Assert.assertEquals
import org.junit.Test

class LinkedRecordsTabRowTest {
    // Sorted by count, as the Details pages receive them
    private val tabs = listOf(
        LinkedRecordTypeCount(RecordType.Note.recordTypeId, 3),
        LinkedRecordTypeCount(RecordType.Organization.recordTypeId, 1),
    )

    @Test
    fun noSelection_isTheDetailsTab() {
        assertEquals(0, tabIndexOf(tabs, null))
    }

    @Test
    fun selectedType_isFoundAfterTheDetailsTab() {
        assertEquals(1, tabIndexOf(tabs, RecordType.Note.recordTypeId))
        assertEquals(2, tabIndexOf(tabs, RecordType.Organization.recordTypeId))
    }

    @Test
    fun selectedType_staysSelected_whenTabsReorder() {
        val reordered = listOf(
            LinkedRecordTypeCount(RecordType.Organization.recordTypeId, 4),
            LinkedRecordTypeCount(RecordType.Note.recordTypeId, 3),
        )
        assertEquals(1, tabIndexOf(reordered, RecordType.Organization.recordTypeId))
    }

    @Test
    fun selectedTypeWithoutTab_fallsBackToDetailsTab() {
        // e.g. #6 right after linking a new Contact, before the Contacts tab has loaded
        assertEquals(0, tabIndexOf(tabs, RecordType.Contact.recordTypeId))
    }
}
