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

package org.gnoseis.data.repository

import kotlinx.coroutines.flow.Flow
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.note.NoteDao

/**
 * [now] gives the system time (UTC epoch ms) stamped on inserts and updates (#50).
 */
class NoteRepository(
    private val noteDao: NoteDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun getNote(id: String) : Note = noteDao.getNote(id)
    fun getNoteStream(id: String) : Flow<Note> = noteDao.getNoteStream(id)
    fun getNotesStream(): Flow<List<Note>> = noteDao.getNotes()
    suspend fun getNoteByRowId (id: Long) : Note = noteDao.getNoteByRowId(id)
    suspend fun addNote(note: Note) : Long =
        now().let { noteDao.addNote(note.copy(createdAt = it, updatedAt = it)) }
    /** Saves the note's editable fields. Returns false if nothing changed. */
    suspend fun updateNote(note: Note): Boolean = noteDao.updateNote(
        id = note.id,
        title = note.title,
        textContents = note.textContents,
        createDate = note.createDate,
        createDateTime = note.createDateTime,
        now = now(),
    ) > 0
    suspend fun deleteNoteById(noteId: String)  = noteDao.deleteNoteById(noteId)


}