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

package org.gnoseis.ui.record

import org.gnoseis.data.enums.RecordType
import org.junit.Assert.assertEquals
import org.junit.Test

/** #56: the Edit page says "New <type>" only while creating a record (modes NEW and NEWLINK). */
class RecordEditTitleTest {
    private val types = listOf(
        RecordType.Note to "Note",
        RecordType.Contact to "Contact",
        RecordType.Organization to "Organization",
        RecordType.Category to "Category",
        RecordType.Item to "Item",
    )

    @Test
    fun creating_isTitledNewType() {
        for ((type, name) in types) assertEquals("New $name", recordEditTitle(type, isNew = true))
    }

    @Test
    fun editing_isTitledWithTheTypeOnly() {
        for ((type, name) in types) assertEquals(name, recordEditTitle(type, isNew = false))
    }
}
