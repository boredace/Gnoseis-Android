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

import org.gnoseis.data.entity.search.SearchResult
import org.gnoseis.data.enums.RecordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** #10: when a search offers to create a record from its query. */
class CreateFromSearchTest {
    @Test
    fun exactMatch_ignoresCaseAndSpaces() {
        assertTrue(matchesSearchExactly("Acme Ltd", "  acme   LTD "))
        assertTrue(matchesSearchExactly("Jane  Doe", "jane doe"))
        assertFalse(matchesSearchExactly("Acme Ltd", "Acme")) // only contains the query
        assertFalse(matchesSearchExactly(null, "Acme"))
        assertFalse(matchesSearchExactly("", "  "))
    }

    @Test
    fun contains_ignoresCaseAndSpaces() {
        assertTrue(searchTitleContains("Budget  meeting", " budget meet"))
        assertFalse(searchTitleContains("Budget meeting", "budgetmeeting"))
    }

    @Test
    fun exactMatch_contactAsFirstLast() {
        val contact = SearchResult(recordTypeId = RecordType.Contact.recordTypeId, recordTitle = "Doe, Jane")
        assertTrue(matchesSearchExactly(contact, "doe, jane"))
        assertTrue(matchesSearchExactly(contact, "Jane  Doe"))
        assertFalse(matchesSearchExactly(contact.copy(recordTypeId = RecordType.Note.recordTypeId), "Jane Doe"))
        assertTrue(searchResultContains(contact, "jane d"))
        assertFalse(searchResultContains(contact.copy(recordTypeId = RecordType.Note.recordTypeId), "jane d"))
    }

    @Test
    fun offer_byResults() {
        fun result(title: String) = SearchResult(recordTypeId = RecordType.Item.recordTypeId, recordTitle = title)
        assertEquals(CreateFromSearchOffer.None, createFromSearchOffer(" ", emptyList()))
        assertEquals(CreateFromSearchOffer.NoResults, createFromSearchOffer("Acme", emptyList()))
        assertEquals(CreateFromSearchOffer.AfterResults, createFromSearchOffer("Acme", listOf(result("Acme Ltd"))))
        assertEquals(CreateFromSearchOffer.None, createFromSearchOffer("acme", listOf(result("Acme Ltd"), result("ACME"))))
    }
}
