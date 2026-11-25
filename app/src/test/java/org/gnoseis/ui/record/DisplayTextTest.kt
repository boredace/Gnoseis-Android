/*
*   Gnoseis is an Android native CRM application and general knowledge manager.
*
*   Copyright (C) 2024-2026 Gnoseis.org
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
*   https://github.com/boredace/Gnoseis-Android/blob/main/LICENSE.
*
*   Dual Licensing Notice:
*
*   Gnoseis can also be licensed under commercial terms. See
*   https://github.com/boredace/Gnoseis-Android/blob/main/COMMERCIAL_LICENSE
*
*   By contributing to Gnoseis, you agree that your contributions will be
*   licensed under both the open-source license in the LICENSE file and the
*   commercial license described in the COMMERCIAL_LICENSE file.
*/

package org.gnoseis.ui.record

import org.gnoseis.data.entity.category.Category
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.organization.Organization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The text that details pages and list rows show has line feeds for every line break (#67). */
class DisplayTextTest {
    private val stored = "one\rtwo\r\nthree\nfour"
    private val shown = "one\ntwo\nthree\nfour"

    @Test
    fun note_textIsShownWithLineFeeds_andTheStoredTextIsKept() {
        val note = Note(textContents = stored)

        assertEquals(shown, note.displayText)
        assertEquals(stored, note.textContents)
    }

    @Test
    fun contact_commentsAreShownWithLineFeeds() =
        assertEquals(shown, Contact(nameLast = "Doe", comments = stored).displayComments)

    @Test
    fun organization_commentsAreShownWithLineFeeds() =
        assertEquals(shown, Organization(organizationName = "Acme", comments = stored).displayComments)

    @Test
    fun category_commentsAreShownWithLineFeeds() =
        assertEquals(shown, Category(categoryName = "Clients", comments = stored).displayComments)

    @Test
    fun item_commentsAreShownWithLineFeeds() =
        assertEquals(shown, Item(itemName = "Laptop", comments = stored).displayComments)

    @Test
    fun noText_staysNull() {
        assertNull(Note().displayText)
        assertNull(Contact(nameLast = "Doe").displayComments)
        assertNull(Organization(organizationName = "Acme").displayComments)
        assertNull(Category(categoryName = "Clients").displayComments)
        assertNull(Item(itemName = "Laptop").displayComments)
    }

    @Test
    fun linkedNoteWithoutATitle_isNamedByItsTextWithLineFeeds() {
        val linked = LinkedRecords(linkedNotes = listOf(Note(id = "untitled", title = "", textContents = stored)))

        assertEquals(shown, linked.nameOf("untitled"))
    }
}
