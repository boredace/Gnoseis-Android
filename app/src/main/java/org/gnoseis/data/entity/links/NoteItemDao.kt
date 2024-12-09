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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.note.Note


@Dao
interface NoteItemDao {
    @Insert
    fun addNoteItem(linkedRecord: LinkedRecord)





    @Query("SELECT * FROM Note WHERE Title LIKE :content OR TextContents LIKE :content")
    fun getNotesListBySearch(content: String?): List<Note?>?

    @Query("SELECT * FROM Item WHERE ItemName LIKE :content")
    fun getItemListBySearch(content: String?): List<Item?>?

    @Query("DELETE FROM LinkedRecord WHERE Record1ID = :noteId AND Record2ID = :itemId")
    fun deleteLinkedNoteToItem(noteId: String?, itemId: String?)

    @Query(
        "DELETE FROM LinkedRecord WHERE (Record1TypeID = 5 AND Record1ID = :itemId) or " +
                "(Record1TypeID = 5 AND Record2ID = :itemId)"
    )
    fun deleteLinkedNoteToItem(itemId: String?)

    @Query(
        "DELETE FROM LinkedRecord WHERE (Record1TypeID = 1 AND Record1ID = :noteID) or " +
                "(Record1TypeID = 1 AND Record2ID = :noteID)"
    )
    fun deleteLinkedItemToNote(noteID: String?)
}


