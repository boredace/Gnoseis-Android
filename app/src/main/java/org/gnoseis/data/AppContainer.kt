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

package org.gnoseis.data

import android.content.Context
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.data.repository.CategoryRepository
import org.gnoseis.data.repository.ContactRepository
import org.gnoseis.data.repository.DatabaseRepository
import org.gnoseis.data.repository.ItemRepository
import org.gnoseis.data.repository.LinkedRecordRepository
import org.gnoseis.data.repository.NoteRepository
import org.gnoseis.data.repository.OrganizationRepository
import org.gnoseis.data.repository.SearchRepository

interface  AppContainer {
    val databaseRepository: DatabaseRepository
    val categoryRepository: CategoryRepository
    val contactRepository: ContactRepository
    val itemRepository: ItemRepository
    val noteRepository: NoteRepository
    val organizationRepository: OrganizationRepository
    val linkedRecordRepository: LinkedRecordRepository
    val searchRepository: SearchRepository
}

class AppDataContainer(
    private val context: Context,
    private val database: () -> GnoseisDatabase = { GnoseisDatabase.getDatabase(context) },
) : AppContainer {

    override val databaseRepository: DatabaseRepository by lazy {
        DatabaseRepository(
            context
        )
    }
    override val categoryRepository: CategoryRepository by lazy{
        CategoryRepository(
            database().categoryDao()
        )
    }
    override val contactRepository: ContactRepository by lazy{
        ContactRepository(
            database().contactDao()
        )
    }
    override val itemRepository: ItemRepository by lazy {
        ItemRepository(
            database().itemDao()
        )
    }
    override val noteRepository: NoteRepository by lazy {
        NoteRepository(
            database().noteDao()
        )
    }
    override val organizationRepository: OrganizationRepository by lazy {
        OrganizationRepository(
            database().organizationDao()
        )
    }
    override val linkedRecordRepository: LinkedRecordRepository by lazy {
        LinkedRecordRepository(
            database().linkedRecordDao()
        )
    }
    override val searchRepository: SearchRepository by lazy {
        SearchRepository(
            database().searchDao()
        )
    }
}

