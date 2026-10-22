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

package org.gnoseis.data.entity.links

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.enums.RecordType
import org.gnoseis.testutil.inMemoryDatabase
import org.gnoseis.testutil.knownBug
import org.gnoseis.testutil.link
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * NoteItemDao is not used anywhere in the app (dead code), but it is part of the database API.
 */
@RunWith(AndroidJUnit4::class)
class NoteItemDaoTest {
    private val db: GnoseisDatabase = inMemoryDatabase()
    private val dao = db.noteItemDao()

    @After
    fun tearDown() = db.close()

    private fun linkedIds(id: String) = runBlocking { db.linkedRecordDao().getRecordIdsLinkedToRecordId(id).first() }

    @Test
    fun notesSearch_matchesTitleOrText_caseInsensitively() {
        val n1 = Note(title = "Shopping list", textContents = "milk")
        val n2 = Note(title = "Other", textContents = "buy MILK")
        val n3 = Note(title = "Unrelated", textContents = null)
        runBlocking { listOf(n1, n2, n3).forEach { db.noteDao().addNote(it) } }

        assertEquals(setOf(n1, n2), dao.getNotesListBySearch("%milk%")!!.toSet())
        assertEquals(listOf(n1), dao.getNotesListBySearch("%shop%"))
    }

    @Test
    fun itemSearch_matchesName() {
        val item = Item(itemName = "Laptop")
        runBlocking { db.itemDao().addItem(item) }
        assertEquals(listOf(item), dao.getItemListBySearch("%lap%"))
        assertEquals(emptyList<Item>(), dao.getItemListBySearch("%desk%"))
    }

    @Test
    fun deleteLinkedNoteToItem_byNoteAndItem_removesThatLink() {
        dao.addNoteItem(link("note", RecordType.Note, "item", RecordType.Item))
        dao.deleteLinkedNoteToItem("note", "item")
        assertEquals(emptyList<String>(), linkedIds("note"))
    }

    @Test
    fun deleteLinkedNoteToItem_byItem_removesItemLinksInBothDirections() {
        knownBug(
            issue = 22,
            "unused NoteItemDao - the delete query only matches links where the item is record1, because its second clause checks Record1TypeID instead of Record2TypeID. Fix: delete the unused NoteItemDao.",
        ) {
            dao.addNoteItem(link("item", RecordType.Item, "note1", RecordType.Note))
            dao.addNoteItem(link("note2", RecordType.Note, "item", RecordType.Item))

            dao.deleteLinkedNoteToItem("item")

            assertEquals(emptyList<String>(), linkedIds("item"))
        }
    }

    @Test
    fun deleteLinkedItemToNote_byNote_removesNoteLinksInBothDirections() {
        knownBug(
            issue = 22,
            "unused NoteItemDao - the delete query only matches links where the note is record1, because its second clause checks Record1TypeID instead of Record2TypeID. Fix: delete the unused NoteItemDao.",
        ) {
            dao.addNoteItem(link("note", RecordType.Note, "item1", RecordType.Item))
            dao.addNoteItem(link("item2", RecordType.Item, "note", RecordType.Note))

            dao.deleteLinkedItemToNote("note")

            assertEquals(emptyList<String>(), linkedIds("note"))
        }
    }
}
