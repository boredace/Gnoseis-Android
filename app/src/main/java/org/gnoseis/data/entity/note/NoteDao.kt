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

package org.gnoseis.data.entity.note

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao{
    @Query("SELECT * FROM Note WHERE id = :id")
    suspend fun getNote(id: String) : Note
    @Query("SELECT* FROM Note WHERE _rowid_ = :rowId")
    suspend fun getNoteByRowId(rowId: Long) : Note
    @Query("SELECT * FROM Note WHERE id = :id")
    fun getNoteStream(id: String) : Flow<Note>
    @Query("SELECT * FROM Note ORDER BY createDateTime DESC")
    fun getNotes(): Flow<List<Note>>
    @Insert
    suspend fun addNote(note: Note) : Long
    /**
     * Writes only the user-editable columns, and only when one of them changed, so saving
     * an unchanged note doesn't move updatedAt. Never writes id, ownerDbId or createdAt.
     * Returns the number of rows changed (0 or 1).
     */
    @Query(
        "UPDATE Note SET title = :title, textContents = :textContents, " +
                "createDate = :createDate, createDateTime = :createDateTime, updatedAt = :now " +
                "WHERE id = :id AND (title IS NOT :title OR textContents IS NOT :textContents " +
                "OR createDate IS NOT :createDate OR createDateTime IS NOT :createDateTime)"
    )
    suspend fun updateNote(
        id: String,
        title: String?,
        textContents: String?,
        createDate: Long,
        createDateTime: Long,
        now: Long,
    ): Int
    @Query (
        "DELETE FROM Note WHERE id = :noteId"
    )
    suspend fun deleteNoteById(noteId: String)
}