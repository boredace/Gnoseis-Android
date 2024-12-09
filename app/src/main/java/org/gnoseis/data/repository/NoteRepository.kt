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

class NoteRepository(
    private val noteDao: NoteDao,
) {
    suspend fun getNote(id: String) : Note = noteDao.getNote(id)
    fun getNoteStream(id: String) : Flow<Note> = noteDao.getNoteStream(id)
    fun getNotesStream(): Flow<List<Note>> = noteDao.getNotes()
    suspend fun getNoteByRowId (id: Long) : Note = noteDao.getNoteByRowId(id)
    suspend fun addNote(note: Note) : Long = noteDao.addNote(note)
    suspend fun updateNote(note: Note)  = noteDao.updateNote(note)
    suspend fun deleteNoteById(noteId: String)  = noteDao.deleteNoteById(noteId)


}