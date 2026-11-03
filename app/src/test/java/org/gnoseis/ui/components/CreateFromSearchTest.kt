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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** #10: when a search offers to create a record from its query. */
class CreateFromSearchTest {
    @Test
    fun exactMatch_ignoresCaseAndSpaces() {
        assertTrue(matchesSearchExactly("Acme Ltd", "  acme   LTD "))
        assertTrue(matchesSearchExactly("Jane  Doe", "jane doe")) // a contact's "First Last" title
        assertFalse(matchesSearchExactly("Acme Ltd", "Acme")) // only contains the query
        assertFalse(matchesSearchExactly(null, "Acme"))
        assertFalse(matchesSearchExactly("", "  "))
    }

    @Test
    fun offer_byResults() {
        assertEquals(CreateFromSearchOffer.None, createFromSearchOffer(" ", emptyList()))
        assertEquals(CreateFromSearchOffer.NoResults, createFromSearchOffer("Acme", emptyList()))
        assertEquals(CreateFromSearchOffer.AfterResults, createFromSearchOffer("Acme", listOf("Acme Ltd", null)))
        assertEquals(CreateFromSearchOffer.None, createFromSearchOffer("acme", listOf("Acme Ltd", "ACME")))
    }
}
